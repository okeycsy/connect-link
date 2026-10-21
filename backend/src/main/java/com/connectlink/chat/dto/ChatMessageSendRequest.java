package com.connectlink.chat.dto;

import java.util.List;
import javax.validation.constraints.Size;

/**
 * 메시지 보내기.
 *
 * @param content 본문. 첨부만 보낼 때(withAttachments)는 비워도 된다
 * @param mentionOrganizationCodes 멘션한 기관 코드(결제원은 KFTC). 방 구성원의 기관이어야 한다
 * @param withAttachments 보낸 뒤 첨부 API(CHAT_MESSAGE)로 파일을 올릴지
 */
public record ChatMessageSendRequest(
    @Size(max = 2000, message = "메시지는 2000자까지 쓸 수 있습니다.") String content,
    List<String> mentionOrganizationCodes,
    boolean withAttachments) {

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
