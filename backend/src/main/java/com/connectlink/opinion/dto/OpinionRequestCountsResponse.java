package com.connectlink.opinion.dto;

/** 목록 필터 칩의 상태별 건수. 기관 담당자의 작성중은 늘 0 이다. */
public record OpinionRequestCountsResponse(long draft, long open, long closed) {}
