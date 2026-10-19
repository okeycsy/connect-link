package com.connectlink.contact.demo;

import com.connectlink.contact.ContactService;
import com.connectlink.contact.dto.ContactReconfirmRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 시연용: 6개월 미확인 알림 예약 작업을 지금 한 번 돌린다(CLAUDE.md 6절 "예약 작업"). local·demo·test 프로필에만 있다. */
@RestController
@RequestMapping("/api/v1/demo/contact-reconfirm-runs")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class ContactDemoController {

  private final ContactService contactService;

  public ContactDemoController(ContactService contactService) {
    this.contactService = contactService;
  }

  @PostMapping
  @Operation(
      summary = "6개월 미확인 알림 지금 보내기(데모)",
      description = "매일 9시 예약 작업과 같다. 이번 미확인 기간에 이미 알린 담당자는 건너뛴다.")
  public ContactReconfirmRunResponse run() {
    return new ContactReconfirmRunResponse(contactService.notifyReconfirmDue());
  }
}
