package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.opinion.OpinionReply.Answer;
import com.connectlink.opinion.OpinionReplyAnswer.Stage;
import com.connectlink.opinion.dto.OpinionAnswerRequest;
import com.connectlink.opinion.dto.OpinionAnswerResponse;
import com.connectlink.opinion.dto.OpinionMyReplyResponse;
import com.connectlink.opinion.dto.OpinionReplySaveRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 회신. 기관 담당자의 임시저장·최종제출·제출 내용 확인·수정 요청과, 결제원 담당자의 수정 요청 승인·반려를 맡는다. 회신은 대상 기관 하나에 하나이며 그 기관의 같은
 * 업무 담당자 누구나 이어서 쓴다. 회신중 요청에만 저장·제출·수정 요청을 받는다.
 */
@Service
@Transactional
public class OpinionReplyService {

  private final OpinionRequestService requestService;
  private final OpinionReplyRepository replyRepository;
  private final OpinionAccessPolicy accessPolicy;
  private final ChangeHistoryService changeHistoryService;
  private final OpinionNotifier notifier;

  public OpinionReplyService(
      OpinionRequestService requestService,
      OpinionReplyRepository replyRepository,
      OpinionAccessPolicy accessPolicy,
      ChangeHistoryService changeHistoryService,
      OpinionNotifier notifier) {
    this.requestService = requestService;
    this.replyRepository = replyRepository;
    this.accessPolicy = accessPolicy;
    this.changeHistoryService = changeHistoryService;
    this.notifier = notifier;
  }

  /** 내 기관의 회신. 아직 저장한 적이 없으면 빈 회신(상태 null)이다. */
  @Transactional(readOnly = true)
  public OpinionMyReplyResponse getMine(CurrentUser user, Long requestId) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    return toResponse(
        request, target, replyRepository.findByTarget_Id(target.getId()).orElse(null));
  }

  /** 임시저장. 처음 쓰는 중이거나 수정 요청이 승인된 뒤에만 된다. 제출한 내용은 그대로 둔다. 빈 질문이 있어도 된다. */
  public OpinionMyReplyResponse saveDraft(
      CurrentUser user, Long requestId, OpinionReplySaveRequest body) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    ensureOpen(request);
    List<Answer> answers = toAnswers(request, body.answers(), false);
    OpinionReply reply = findOrStart(target);
    ensureEditable(reply);
    reply.saveDraft(body.type(), trimToNull(body.content()), answers, user.email(), user.name());
    replyRepository.saveAndFlush(reply);
    return toResponse(request, target, reply);
  }

  /**
   * 최종제출. 의견 유형이 있어야 하고, 조건부 동의·이견은 세부의견도 있어야 하며, 필수 질문에는 모두 답해야 한다. 처음 제출이면 등록 이력을, 수정 뒤 다시 제출이면
   * 바뀐 항목을 이력으로 남긴다.
   */
  public OpinionMyReplyResponse submit(
      CurrentUser user, Long requestId, OpinionReplySaveRequest body) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    ensureOpen(request);
    if (body.type() == null) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "OPINION_REPLY_TYPE_REQUIRED", "type", "의견 유형을 골라 주세요.");
    }
    String content = trimToNull(body.content());
    if (body.type() != OpinionReplyType.AGREE && content == null) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "OPINION_REPLY_CONTENT_REQUIRED",
          "content",
          body.type().getLabel() + "은(는) 세부의견을 적어 주세요.");
    }
    List<Answer> answers = toAnswers(request, body.answers(), true);
    OpinionReply reply = findOrStart(target);
    ensureEditable(reply);
    boolean resubmit = reply.isReplied();
    OpinionReplyType beforeType = reply.getType();
    String beforeContent = reply.getContent();
    String beforeAnswers = answerSummary(request, reply.getAnswers(Stage.SUBMITTED));
    reply.submit(body.type(), content, answers, user.email(), user.name(), requestService.now());
    String afterAnswers = answerSummary(request, answers);
    replyRepository.saveAndFlush(reply);
    String organization = target.getOrganization().getName();
    changeHistoryService.record(
        user,
        resubmit
            ? ChangeHistoryCommand.updated(
                HistoryTargetType.OPINION_REPLY,
                reply.getId(),
                request.getBusiness(),
                organization + " 회신 수정 제출",
                FieldChanges.compare()
                    .field("type", "의견 유형", beforeType, reply.getType())
                    .field("content", "세부의견", beforeContent, reply.getContent())
                    .field("answers", "질문 답", beforeAnswers, afterAnswers)
                    .toList())
            : ChangeHistoryCommand.created(
                HistoryTargetType.OPINION_REPLY,
                reply.getId(),
                request.getBusiness(),
                organization + " 회신 제출",
                FieldChanges.compare()
                    .value("type", "의견 유형", null, reply.getType())
                    .value("content", "세부의견", null, reply.getContent())
                    .value("answers", "질문 답", null, afterAnswers)
                    .toList()));
    return toResponse(request, target, reply);
  }

  /** 제출한 회신을 고치고 싶다고 요청한다. 요청을 만든 결제원 담당자가 승인하면 고칠 수 있다. */
  public OpinionMyReplyResponse requestRevision(CurrentUser user, Long requestId, String reason) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    ensureOpen(request);
    OpinionReply reply =
        replyRepository
            .findByTarget_Id(target.getId())
            .filter(r -> r.getStatus() == OpinionReplyStatus.SUBMITTED)
            .orElseThrow(
                () -> BusinessException.conflict("OPINION_REPLY_STATUS", "제출한 회신만 수정 요청할 수 있습니다."));
    reply.requestRevision(reason.trim(), user.email(), user.name(), requestService.now());
    notifier.revisionRequested(request, reply);
    return toResponse(request, target, reply);
  }

  /** 수정 요청을 승인한다. 기관은 제출한 내용에서 고쳐 다시 제출한다. */
  public void approveRevision(CurrentUser user, Long requestId, Long replyId) {
    OpinionRequest request = requestService.loadManaged(user, requestId);
    ensureOpen(request);
    OpinionReply reply = loadRevisionRequested(request, replyId);
    reply.approveRevision();
    notifier.revisionReviewed(request, reply, true);
  }

  /** 수정 요청을 받아들이지 않는다. 제출 상태로 돌아가고 사유가 기관 담당자에게 간다. */
  public void rejectRevision(CurrentUser user, Long requestId, Long replyId, String reason) {
    OpinionRequest request = requestService.loadManaged(user, requestId);
    ensureOpen(request);
    OpinionReply reply = loadRevisionRequested(request, replyId);
    reply.rejectRevision(reason.trim());
    notifier.revisionReviewed(request, reply, false);
  }

  /**
   * 보낸 답을 저장할 칸으로 바꾼다. 이 요청의 질문·선택지여야 하고, 하나 고르기는 한 개까지다. 제출이면 필수 질문에 모두 답했는지도 본다. 오류는 그 질문
   * 칸({@code answers.{질문 ID}})에 붙는다.
   */
  static List<Answer> toAnswers(
      OpinionRequest request, List<OpinionAnswerRequest> body, boolean submit) {
    Map<Long, OpinionQuestion> questions =
        request.getQuestions().stream()
            .collect(Collectors.toMap(OpinionQuestion::getId, Function.identity()));
    Set<Long> seen = new HashSet<>();
    Set<Long> answered = new HashSet<>();
    List<Answer> answers = new ArrayList<>();
    for (OpinionAnswerRequest a : body) {
      OpinionQuestion question = questions.get(a.questionId());
      if (question == null || !seen.add(a.questionId())) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST, "OPINION_ANSWER_INVALID", "answers", "이 요청의 질문이 아닌 답이 있습니다.");
      }
      String field = "answers." + question.getId();
      if (question.getType().isChoice()) {
        Set<Long> optionIds = new LinkedHashSet<>(a.optionIds());
        if (question.getType() == OpinionQuestionType.SINGLE_CHOICE && optionIds.size() > 1) {
          throw BusinessException.onField(
              HttpStatus.BAD_REQUEST, "OPINION_ANSWER_INVALID", field, "하나만 골라 주세요.");
        }
        for (Long optionId : optionIds) {
          if (question.findOption(optionId).isEmpty()) {
            throw BusinessException.onField(
                HttpStatus.BAD_REQUEST, "OPINION_ANSWER_INVALID", field, "이 질문의 선택지가 아닙니다.");
          }
          answers.add(new Answer(question.getId(), optionId, null));
        }
        if (!optionIds.isEmpty()) {
          answered.add(question.getId());
        }
      } else {
        String text = trimToNull(a.text());
        if (text != null) {
          answers.add(new Answer(question.getId(), null, text));
          answered.add(question.getId());
        }
      }
    }
    if (submit) {
      for (OpinionQuestion question : request.getQuestions()) {
        if (question.isRequired() && !answered.contains(question.getId())) {
          throw BusinessException.onField(
              HttpStatus.BAD_REQUEST,
              "OPINION_ANSWER_REQUIRED",
              "answers." + question.getId(),
              "필수 질문입니다. 답해 주세요.");
        }
      }
    }
    return answers;
  }

  /** 답을 "1. 질문: 답" 꼴로 한 줄씩(이력·엑셀용). 답이 없으면 null. */
  static String answerSummary(OpinionRequest request, List<Answer> answers) {
    List<String> lines = new ArrayList<>();
    for (OpinionQuestion question : request.getQuestions()) {
      String value = answerText(question, answers);
      if (value != null) {
        lines.add((question.getSortOrder() + 1) + ". " + question.getTitle() + ": " + value);
      }
    }
    return lines.isEmpty() ? null : String.join("\n", lines);
  }

  /** 한 질문의 답을 글로. 객관식은 고른 선택지를 ", " 로 잇는다. 답이 없으면 null. */
  static String answerText(OpinionQuestion question, List<Answer> answers) {
    List<Answer> mine =
        answers.stream().filter(a -> a.questionId().equals(question.getId())).toList();
    if (mine.isEmpty()) {
      return null;
    }
    if (!question.getType().isChoice()) {
      return mine.get(0).text();
    }
    Set<Long> chosen = mine.stream().map(Answer::optionId).collect(Collectors.toSet());
    return question.getOptions().stream()
        .filter(o -> chosen.contains(o.getId()))
        .map(OpinionQuestionOption::getLabel)
        .collect(Collectors.joining(", "));
  }

  /** 저장한 답을 질문별 응답으로. 질문 순서다. */
  static List<OpinionAnswerResponse> toAnswerResponses(
      OpinionRequest request, List<Answer> answers) {
    List<OpinionAnswerResponse> result = new ArrayList<>();
    for (OpinionQuestion question : request.getQuestions()) {
      List<Answer> mine =
          answers.stream().filter(a -> a.questionId().equals(question.getId())).toList();
      if (mine.isEmpty()) {
        continue;
      }
      result.add(
          new OpinionAnswerResponse(
              question.getId(),
              mine.stream().map(Answer::optionId).filter(Objects::nonNull).toList(),
              question.getType().isChoice() ? null : mine.get(0).text()));
    }
    return result;
  }

  private OpinionReply loadRevisionRequested(OpinionRequest request, Long replyId) {
    OpinionReply reply =
        replyRepository
            .findById(replyId)
            .filter(r -> r.getTarget().getRequest().getId().equals(request.getId()))
            .orElseThrow(
                () -> BusinessException.notFound("OPINION_REPLY_NOT_FOUND", "기관 회신을 찾을 수 없습니다."));
    if (reply.getStatus() != OpinionReplyStatus.REVISION_REQUESTED) {
      throw BusinessException.conflict("OPINION_REPLY_STATUS", "수정 요청한 회신이 아닙니다.");
    }
    return reply;
  }

  private OpinionTarget ownTarget(CurrentUser user, OpinionRequest request) {
    accessPolicy.ensureTargetMember(user, request);
    return OpinionRequestService.ownTarget(user, request).orElseThrow();
  }

  private OpinionReply findOrStart(OpinionTarget target) {
    return replyRepository
        .findByTarget_Id(target.getId())
        .orElseGet(() -> new OpinionReply(target));
  }

  private static void ensureOpen(OpinionRequest request) {
    if (request.getStatus() != OpinionRequestStatus.OPEN) {
      throw BusinessException.conflict(
          "OPINION_REQUEST_STATUS",
          request.isCancelled() ? "취소된 의견 요청입니다." : "마감한 의견 요청이라 회신을 받지 않습니다.");
    }
  }

  private static void ensureEditable(OpinionReply reply) {
    if (!reply.isEditable()) {
      throw BusinessException.conflict(
          "OPINION_REPLY_NOT_EDITABLE",
          reply.getStatus() == OpinionReplyStatus.REVISION_REQUESTED
              ? "수정 요청을 결제원 담당자가 승인하면 고칠 수 있습니다."
              : "이미 제출한 회신입니다. 고치려면 수정 요청을 해 주세요.");
    }
  }

  static OpinionMyReplyResponse toResponse(
      OpinionRequest request, OpinionTarget target, OpinionReply reply) {
    boolean open = request.getStatus() == OpinionRequestStatus.OPEN;
    if (reply == null) {
      return new OpinionMyReplyResponse(
          request.getId(),
          target.getId(),
          target.getOrganization().getName(),
          null,
          null,
          "미작성",
          null,
          null,
          null,
          null,
          null,
          List.of(),
          List.of(),
          null,
          null,
          null,
          null,
          null,
          open,
          false);
    }
    return new OpinionMyReplyResponse(
        request.getId(),
        target.getId(),
        target.getOrganization().getName(),
        reply.getId(),
        reply.getStatus(),
        reply.getStatus().getLabel(),
        reply.getType(),
        reply.getType() == null ? null : reply.getType().getLabel(),
        reply.getContent(),
        reply.getDraftType(),
        reply.getDraftContent(),
        toAnswerResponses(request, reply.getAnswers(Stage.SUBMITTED)),
        toAnswerResponses(request, reply.getAnswers(Stage.DRAFT)),
        reply.getReplierName(),
        reply.getSubmittedAt(),
        reply.getRevisionReason(),
        reply.getRevisionRequestedAt(),
        reply.getRevisionRejectionReason(),
        open && reply.isEditable(),
        open && reply.getStatus() == OpinionReplyStatus.SUBMITTED);
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
