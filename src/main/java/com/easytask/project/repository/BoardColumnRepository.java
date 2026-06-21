package com.easytask.project.repository;

import com.easytask.project.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
}
