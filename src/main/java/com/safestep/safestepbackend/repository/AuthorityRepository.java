package com.safestep.safestepbackend.repository;

import com.safestep.safestepbackend.model.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, Integer> {
    Optional<Authority> findByUsername(String username);
}