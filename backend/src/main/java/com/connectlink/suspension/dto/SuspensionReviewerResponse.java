package com.connectlink.suspension.dto;

import java.time.LocalDateTime;

/** 확인을 시작한 결제원 담당자. */
public record SuspensionReviewerResponse(String name, String roleLabel, LocalDateTime startedAt) {}
