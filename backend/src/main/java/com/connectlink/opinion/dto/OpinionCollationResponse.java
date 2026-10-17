package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionReplyStatus;
import com.connectlink.opinion.OpinionReplyType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회신 현황과 기관별 취합표. 결제원 쪽 화면의 "회신 현황" 과 "취합표", 엑셀이 같은 값을 쓴다.
 *
 * @param summary 결과 요약
 * @param rows 대상 기관마다 한 줄(대상에 넣은 순서)
 */
public record OpinionCollationResponse(Summary summary, List<Row> rows) {

  /**
   * @param replyRate 회신율(%, 반올림). 대상이 없으면 0
   * @param byType 의견 유형별 기관 수와 기관 이름(동의, 조건부 동의, 이견 순)
   */
  public record Summary(
      int targetCount,
      int repliedCount,
      int notRepliedCount,
      int replyRate,
      List<TypeCount> byType,
      List<String> notRepliedOrganizations) {}

  public record TypeCount(
      OpinionReplyType type, String label, int count, List<String> organizations) {}

  /**
   * @param replied 한 번이라도 제출했는지(회신/미회신 자동 구분)
   * @param replyId 회신 ID. 저장한 적이 없으면 null. 근거자료는 첨부 API 로 본다
   * @param type 제출한 의견 유형. 미회신이면 null
   * @param recipientNames 회신 담당자 이름들
   */
  public record Row(
      Long targetId,
      Long organizationId,
      String organizationCode,
      String organizationName,
      @Schema(nullable = true) String formName,
      List<String> recipientNames,
      boolean replied,
      @Schema(nullable = true) Long replyId,
      @Schema(nullable = true) OpinionReplyStatus replyStatus,
      @Schema(nullable = true) String replyStatusLabel,
      @Schema(nullable = true) OpinionReplyType type,
      @Schema(nullable = true) String typeLabel,
      @Schema(nullable = true) String content,
      @Schema(nullable = true) String replierName,
      @Schema(nullable = true) LocalDateTime submittedAt,
      @Schema(nullable = true) String revisionReason,
      @Schema(nullable = true) LocalDateTime revisionRequestedAt) {}
}
