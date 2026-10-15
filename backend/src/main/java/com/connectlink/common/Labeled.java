package com.connectlink.common;

/** 화면 이름(label)을 가진 enum. 응답의 {@code xxxLabel} 필드와 변경 이력의 값 표기에 쓴다. */
public interface Labeled {
  String getLabel();
}
