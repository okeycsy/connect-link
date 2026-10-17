package com.connectlink.organization.dto;

import com.connectlink.organization.FinanceSector;

/** 업권 한 건. 기관 기준정보와 의견 요청 대상 고르기의 선택지다. */
public record FinanceSectorResponse(FinanceSector code, String label) {

  public static FinanceSectorResponse from(FinanceSector sector) {
    return new FinanceSectorResponse(sector, sector.getLabel());
  }
}
