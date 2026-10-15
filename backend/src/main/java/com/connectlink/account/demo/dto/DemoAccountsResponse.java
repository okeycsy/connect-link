package com.connectlink.account.demo.dto;

import java.util.List;

/** 데모 계정 목록과 공통 비밀번호. */
public record DemoAccountsResponse(String password, List<DemoAccountResponse> accounts) {}
