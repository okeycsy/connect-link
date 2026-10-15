package com.connectlink.account;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  @EntityGraph(attributePaths = "organization")
  Optional<UserAccount> findByEmail(String email);

  @EntityGraph(attributePaths = "organization")
  List<UserAccount> findByStatus(AccountStatus status);
}
