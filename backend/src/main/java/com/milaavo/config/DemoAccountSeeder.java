package com.milaavo.config;

import com.milaavo.auth.entity.User;
import com.milaavo.auth.enums.UserStatus;
import com.milaavo.auth.repository.UserRepository;
import com.milaavo.report.entity.ItemReport;
import com.milaavo.report.enums.ReportType;
import com.milaavo.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Demo profile only: makes sure the public demo account and a few sample reports exist.
// Runs on every startup, so it must be idempotent: check first, create only what's missing.
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoAccountSeeder implements ApplicationRunner {

    private final DemoAccountProperties account;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (account.mobileNumber() == null || account.password() == null) {
            log.warn("Demo profile is on, but app.demo.account is not set. Skipping the demo account");
            return;
        }

        User user = userRepository.findByMobileNumber(account.mobileNumber())
                .orElseGet(this::createDemoUser);

        if (reportRepository.countByOwnerId(user.getId()) == 0) {
            reportRepository.saveAll(sampleReports(user));
            log.info("Demo account: added sample reports");
        }
    }

    private User createDemoUser() {
        User user = new User();
        user.setName(account.name());
        user.setMobileNumber(account.mobileNumber());
        user.setPasswordHash(passwordEncoder.encode(account.password()));
        user.setMobileVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        log.info("Demo account: created {}", account.mobileNumber());
        return userRepository.saveAndFlush(user);
    }

    private List<ItemReport> sampleReports(User owner) {
        return List.of(
                report(owner, ReportType.LOST, "Black leather wallet",
                        "Lost near MG Road metro station, exit 2. Has my college ID inside.", "Bengaluru"),
                report(owner, ReportType.FOUND, "Blue steel water bottle",
                        "Found on the Hyderabad metro, Ameerpet platform. Has a cricket sticker.", "Hyderabad"),
                report(owner, ReportType.SWAPPED, "Grey laptop bag",
                        "Picked up a similar grey bag by mistake at the office cafeteria. Mine has a red tag.", "Pune"));
    }

    private ItemReport report(User owner, ReportType type, String itemName, String description, String city) {
        ItemReport report = new ItemReport();
        report.setOwnerId(owner.getId());
        report.setType(type);
        report.setItemName(itemName);
        report.setDescription(description);
        report.setCity(city);
        report.setStatus("REPORTED");
        return report;
    }
}