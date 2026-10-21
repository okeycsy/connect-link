package com.connectlink.suspension.dto;

import com.connectlink.organization.Business;
import java.util.List;

/**
 * 일시중단 등록 양식에서 고를 수 있는 기관 한 곳.
 *
 * @param businesses 이 기관이 참여하는 업무. 등록 양식의 대상 업무 선택지다
 */
public record SuspensionRegistrableOrganizationResponse(
    Long id, String code, String name, List<Business> businesses) {}
