package com.milaavo.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.milaavo.report.entity.ItemReport;
import com.milaavo.report.enums.ReportType;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportRepository extends JpaRepository<ItemReport, UUID> {

    Optional<ItemReport> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<ItemReport> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<ItemReport> findByOwnerIdAndType(UUID ownerId, ReportType type, Pageable pageable);

    long countByOwnerId(UUID ownerId);
}
