package com.connectlink.account;

import com.connectlink.organization.Business;
import java.util.Set;

/**
 * 결제원 팀 하나의 팀장(직급이 {@link AccountService#TEAM_LEADER_TITLE} 인 사용 중 결제원 계정). 결제원 담당자가 올린 파일의 반출
 * 결재자다.
 *
 * @param department 부서
 * @param team 팀
 * @param businesses 팀장이 맡은 업무. 알림을 눌렀을 때 들어갈 업무 영역을 고르는 데 쓴다
 */
public record TeamLeader(
    String email, String name, String department, String team, Set<Business> businesses) {

  public TeamLeader {
    businesses = businesses == null ? Set.of() : Set.copyOf(businesses);
  }

  /** 화면에 쓰는 소속. 예: "계좌서비스부 계좌통합팀". */
  public String teamLabel() {
    return department + " " + team;
  }
}
