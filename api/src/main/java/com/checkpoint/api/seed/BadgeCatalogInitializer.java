package com.checkpoint.api.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.enums.BadgeCode;
import com.checkpoint.api.repositories.BadgeRepository;

/**
 * Materialises the {@link BadgeCode} catalog into the {@code badges} table at startup.
 *
 * <p>Runs in every environment, unlike {@link DatabaseSeeder} which is gated behind the
 * {@code seed} profile. The catalog is not optional demo data: an empty {@code badges}
 * table makes {@code awardIfEligible} a no-op for every user and leaves the profile badge
 * section blank, since the profile exposes the whole catalog (earned badges in colour,
 * the rest locked) rather than only the badges a user owns.
 *
 * <p>Idempotent: missing codes are inserted, and the {@code hidden} flag is re-synced
 * because it drives server-side behaviour and is owned by the enum. The display name,
 * description and picture are only written on insert, so later edits made in the DB
 * survive a restart.
 *
 * <p>Ordered ahead of {@link DatabaseSeeder} so the seeder can attach badges to the
 * fake users it generates.
 */
@Component
@Order(0)
public class BadgeCatalogInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BadgeCatalogInitializer.class);

    private final BadgeRepository badgeRepository;

    public BadgeCatalogInitializer(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        int resynced = 0;

        for (BadgeCode code : BadgeCode.values()) {
            Badge existing = badgeRepository.findByCode(code.name()).orElse(null);
            if (existing == null) {
                badgeRepository.save(new Badge(
                        code.name(),
                        code.getDefaultName(),
                        code.getDefaultDescription(),
                        null,
                        code.isHidden()));
                created++;
                continue;
            }
            if (existing.isHidden() != code.isHidden()) {
                existing.setHidden(code.isHidden());
                badgeRepository.save(existing);
                resynced++;
            }
        }

        log.info("Badge catalog: {} new badge(s) inserted, {} visibility flag(s) re-synced ({} total in catalog).",
                created, resynced, BadgeCode.values().length);
    }
}
