package com.connectlink.account.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이메일 인증을 마쳤다. 가입 신청은 {@code verificationToken} 을 함께 보내 이 이메일을 인증했음을 증명한다.
 *
 * @param tokenExpiresAt 이 시각까지 가입 신청을 마쳐야 한다
 * @param suggestedOrganizationIds 이메일 도메인이 같은 기관(가입할 수 있는 기관만). 가입 화면이 미리 골라 둔다
 */
public record EmailVerificationConfirmResponse(
    String email,
    String verificationToken,
    LocalDateTime tokenExpiresAt,
    List<Long> suggestedOrganizationIds) {}
