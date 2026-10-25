package com.milaavo.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.milaavo.report.entity.LostReport;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ReportRepository extends JpaRepository<LostReport, UUID> {


    Optional<LostReport> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<LostReport> findByOwnerId(UUID ownerId, Pageable pageable);
}
