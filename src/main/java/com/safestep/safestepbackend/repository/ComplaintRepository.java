package com.safestep.safestepbackend.repository;

import com.safestep.safestepbackend.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, String> {
}