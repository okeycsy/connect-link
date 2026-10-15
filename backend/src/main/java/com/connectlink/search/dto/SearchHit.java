package com.connectlink.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 검색 결과 한 건. {@code link} 는 화면 주소다. */
public record SearchHit(String title, @Schema(nullable = true) String subtitle, String link) {}
