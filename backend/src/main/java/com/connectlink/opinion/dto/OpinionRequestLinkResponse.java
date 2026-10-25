package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionRequestStatus;

/**
 * 다른 화면(공지 등)에 이어 보이는 의견 요청 한 건. 볼 수 있는 사람에게만 준다.
 *
 * @param replyable 지금 이 사용자가 회신할 수 있는지(회신중 요청의 대상 기관 담당자)
 */
public record OpinionRequestLinkResponse(
    Long id, String title, OpinionRequestStatus status, boolean replyable) {}
