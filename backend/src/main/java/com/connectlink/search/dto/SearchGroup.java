package com.connectlink.search.dto;

import com.connectlink.search.SearchCategory;
import com.connectlink.search.SearchSource;
import java.util.List;

/** 묶음 하나(기관, 담당자 …)의 결과. */
public record SearchGroup(
    SearchCategory category, String label, SearchSource source, List<SearchHit> hits) {}
