package com.connectlink.attachment;

import com.connectlink.organization.Business;

/**
 * 첨부가 붙은 대상의 설명. 결재함에서 팀장이 어느 글의 파일인지 보고 그 화면으로 가는 데 쓴다.
 *
 * @param title 대상 이름(예: 공지 제목). 대상을 찾지 못하면 대상 종류 이름이다
 * @param business 대상의 업무. 업무가 없는 대상(전 업무 공통 공지 등)은 null 이다
 * @param path 업무 영역 아래 화면 경로(예: "notices?id=3"). 화면이 없으면 null 이다
 */
public record AttachmentOwnerInfo(String title, Business business, String path) {

  public static AttachmentOwnerInfo unknown(AttachmentOwnerType ownerType) {
    return new AttachmentOwnerInfo(ownerType.getLabel(), null, null);
  }
}
