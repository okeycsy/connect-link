package com.connectlink.chat.dto;

import java.util.List;
import javax.validation.constraints.Size;

/**
 * 메시지 보내기. 소통 채널에서는 파일을 보내지 않는다(결제원 파일 반출은 팀장 결재가 필요해 대화방에서는 막았다).
 *
 * @param content 본문
 * @param mentionOrganizationCodes 멘션한 기관 코드(결제원은 KFTC). 방 구성원의 기관이어야 한다
 */
public record ChatMessageSendRequest(
    @Size(max = 2000, message = "메시지는 2000자까지 쓸 수 있습니다.") String content,
    List<String> mentionOrganizationCodes) {

  public ChatMessageSendRequest {
    content = content == null ? "" : content.strip();
    mentionOrganizationCodes =
        mentionOrganizationCodes == null
            ? List.of()
            : mentionOrganizationCodes.stream()
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();
  }
}
