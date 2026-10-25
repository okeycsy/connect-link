package com.connectlink.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

  /** 그 이메일로 가장 최근에 보낸 비밀번호 찾기 코드. 확인과 재발송 간격은 이 한 건으로 판단한다. */
  Optional<PasswordReset> findFirstByEmailOrderByIdDesc(String email);

  /** 탈퇴한 사람의 비밀번호 찾기 기록을 지운다. */
  long deleteByEmail(String email);
}
