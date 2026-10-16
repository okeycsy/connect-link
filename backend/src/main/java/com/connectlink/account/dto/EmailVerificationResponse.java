package com.connectlink.account.dto;

import java.time.LocalDateTime;

/**
 * 인증 코드를 보냈다. 코드는 응답에 없고 메일로만 간다(프로토타입은 발송함에 남는다).
 *
 * @param expiresAt 코드 유효 시각(서버 시간대 Asia/Seoul)
 * @param resendAfterSeconds 코드를 다시 받을 수 있을 때까지 남은 초. 화면이 이 값으로 센다(브라우저 시간대와 상관없게)
 */
public record EmailVerificationResponse(
    String email, LocalDateTime expiresAt, long resendAfterSeconds) {}
