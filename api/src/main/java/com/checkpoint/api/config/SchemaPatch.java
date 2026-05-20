package com.checkpoint.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs idempotent DDL fixups that Hibernate's {@code ddl-auto=update} cannot
 * perform on its own (notably dropping a NOT NULL constraint on an existing
 * column).
 *
 * <p>Each statement here must be idempotent — it runs on every application
 * startup. Add new patches by appending statements; never delete past ones.</p>
 */
@Component
@Order(0)
public class SchemaPatch implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaPatch.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaPatch(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        dropNotNullOnNewsUserId();
    }

    /**
     * TE-302: imported news (STEAM/RSS) have no human author, so {@code news.user_id}
     * must be nullable. Postgres treats {@code ALTER COLUMN ... DROP NOT NULL} as a
     * no-op when the column is already nullable, making this safe to run repeatedly.
     */
    private void dropNotNullOnNewsUserId() {
        try {
            jdbcTemplate.execute("ALTER TABLE news ALTER COLUMN user_id DROP NOT NULL");
            log.info("SchemaPatch: ensured news.user_id is nullable");
        } catch (Exception e) {
            // Table may not exist yet on a fresh install — Hibernate creates it
            // after the runner fires. The next startup will pick it up.
            log.debug("SchemaPatch: skipping news.user_id NOT NULL drop ({})", e.getMessage());
        }
    }
}
