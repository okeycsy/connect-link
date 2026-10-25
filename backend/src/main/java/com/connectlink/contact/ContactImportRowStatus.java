package com.connectlink.contact;

import com.connectlink.common.Labeled;

/** 담당자 일괄 등록에서 엑셀 한 줄의 처리. */
public enum ContactImportRowStatus implements Labeled {
  CREATE("등록"),
  SKIP("건너뜀"),
  ERROR("오류");

  private final String label;

  ContactImportRowStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
