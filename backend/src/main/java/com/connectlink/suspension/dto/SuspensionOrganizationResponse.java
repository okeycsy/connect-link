package com.connectlink.suspension.dto;

/** 일시중단을 등록하거나 이력을 볼 수 있는 기관 한 곳. */
public record SuspensionOrganizationResponse(Long id, String code, String name) {}
