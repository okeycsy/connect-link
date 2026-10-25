package com.connectlink.account.dto;

/**
 * 비밀번호 찾기 코드를 보냈다(계정이 없는 이메일에도 같은 답이다).
 *
 * @param email 소문자로 맞춘 이메일
 * @param resendAfterSeconds 코드를 다시 받을 수 있을 때까지 기다리는 초
 */
public record PasswordResetResponse(String email, long resendAfterSeconds) {}
