package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.KftcOfficePhone;
import org.springframework.stereotype.Component;

/**
 * 담당자 내선번호 칸을 보는 사람에 맞춰 보인다. 참가기관 담당자에게는 결제원 담당자의 4자리 내선을 대표 국번이 붙은 전체 번호로 보이고, 결제원 쪽(관리자 포함)에게는
 * 저장된 4자리 그대로 보인다. 목록·상세, 전역 검색, 엑셀, 변경 이력이 같은 규칙을 쓴다.
 */
@Component
class ContactOfficePhone {

  private final KftcOfficePhone kftcOfficePhone;
  private final ContactAccessPolicy accessPolicy;

  ContactOfficePhone(KftcOfficePhone kftcOfficePhone, ContactAccessPolicy accessPolicy) {
    this.kftcOfficePhone = kftcOfficePhone;
    this.accessPolicy = accessPolicy;
  }

  String forViewer(CurrentUser user, Contact contact) {
    return kftcOfficePhone.forViewer(
        accessPolicy.isKftcSide(user),
        contact.getOrganization().getType(),
        contact.getExtensionPhone());
  }

  /**
   * 변경 이력의 내선번호 값. 이력에는 담당자 기관이 실려 오지 않지만, 결제원 밖 담당자 번호는 전체 번호만 받으므로({@code OfficePhone}) 짧은 번호는
   * 결제원 내선이다.
   */
  String historyValueForViewer(CurrentUser user, String value) {
    return accessPolicy.isKftcSide(user) ? value : kftcOfficePhone.fullNumber(value);
  }
}
