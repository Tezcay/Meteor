package dev.meteor.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoAccountStateInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD_HASH =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
    private static final String DISABLED_PASSWORD = "!demo-account-disabled!";

    private final JdbcTemplate jdbcTemplate;
    private final boolean demoAccountsEnabled;

    public DemoAccountStateInitializer(
            JdbcTemplate jdbcTemplate,
            @Value("${meteor.demo-accounts.enabled:false}") boolean demoAccountsEnabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.demoAccountsEnabled = demoAccountsEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (demoAccountsEnabled) {
            jdbcTemplate.update("""
                    UPDATE users
                    SET password_hash = ?, status = 'ACTIVE'
                    WHERE email IN ('buyer@meteor.local', 'creator@meteor.local')
                    """, DEMO_PASSWORD_HASH);
            return;
        }
        jdbcTemplate.update("""
                UPDATE users
                SET password_hash = ?, status = 'LOCKED'
                WHERE email IN ('buyer@meteor.local', 'creator@meteor.local')
                """, DISABLED_PASSWORD);
    }
}
