package com.connectlink.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

  /** 그 이메일로 가장 최근에 보낸 인증. 확인과 재발송 간격은 이 한 건으로 판단한다. */
  Optional<EmailVerification> findFirstByEmailOrderByIdDesc(String email);

  Optional<EmailVerification> findByTokenHash(String tokenHash);
}
