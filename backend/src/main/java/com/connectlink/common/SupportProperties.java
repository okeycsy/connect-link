package com.connectlink.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 문의 안내 설정 ({@code app.support.*}).
 *
 * <ul>
 *   <li>{@code inquiry-email}: 로그인 화면 "문의 메일 보내기" 가 여는 주소. 공개 저장소라 실제 주소는 배포 설정에서만 넣는다.
 *       비우면 화면에서 문의하기 칸이 빠진다.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.support")
public class SupportProperties {

  private String inquiryEmail = "";

  public String getInquiryEmail() {
    return inquiryEmail;
  }

  public void setInquiryEmail(String inquiryEmail) {
    this.inquiryEmail = inquiryEmail;
  }
}
