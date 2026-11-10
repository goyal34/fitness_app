package com.fitness.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-off clean-up for databases created by older versions, which stored a
 * plain-text placeholder password for every user. Passwords are handled by
 * Keycloak, so the column is removed. ddl-auto=update never drops columns,
 * and the old NOT NULL constraint would otherwise break new sign-ups.
 *
 * Safe to run on every start. Replace with a Flyway migration once one exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DropPasswordColumn implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS password");
        log.info("Ensured the legacy users.password column is removed.");
    }
}
