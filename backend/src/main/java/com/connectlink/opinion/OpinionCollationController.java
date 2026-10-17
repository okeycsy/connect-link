package com.connectlink.opinion;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.opinion.dto.OpinionCollationResponse;
import com.connectlink.opinion.dto.OpinionReminderResponse;
import com.connectlink.opinion.dto.OpinionReminderSendRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 회신 관리와 취합: 회신 현황·기관별 취합표·엑셀, 미회신 리마인드 메일과 이력. 그 업무의 결제원 담당자만 쓴다. */
@RestController
@RequestMapping("/api/v1/opinion-requests/{requestId}")
@Tag(name = "의견 취합", description = "회신 현황, 취합표, 리마인드")
public class OpinionCollationController {

  private final OpinionCollationService collationService;
  private final CurrentUserProvider currentUserProvider;

  public OpinionCollationController(
      OpinionCollationService collationService, CurrentUserProvider currentUserProvider) {
    this.collationService = collationService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping("/collation")
  @Operation(
      summary = "회신 현황·취합표 조회",
      description = "회신/미회신 구분, 의견 유형별 기관 수와 목록, 회신율, 대상 기관마다 한 줄의 취합표.")
  public OpinionCollationResponse collation(@PathVariable Long requestId) {
    return collationService.collation(currentUserProvider.require(), requestId);
  }

  @GetMapping(value = "/collation/excel", produces = OpinionExcel.CONTENT_TYPE)
  @Operation(
      summary = "취합표 엑셀 다운로드",
      description = "결과 요약과 기관별 취합표 두 장의 xlsx. 내부망 전용이라 인터넷망이면 403 INTERNAL_ONLY.")
  public ResponseEntity<byte[]> excel(@PathVariable Long requestId) {
    OpinionExcel excel = collationService.excel(currentUserProvider.require(), requestId);
    ContentDisposition disposition =
        ContentDisposition.attachment().filename(excel.fileName(), StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(OpinionExcel.CONTENT_TYPE))
        .body(excel.content());
  }

  @GetMapping("/reminders")
  @Operation(summary = "리마인드 메일 발송 이력 조회", description = "최신순.")
  public List<OpinionReminderResponse> reminders(@PathVariable Long requestId) {
    return collationService.reminders(currentUserProvider.require(), requestId);
  }

  @PostMapping("/reminders")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "미회신 기관에 리마인드 메일 보내기",
      description =
          "고른 미회신 기관(비우면 전부)의 회신 담당자에게 알림과 메일을 보내고 이력을 남긴다. 받을 사람이 없으면 400 OPINION_REMINDER_NO_RECIPIENT.")
  public OpinionReminderResponse remind(
      @PathVariable Long requestId, @RequestBody OpinionReminderSendRequest request) {
    return collationService.remind(
        currentUserProvider.require(), requestId, request.organizationIds());
  }
}
