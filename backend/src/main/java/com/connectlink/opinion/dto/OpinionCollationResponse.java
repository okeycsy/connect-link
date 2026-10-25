package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionQuestionType;
import com.connectlink.opinion.OpinionReplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회신 현황과 기관별 취합표. 결제원 쪽 화면의 "회신 현황" 과 "취합표", 엑셀이 같은 값을 쓴다.
 *
 * @param summary 결과 요약
 * @param questions 질문별 답 모음(질문 순서). 회신한 기관의 제출한 답만 센다
 * @param rows 대상 기관마다 한 줄(대상에 넣은 순서)
 */
public record OpinionCollationResponse(
    Summary summary, List<QuestionSummary> questions, List<Row> rows) {

  /**
   * @param sectionIndex 질문이 묶인 섹션의 순서(0부터)
   * @param answeredCount 이 질문에 답한 기관 수
   * @param skippedCount 회신했지만 고른 답에 따라 이 질문의 섹션을 건너뛴 기관 수
   * @param options 객관식이면 선택지마다 고른 기관 수와 기관 이름. 주관식이면 비어 있다
   * @param textAnswers 주관식이면 기관마다의 답. 객관식이면 비어 있다
   */
  @Schema(name = "OpinionCollationQuestion")
  public record QuestionSummary(
      Long questionId,
      String title,
      OpinionQuestionType type,
      String typeLabel,
      boolean required,
      int sectionIndex,
      int answeredCount,
      int skippedCount,
      List<OptionCount> options,
      List<TextAnswer> textAnswers) {}

  @Schema(name = "OpinionCollationOptionCount")
  public record OptionCount(Long optionId, String label, int count, List<String> organizations) {}

  @Schema(name = "OpinionCollationTextAnswer")
  public record TextAnswer(String organizationCode, String organizationName, String text) {}

  /**
   * @param value 답을 글로(객관식은 고른 선택지를 ", " 로 이음). 답하지 않았으면 null
   * @param skipped 고른 답에 따라 이 질문의 섹션을 건너뛰었는지. 미회신이면 false
   */
  @Schema(name = "OpinionCollationRowAnswer")
  public record RowAnswer(
      Long questionId, @Schema(nullable = true) String value, boolean skipped) {}

  /**
   * @param replyRate 회신율(%, 반올림). 대상이 없으면 0
   */
  @Schema(name = "OpinionCollationSummary")
  public record Summary(
      int targetCount,
      int repliedCount,
      int notRepliedCount,
      int replyRate,
      List<String> notRepliedOrganizations) {}

  /**
   * @param replied 한 번이라도 제출했는지(회신/미회신 자동 구분)
   * @param replyId 회신 ID. 저장한 적이 없으면 null. 근거자료는 첨부 API 로 본다
   * @param recipientNames 회신 담당자 이름들
   * @param answers 질문마다의 제출한 답(질문 순서). 미회신이면 값이 모두 null
   */
  @Schema(name = "OpinionCollationRow")
  public record Row(
      Long targetId,
      Long organizationId,
      String organizationCode,
      String organizationName,
      List<String> recipientNames,
      boolean replied,
      @Schema(nullable = true) Long replyId,
      @Schema(nullable = true) OpinionReplyStatus replyStatus,
      @Schema(nullable = true) String replyStatusLabel,
      @Schema(nullable = true) String replierName,
      @Schema(nullable = true) LocalDateTime submittedAt,
      @Schema(nullable = true) String revisionReason,
      @Schema(nullable = true) LocalDateTime revisionRequestedAt,
      List<RowAnswer> answers) {}
}
