package com.easytask.project.repository;

import com.easytask.project.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StatusRepository extends JpaRepository<Status, UUID> {

    List<Status> findByProject_IdOrderByPosition(UUID projectId);
}
