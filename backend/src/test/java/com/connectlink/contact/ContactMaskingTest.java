package com.connectlink.contact;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContactMaskingTest {

  @Test
  void 휴대폰번호는_가운데를_가린다() {
    assertThat(ContactMasking.mobile("010-1234-5678")).isEqualTo("010-****-5678");
    assertThat(ContactMasking.mobile("0111235678")).isEqualTo("011-***-5678");
    assertThat(ContactMasking.mobile(null)).isNull();
  }

  @Test
  void 이메일은_아이디_앞_두_글자만_남긴다() {
    assertThat(ContactMasking.email("hong.gildong@kftc.example.com"))
        .isEqualTo("ho***@kftc.example.com");
    assertThat(ContactMasking.email("a@b.example.com")).isEqualTo("a***@b.example.com");
    assertThat(ContactMasking.email(" ")).isNull();
  }
}
