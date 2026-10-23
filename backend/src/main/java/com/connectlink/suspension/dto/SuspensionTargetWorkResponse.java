package com.connectlink.suspension.dto;

/** 일시중단이 고른 대상 업무 하나. 이름은 고를 때의 것이다. */
public record SuspensionTargetWorkResponse(Long id, String name) {}
