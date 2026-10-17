package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 업권별 회신 양식 한 개. 양식 파일은 첨부 API(ownerType=OPINION_REPLY_FORM, ownerId=id)로 본다. */
public record OpinionReplyFormResponse(
    Long id, String name, @Schema(nullable = true) String description) {}
