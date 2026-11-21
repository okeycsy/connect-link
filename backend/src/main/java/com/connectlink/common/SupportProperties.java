package com.connectlink.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 문의 안내 설정 ({@code app.support.*}).
 *
 * <ul>
 *   <li>{@code inquiry-email}: 로그인 화면 "문의 메일 보내기" 가 여는 주소. 공개 저장소라 실제 주소는 배포 설정에서만 넣는다. 비우면 화면에서
 *       문의하기 칸이 빠진다. 개인정보 처리방침의 개인정보 관리부서 메일로도 쓴다.
 *   <li>{@code privacy-department}: 개인정보 처리방침에 적는 개인정보 관리부서 이름. 비우면 "운영 담당 부서" 로 적는다.
 *   <li>{@code privacy-phone}: 개인정보 관리부서 전화번호. 비우면 처리방침에서 전화 줄이 빠진다.
 * </ul>
 *
 * <p>공개 저장소라 실제 부서와 연락처는 배포 설정에서만 넣는다.
 */
@Component
@ConfigurationProperties(prefix = "app.support")
public class SupportProperties {

  private String inquiryEmail = "";
  private String privacyDepartment = "";
  private String privacyPhone = "";

  public String getInquiryEmail() {
    return inquiryEmail;
  }

  public void setInquiryEmail(String inquiryEmail) {
    this.inquiryEmail = inquiryEmail;
  }

  public String getPrivacyDepartment() {
    return privacyDepartment;
  }

  public void setPrivacyDepartment(String privacyDepartment) {
    this.privacyDepartment = privacyDepartment;
  }

  public String getPrivacyPhone() {
    return privacyPhone;
  }

  public void setPrivacyPhone(String privacyPhone) {
    this.privacyPhone = privacyPhone;
  }
}
