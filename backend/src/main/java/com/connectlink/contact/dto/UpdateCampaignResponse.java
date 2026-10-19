package com.connectlink.contact.dto;

import com.connectlink.contact.UpdateCampaignStatus;
import com.connectlink.organization.Business;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정기 변경 주간 하나와 진행 요약.
 *
 * @param targetCount 대상 기관 수. 그 업무에 참여하는 사용 중 참가기관이다
 * @param completedCount "현행화 완료" 를 누른 기관 수
 * @param changedCount 주간 동안 담당자 정보가 바뀐 기관 수
 * @param progressRate 전체 진행률(완료 기관 / 대상 기관, 0~100 정수)
 * @param autoCreated 6개월 주기 예약 작업이 스스로 등록한 주간인지
 */
public record UpdateCampaignResponse(
    Long id,
    Business business,
    String businessLabel,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    UpdateCampaignStatus status,
    String statusLabel,
    String registrantName,
    LocalDateTime createdAt,
    int targetCount,
    int completedCount,
    int changedCount,
    int progressRate,
    boolean autoCreated) {}
