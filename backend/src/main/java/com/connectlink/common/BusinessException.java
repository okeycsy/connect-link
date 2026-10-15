package com.connectlink.common;

import org.springframework.http.HttpStatus;

/** 업무 규칙 위반. 상태 코드와 업무 오류 코드를 함께 가진다. 메시지는 화면에 그대로 보여 주므로 한국어로 쓴다. */
public class BusinessException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  public BusinessException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static BusinessException notFound(String code, String message) {
    return new BusinessException(HttpStatus.NOT_FOUND, code, message);
  }

  public static BusinessException conflict(String code, String message) {
    return new BusinessException(HttpStatus.CONFLICT, code, message);
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }
}
