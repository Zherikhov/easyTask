package com.easytask.project.repository;

import com.easytask.project.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    Optional<Board> findByProject_IdAndIsDefaultTrue(UUID projectId);
}
