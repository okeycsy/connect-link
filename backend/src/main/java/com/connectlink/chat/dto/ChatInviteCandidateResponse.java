package com.connectlink.chat.dto;

import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import java.util.List;

/**
 * 방에 초대할 수 있는 사람 한 명. 그 업무 참가기관 담당자와 결제원 사람(업무 밖 대외계 등 포함)이다. 초대 화면은 이름 검색 대신 담당 업무와 직무(현업/전산, 전산
 * 세부 직무)로 골라 묶음으로 초대한다.
 *
 * @param businesses 담당 업무(선언 순서)
 * @param jobRole 직무(현업/전산)
 * @param itRoles 전산 세부 직무(선언 순서). 현업이면 비어 있다.
 */
public record ChatInviteCandidateResponse(
    String email,
    String name,
    String organizationCode,
    String organizationName,
    boolean kftc,
    String roleLabel,
    List<Business> businesses,
    JobRole jobRole,
    List<ItRole> itRoles) {}
