package com.connectlink.organization.dto;

import com.connectlink.organization.OrganizationOverviewSectionKey;
import java.util.List;

/**
 * 기관 한눈에 보기의 칸 하나.
 *
 * @param link 그 기능 목록 화면 주소
 */
public record OrganizationOverviewSection(
    OrganizationOverviewSectionKey key,
    String title,
    List<OrganizationOverviewItem> items,
    String link) {}
