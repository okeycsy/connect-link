package com.connectlink.common;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * 업무 규칙 위반. 상태 코드와 업무 오류 코드를 함께 가진다. 메시지는 화면에 그대로 보여 주므로 한국어로 쓴다. 특정 입력 칸 때문이면 {@link #onField} 로
 * 만들어 응답의 {@code fieldErrors} 에도 싣는다(화면은 그 칸 아래에 보여 준다).
 */
public class BusinessException extends RuntimeException {

  private final HttpStatus status;
  private final String code;
  private final List<ErrorResponse.FieldError> fieldErrors;

  public BusinessException(HttpStatus status, String code, String message) {
    this(status, code, message, List.of());
  }

  private BusinessException(
      HttpStatus status, String code, String message, List<ErrorResponse.FieldError> fieldErrors) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }

  /** 요청 본문의 한 칸({@code field}) 때문에 생긴 위반. 메시지는 본문 오류와 그 칸의 오류에 함께 쓴다. */
  public static BusinessException onField(
      HttpStatus status, String code, String field, String message) {
    return new BusinessException(
        status, code, message, List.of(new ErrorResponse.FieldError(field, message)));
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

  public List<ErrorResponse.FieldError> getFieldErrors() {
    return fieldErrors;
  }
}
