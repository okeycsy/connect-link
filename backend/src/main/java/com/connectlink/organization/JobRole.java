package com.connectlink.organization;

import com.connectlink.common.Labeled;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

/** 직무. 현업이거나 전산이다. 전산이면 세부 직무({@link ItRole})를 하나 이상 고른다. */
public enum JobRole implements Labeled {
  BUSINESS("현업"),
  IT("전산");

  private final String label;

  JobRole(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }

  /** 화면 표기. 현업은 "현업", 전산은 "전산(운영, 상황실)" 꼴이다. 세부 직무는 선언 순서로 적는다. */
  public String describe(Collection<ItRole> itRoles) {
    if (this != IT || itRoles == null || itRoles.isEmpty()) {
      return label;
    }
    return label
        + "("
        + itRoles.stream()
            .sorted(Comparator.naturalOrder())
            .map(ItRole::getLabel)
            .collect(Collectors.joining(", "))
        + ")";
  }
}
