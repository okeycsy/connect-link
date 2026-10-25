package com.connectlink.contact.dto;

import java.util.List;

/**
 * 담당자 일괄 등록 결과. 한 줄이라도 오류면 아무것도 등록하지 않는다({@code saved} false).
 *
 * @param createCount 등록할(했던) 줄 수
 * @param skipCount 같은 이메일이 이미 있어 건너뛸 줄 수
 * @param errorCount 오류 줄 수
 * @param saved 실제로 등록했는지. 검사만 했거나 오류가 있으면 false
 */
public record ContactImportResponse(
    int totalCount,
    int createCount,
    int skipCount,
    int errorCount,
    boolean saved,
    List<ContactImportRowResponse> rows) {}
