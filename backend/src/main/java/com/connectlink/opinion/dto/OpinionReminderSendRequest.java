package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 리마인드 메일 보내기.
 *
 * @param organizationIds 보낼 미회신 기관. 비우면 미회신 기관 전부에 보낸다. 이미 회신한 기관은 빠진다
 */
public record OpinionReminderSendRequest(@Schema(nullable = true) List<Long> organizationIds) {}
