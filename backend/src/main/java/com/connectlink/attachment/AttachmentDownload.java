package com.connectlink.attachment;

import org.springframework.core.io.Resource;

/** 내려받기 한 건: 파일 정보와 내용. Controller 가 응답 헤더를 만드는 데 쓴다. */
public record AttachmentDownload(Attachment attachment, Resource content) {}
