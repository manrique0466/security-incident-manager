package com.securityincidentmanager.domain.repository;

import com.securityincidentmanager.domain.entity.RefreshToken;
import com.securityincidentmanager.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(User user);
}
