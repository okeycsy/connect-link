package com.connectlink.account.dto;

import java.time.LocalDateTime;

/**
 * 인증 코드를 보냈다. 코드는 응답에 없고 메일로만 간다(프로토타입은 발송함에 남는다).
 *
 * @param expiresAt 코드 유효 시각
 * @param resendAvailableAt 코드를 다시 받을 수 있는 시각
 */
public record EmailVerificationResponse(
    String email, LocalDateTime expiresAt, LocalDateTime resendAvailableAt) {}
