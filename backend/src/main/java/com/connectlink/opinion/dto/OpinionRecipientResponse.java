package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 대상 기관의 회신 담당자 한 명(고를 때의 정보). */
public record OpinionRecipientResponse(
    Long accountId, String name, String email, @Schema(nullable = true) String subBusiness) {}
