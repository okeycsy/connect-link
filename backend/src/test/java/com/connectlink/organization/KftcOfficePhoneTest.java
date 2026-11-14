package com.connectlink.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KftcOfficePhoneTest {

  private final KftcOfficePhone phone = new KftcOfficePhone("02-000-");

  @Test
  void 결제원_내선은_대표_국번을_붙인다() {
    assertThat(phone.fullNumber("1101")).isEqualTo("02-000-1101");
  }

  @Test
  void 이미_전체_번호면_그대로다() {
    assertThat(phone.fullNumber("02-0000-2201")).isEqualTo("02-0000-2201");
  }

  @Test
  void 국번_설정이_없으면_그대로다() {
    assertThat(new KftcOfficePhone("").fullNumber("1101")).isEqualTo("1101");
  }

  @Test
  void 결제원_쪽이_보거나_결제원_담당자가_아니면_바꾸지_않는다() {
    assertThat(phone.forViewer(true, OrganizationType.KFTC, "1101")).isEqualTo("1101");
    assertThat(phone.forViewer(false, OrganizationType.FINANCE_ORGANIZATION, "1101"))
        .isEqualTo("1101");
    assertThat(phone.forViewer(false, OrganizationType.KFTC, "1101")).isEqualTo("02-000-1101");
  }
}
