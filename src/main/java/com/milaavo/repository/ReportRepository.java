package com.milaavo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.milaavo.entity.LostReport;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<LostReport, UUID> {

    List<LostReport> findAll();

    Optional<LostReport> findById(UUID id);
}
