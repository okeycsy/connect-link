package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 대상 기관의 회신 담당자 한 명(고를 때의 정보). 담당자 연락처와는 회사 이메일로 잇는다. */
public record OpinionRecipientResponse(
    String name, String email, @Schema(nullable = true) String subBusiness) {}
