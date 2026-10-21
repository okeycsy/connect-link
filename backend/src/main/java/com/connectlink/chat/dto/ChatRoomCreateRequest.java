package com.connectlink.chat.dto;

import com.connectlink.organization.Business;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 초대방 만들기. 만든 사람은 저절로 들어간다.
 *
 * @param memberEmails 처음 초대할 사람(회사 이메일)
 */
public record ChatRoomCreateRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @NotBlank(message = "방 이름을 입력해 주세요.") @Size(max = 50, message = "방 이름은 50자까지 쓸 수 있습니다.")
        String name,
    List<String> memberEmails) {

  public ChatRoomCreateRequest {
    memberEmails = ChatInviteRequest.normalize(memberEmails);
  }
}
