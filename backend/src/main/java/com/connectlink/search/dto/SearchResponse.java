package com.connectlink.search.dto;

import com.connectlink.organization.Business;
import java.util.List;

/** 전역 검색 응답. 결과가 없는 묶음은 빠진다. */
public record SearchResponse(String query, Business business, List<SearchGroup> groups) {}
