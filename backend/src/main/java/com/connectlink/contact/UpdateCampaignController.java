package com.connectlink.contact;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.contact.dto.CurrentUpdateCampaignResponse;
import com.connectlink.contact.dto.UpdateCampaignProgressResponse;
import com.connectlink.contact.dto.UpdateCampaignReminderRequest;
import com.connectlink.contact.dto.UpdateCampaignReminderResultResponse;
import com.connectlink.contact.dto.UpdateCampaignResponse;
import com.connectlink.contact.dto.UpdateCampaignSaveRequest;
import com.connectlink.contact.dto.UpdateCampaignScheduleResponse;
import com.connectlink.notification.dto.MailPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 정기 변경(현행화) 주간: 결제원 담당자의 등록·수정·삭제, 진행률과 기관별 현행화, 진행 중인 주간 안내. */
@RestController
@RequestMapping("/api/v1/update-campaigns")
@Tag(name = "정기 변경 주간", description = "담당자 연락처 현행화 주간 등록과 진행률")
public class UpdateCampaignController {

  private final UpdateCampaignService campaignService;
  private final CurrentUserProvider currentUserProvider;

  public UpdateCampaignController(
      UpdateCampaignService campaignService, CurrentUserProvider currentUserProvider) {
    this.campaignService = campaignService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(summary = "정기 변경 주간 목록 조회", description = "한 업무의 주간 전부, 최근 시작 순. 결제원 담당자만 본다.")
  public List<UpdateCampaignResponse> list(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return campaignService.list(
        currentUserProvider.require(), ContactController.resolveBusiness(business));
  }

  @GetMapping("/current")
  @Operation(
      summary = "진행 중인 정기 변경 주간 조회",
      description =
          "연락처 화면 위쪽 안내. 담당자 목록을 볼 수 있는 누구나 보고, 기관 담당자는 자기 기관 현행화도 받는다. 진행 중인 주간이 없으면 campaign 이 null.")
  public CurrentUpdateCampaignResponse current(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return campaignService.current(
        currentUserProvider.require(), ContactController.resolveBusiness(business));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "정기 변경 주간 등록",
      description =
          "그 업무 결제원 담당자가 등록한다. 그 업무 참가기관 담당자에게 알림 센터와 메일로 알린다. 같은 업무의 다른 주간과 기간이 겹치면 409 UPDATE_CAMPAIGN_OVERLAP.")
  public UpdateCampaignResponse create(@Valid @RequestBody UpdateCampaignSaveRequest request) {
    return campaignService.create(currentUserProvider.require(), request);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "정기 변경 주간 수정",
      description = "이름과 기간을 고친다. 끝난 주간이면 409 UPDATE_CAMPAIGN_CLOSED.")
  public UpdateCampaignResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateCampaignSaveRequest request) {
    return campaignService.update(currentUserProvider.require(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "정기 변경 주간 삭제",
      description = "아직 시작하지 않은 주간만 지운다. 시작했으면 409 UPDATE_CAMPAIGN_STARTED.")
  public void delete(@PathVariable Long id) {
    campaignService.delete(currentUserProvider.require(), id);
  }

  @GetMapping("/{id}/progress")
  @Operation(
      summary = "정기 변경 진행률 조회",
      description =
          "전체 진행률과 기관별 현행화 완료·미완료, 정보 변경 여부. filter 로 미확인(INCOMPLETE)·정보 변경(CHANGED) 기관만 따로 본다. 결제원 담당자만 본다.")
  public UpdateCampaignProgressResponse progress(
      @PathVariable Long id,
      @Parameter(description = "ALL(기본), COMPLETED, INCOMPLETE, CHANGED")
          @RequestParam(defaultValue = "ALL")
          UpdateCampaignProgressFilter filter) {
    return campaignService.progress(currentUserProvider.require(), id, filter);
  }

  @GetMapping("/schedule")
  @Operation(
      summary = "정기 변경 주기 조회",
      description =
          "주기(기본 6개월)와 다음 정기 변경 요청일, 자동 등록 여부, 리마인드 간격. 다음 요청일은 최근 주간 시작일 + 주기이고, 주간이 없으면 null. 결제원 담당자만 본다.")
  public UpdateCampaignScheduleResponse schedule(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return campaignService.schedule(
        currentUserProvider.require(), ContactController.resolveBusiness(business));
  }

  @PostMapping("/{id}/reminders")
  @Operation(
      summary = "미회신 기관에 리마인드 메일 보내기",
      description =
          "진행 중 주간에서 아직 현행화 완료를 누르지 않은 기관에 지금 리마인드 메일을 보낸다. organizationIds 를 비우면 미완료 기관 전부. subject·body 를 넘기면 기본 문구 대신 그 문구로 보낸다. 받는 사람은 그 기관·업무의 사용 중 계정과 담당자 연락처 이메일. 진행 중이 아니면 409 UPDATE_CAMPAIGN_NOT_OPEN. 결제원 담당자만 보낸다.")
  public UpdateCampaignReminderResultResponse remind(
      @PathVariable Long id,
      @Valid @RequestBody(required = false) UpdateCampaignReminderRequest request) {
    return campaignService.sendReminders(currentUserProvider.require(), id, request);
  }

  @GetMapping("/{id}/reminder-preview")
  @Operation(
      summary = "리마인드 메일 미리보기",
      description =
          "보내기 전에 기본 제목·본문과 받을 기관(고른 미완료 기관, 비우면 전부)을 본다. 받을 사람이 없는 기관은 건너뛸 기관으로 따로 준다. 진행 중이 아니면 409 UPDATE_CAMPAIGN_NOT_OPEN. 결제원 담당자만 본다.")
  public MailPreviewResponse reminderPreview(
      @PathVariable Long id,
      @Parameter(description = "고른 기관 ID. 비우면 미완료 기관 전부") @RequestParam(required = false)
          List<Long> organizationIds) {
    return campaignService.reminderPreview(currentUserProvider.require(), id, organizationIds);
  }
}
