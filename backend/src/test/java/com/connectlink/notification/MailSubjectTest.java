package com.connectlink.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.connectlink.organization.Business;
import java.util.List;
import org.junit.jupiter.api.Test;

class MailSubjectTest {

  @Test
  void 업무_메일은_금융결제원과_업무명을_앞에_붙인다() {
    assertThat(MailSubject.of(Business.CMS, "의견 요청: 출금 한도"))
        .isEqualTo("[금융결제원] (CMS) 의견 요청: 출금 한도");
  }

  @Test
  void 업무가_없거나_계정_메일이면_연결이다() {
    assertThat(MailSubject.of((Business) null, "알림")).isEqualTo("[금융결제원] (연결이) 알림");
    assertThat(MailSubject.service("회사 이메일 인증 코드")).isEqualTo("[금융결제원] (연결이) 회사 이메일 인증 코드");
  }

  @Test
  void 여러_업무는_업무_순서대로_잇고_없으면_연결이다() {
    assertThat(MailSubject.of(List.of(Business.CMS, Business.ACCOUNT_INFO), "점검"))
        .isEqualTo("[금융결제원] (계좌통합관리·CMS) 점검");
    assertThat(MailSubject.of(List.of(), "점검")).isEqualTo("[금융결제원] (연결이) 점검");
  }

  @Test
  void 이미_금융결제원으로_시작하면_그대로_둔다() {
    String reminder = "[금융결제원] (계좌통합관리) 정기 변경 리마인드: 하반기 (~2026-11-14)";
    assertThat(MailSubject.of(Business.ACCOUNT_INFO, reminder)).isEqualTo(reminder);
  }
}
