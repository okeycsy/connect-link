package com.connectlink.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.connectlink.history.dto.FieldChange;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class FieldChangesTest {

  @Test
  void 값이_다른_항목만_남기고_불리언과_enum은_화면_이름으로_적는다() {
    List<FieldChange> changes =
        FieldChanges.compare()
            .field("name", "기관명", "가나은행", "가나은행")
            .field("active", "사용 여부", true, false)
            .field("type", "기관 구분", OrganizationType.KFTC, OrganizationType.FINANCE_ORGANIZATION)
            .field("emailDomain", "이메일 도메인", null, "  ")
            .toList();

    assertThat(changes)
        .containsExactly(
            new FieldChange("active", "사용 여부", "예", "아니요"),
            new FieldChange("type", "기관 구분", "결제원", "참가기관"));
  }

  @Test
  void 목록은_쉼표로_잇고_비면_null이다() {
    assertThat(FieldChanges.join(EnumSet.of(Business.CMS, Business.ACCOUNT_INFO)))
        .isEqualTo("계좌통합관리, CMS");
    // 순서가 없는 집합이라도 enum 선언 순서로 적는다.
    assertThat(FieldChanges.join(new HashSet<>(List.of(Business.CMS, Business.LOAN_TRANSFER))))
        .isEqualTo("대출이동, CMS");
    assertThat(FieldChanges.join(List.of())).isNull();
    assertThat(FieldChanges.compare().isEmpty()).isTrue();
  }

  @Test
  void value는_비교_없이_그대로_남긴다() {
    List<FieldChange> changes =
        FieldChanges.compare().value("code", "기관 코드", null, "GANA").toList();
    assertThat(changes).containsExactly(new FieldChange("code", "기관 코드", null, "GANA"));
  }
}
