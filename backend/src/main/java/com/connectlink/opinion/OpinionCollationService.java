package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.NetworkZoneResolver;
import com.connectlink.notification.dto.MailPreviewResponse;
import com.connectlink.opinion.OpinionReply.Answer;
import com.connectlink.opinion.OpinionReplyAnswer.Stage;
import com.connectlink.opinion.dto.OpinionCollationResponse;
import com.connectlink.opinion.dto.OpinionCollationResponse.OptionCount;
import com.connectlink.opinion.dto.OpinionCollationResponse.QuestionSummary;
import com.connectlink.opinion.dto.OpinionCollationResponse.Row;
import com.connectlink.opinion.dto.OpinionCollationResponse.RowAnswer;
import com.connectlink.opinion.dto.OpinionCollationResponse.Summary;
import com.connectlink.opinion.dto.OpinionCollationResponse.TextAnswer;
import com.connectlink.opinion.dto.OpinionReminderResponse;
import com.connectlink.opinion.dto.OpinionReminderSendRequest;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회신 관리와 취합. 회신 여부 자동 구분, 질문별 답 모음(선택지별 기관 수·목록), 회신율, 미회신 기관 리마인드 메일과 그 이력, 기관별 취합표와 엑셀을 맡는다. 모두 그
 * 업무를 맡은 결제원 담당자와 관리자만 쓴다. 엑셀은 내부망 전용이다.
 */
@Service
@Transactional(readOnly = true)
public class OpinionCollationService {

  private final OpinionRequestService requestService;
  private final OpinionReplyRepository replyRepository;
  private final OpinionReminderRepository reminderRepository;
  private final OpinionNotifier notifier;
  private final NetworkZoneResolver networkZoneResolver;

  public OpinionCollationService(
      OpinionRequestService requestService,
      OpinionReplyRepository replyRepository,
      OpinionReminderRepository reminderRepository,
      OpinionNotifier notifier,
      NetworkZoneResolver networkZoneResolver) {
    this.requestService = requestService;
    this.replyRepository = replyRepository;
    this.reminderRepository = reminderRepository;
    this.notifier = notifier;
    this.networkZoneResolver = networkZoneResolver;
  }

  /** 회신 현황과 기관별 취합표. 대상 기관마다 한 줄이고 대상에 넣은 순서다. */
  public OpinionCollationResponse collation(CurrentUser user, Long requestId) {
    return build(requestService.loadManaged(user, requestId));
  }

  /** 취합표 엑셀(결과 요약, 질문별 답, 기관별 취합표). 내부망에서만 내려받는다. */
  public OpinionExcel excel(CurrentUser user, Long requestId) {
    OpinionRequest request = requestService.loadManaged(user, requestId);
    networkZoneResolver.ensureInternal("엑셀 다운로드");
    return OpinionExcel.of(request, build(request), requestService.now());
  }

  /**
   * 미회신 기관의 회신 담당자에게 리마인드 메일을 보내고 이력을 남긴다. 기관을 고르지 않으면 미회신 기관 전부이고, 회신한 기관은 골라도 빠진다. 받을 담당자가 한 명도
   * 없으면 400 이다. 제목·본문을 넘기면 기본 문구 대신 그 문구로 보낸다.
   */
  @Transactional
  public OpinionReminderResponse remind(
      CurrentUser user, Long requestId, OpinionReminderSendRequest body) {
    OpinionRequest request = loadOpen(user, requestId);
    ReminderTargets targets = reminderTargets(request, body.organizationIds());
    if (targets.emails().isEmpty()) {
      throw new BusinessException(
          HttpStatus.BAD_REQUEST,
          "OPINION_REMINDER_NO_RECIPIENT",
          "리마인드 메일을 받을 미회신 기관 담당자가 없습니다. 대상 기관의 회신 담당자를 확인해 주세요.");
    }
    OpinionNotifier.Message message = notifier.reminderMessage(request, daysLeft(request));
    notifier.remind(
        request,
        targets.emails(),
        new OpinionNotifier.Message(
            body.subject() == null ? message.title() : body.subject().strip(),
            body.body() == null ? message.body() : body.body().strip()));
    List<String> organizations =
        targets.organizations().stream()
            .map(MailPreviewResponse.Organization::organizationName)
            .toList();
    OpinionReminder reminder =
        reminderRepository.saveAndFlush(
            new OpinionReminder(
                request,
                user.name(),
                organizations.size(),
                targets.emails().size(),
                String.join(", ", organizations)));
    return toResponse(reminder);
  }

  /** 리마인드 메일 미리보기. 지금 보내면 나갈 기본 제목·본문과 받을 기관. 보내기와 같은 규칙으로 기관을 고른다. */
  public MailPreviewResponse reminderPreview(
      CurrentUser user, Long requestId, List<Long> organizationIds) {
    OpinionRequest request = loadOpen(user, requestId);
    ReminderTargets targets = reminderTargets(request, organizationIds);
    OpinionNotifier.Message message = notifier.reminderMessage(request, daysLeft(request));
    return MailPreviewResponse.of(
        message.title(),
        message.body(),
        targets.organizations(),
        targets.emails().size(),
        targets.skipped());
  }

  private OpinionRequest loadOpen(CurrentUser user, Long requestId) {
    OpinionRequest request = requestService.loadManaged(user, requestId);
    if (request.getStatus() != OpinionRequestStatus.OPEN) {
      throw BusinessException.conflict("OPINION_REQUEST_STATUS", "회신중 요청에만 리마인드 메일을 보냅니다.");
    }
    return request;
  }

  private long daysLeft(OpinionRequest request) {
    return ChronoUnit.DAYS.between(requestService.today(), request.getDueDate());
  }

  /** 리마인드를 받을 미회신 기관(고른 기관만, 비우면 전부)과 받을 이메일, 회신 담당자가 없어 건너뛸 기관. */
  private ReminderTargets reminderTargets(OpinionRequest request, List<Long> organizationIds) {
    Set<Long> replied =
        replyRepository.findByTarget_Request_Id(request.getId()).stream()
            .filter(OpinionReply::isReplied)
            .map(r -> r.getTarget().getId())
            .collect(Collectors.toSet());
    Set<Long> chosen =
        organizationIds == null || organizationIds.isEmpty() ? null : Set.copyOf(organizationIds);
    List<MailPreviewResponse.Organization> organizations = new ArrayList<>();
    List<String> skipped = new ArrayList<>();
    Set<String> emails = new LinkedHashSet<>();
    for (OpinionTarget target : request.getTargets()) {
      if (replied.contains(target.getId())
          || (chosen != null && !chosen.contains(target.getOrganization().getId()))) {
        continue;
      }
      if (target.getRecipients().isEmpty()) {
        skipped.add(target.getOrganization().getName());
        continue;
      }
      Set<String> mine = new LinkedHashSet<>();
      target.getRecipients().forEach(r -> mine.add(r.getEmail()));
      emails.addAll(mine);
      organizations.add(
          new MailPreviewResponse.Organization(
              target.getOrganization().getId(),
              target.getOrganization().getName(),
              target.getOrganization().getCode(),
              mine.size()));
    }
    return new ReminderTargets(organizations, emails, skipped);
  }

  private record ReminderTargets(
      List<MailPreviewResponse.Organization> organizations,
      Set<String> emails,
      List<String> skipped) {}

  /** 리마인드 메일 발송 이력, 최신순. */
  public List<OpinionReminderResponse> reminders(CurrentUser user, Long requestId) {
    OpinionRequest request = requestService.loadManaged(user, requestId);
    return reminderRepository.findByRequest_IdOrderByCreatedAtDescIdDesc(request.getId()).stream()
        .map(OpinionCollationService::toResponse)
        .toList();
  }

  OpinionCollationResponse build(OpinionRequest request) {
    Map<Long, OpinionReply> replies =
        replyRepository.findByTarget_Request_Id(request.getId()).stream()
            .collect(Collectors.toMap(r -> r.getTarget().getId(), Function.identity()));
    List<Row> rows = new ArrayList<>();
    for (OpinionTarget target : request.getTargets()) {
      OpinionReply reply = replies.get(target.getId());
      boolean replied = reply != null && reply.isReplied();
      rows.add(
          new Row(
              target.getId(),
              target.getOrganization().getId(),
              target.getOrganization().getCode(),
              target.getOrganization().getName(),
              target.getRecipients().stream().map(OpinionTargetRecipient::getName).toList(),
              replied,
              reply == null ? null : reply.getId(),
              reply == null ? null : reply.getStatus(),
              reply == null ? null : reply.getStatus().getLabel(),
              reply == null ? null : reply.getReplierName(),
              replied ? reply.getSubmittedAt() : null,
              reply == null ? null : reply.getRevisionReason(),
              reply == null ? null : reply.getRevisionRequestedAt(),
              rowAnswers(
                  request, replied, replied ? reply.getAnswers(Stage.SUBMITTED) : List.of())));
    }
    int total = rows.size();
    int repliedCount = (int) rows.stream().filter(Row::replied).count();
    List<String> notReplied =
        rows.stream().filter(r -> !r.replied()).map(Row::organizationName).toList();
    List<QuestionSummary> questions = new ArrayList<>();
    for (OpinionQuestion question : request.getQuestions()) {
      questions.add(questionSummary(request, question, replies));
    }
    return new OpinionCollationResponse(
        new Summary(
            total,
            repliedCount,
            total - repliedCount,
            total == 0 ? 0 : Math.round(repliedCount * 100f / total),
            notReplied),
        questions,
        rows);
  }

  /** 기관 하나의 질문별 답. 제출한 답으로 지나지 않은 섹션의 질문은 건너뜀이다. 미회신이면 답도 건너뜀도 없다. */
  private static List<RowAnswer> rowAnswers(
      OpinionRequest request, boolean replied, List<Answer> answers) {
    Set<Long> path = replied ? OpinionReplyService.path(request, answers) : null;
    return request.getQuestions().stream()
        .map(
            q ->
                new RowAnswer(
                    q.getId(),
                    OpinionReplyService.answerText(q, answers),
                    path != null && !path.contains(q.getId())))
        .toList();
  }

  /** 질문 하나에 대한 회신 기관들의 제출한 답. 기관은 대상에 넣은 순서다. */
  private static QuestionSummary questionSummary(
      OpinionRequest request, OpinionQuestion question, Map<Long, OpinionReply> replies) {
    Map<Long, List<String>> byOption = new LinkedHashMap<>();
    question.getOptions().forEach(o -> byOption.put(o.getId(), new ArrayList<>()));
    List<TextAnswer> texts = new ArrayList<>();
    int answered = 0;
    int skipped = 0;
    for (OpinionTarget target : request.getTargets()) {
      OpinionReply reply = replies.get(target.getId());
      if (reply == null || !reply.isReplied()) {
        continue;
      }
      String organization = target.getOrganization().getName();
      List<Answer> submitted = reply.getAnswers(Stage.SUBMITTED);
      if (!OpinionReplyService.path(request, submitted).contains(question.getId())) {
        skipped++;
        continue;
      }
      List<Answer> mine =
          submitted.stream().filter(a -> a.questionId().equals(question.getId())).toList();
      if (mine.isEmpty()) {
        continue;
      }
      answered++;
      for (Answer a : mine) {
        if (a.optionId() != null && byOption.containsKey(a.optionId())) {
          byOption.get(a.optionId()).add(organization);
        } else if (a.text() != null) {
          texts.add(new TextAnswer(target.getOrganization().getCode(), organization, a.text()));
        }
      }
    }
    return new QuestionSummary(
        question.getId(),
        question.getTitle(),
        question.getType(),
        question.getType().getLabel(),
        question.isRequired(),
        question.getSection().getSortOrder(),
        answered,
        skipped,
        question.getOptions().stream()
            .map(
                o -> {
                  List<String> names = byOption.get(o.getId());
                  return new OptionCount(o.getId(), o.getLabel(), names.size(), names);
                })
            .toList(),
        texts);
  }

  private static OpinionReminderResponse toResponse(OpinionReminder reminder) {
    return new OpinionReminderResponse(
        reminder.getId(),
        reminder.getCreatedAt(),
        reminder.getSenderName(),
        reminder.getOrganizationCount(),
        reminder.getRecipientCount(),
        reminder.getOrganizationNames().isBlank()
            ? List.of()
            : List.of(reminder.getOrganizationNames().split(", ")));
  }
}
