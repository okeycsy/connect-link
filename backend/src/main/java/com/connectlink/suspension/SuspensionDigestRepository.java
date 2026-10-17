package com.connectlink.suspension;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspensionDigestRepository extends JpaRepository<SuspensionDigest, Long> {

  Optional<SuspensionDigest> findBySentOn(LocalDate sentOn);
}
