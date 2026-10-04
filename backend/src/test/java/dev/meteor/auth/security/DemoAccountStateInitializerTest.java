package dev.meteor.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoAccountStateInitializerTest {

    @Test
    void activatesKnownCredentialsOnlyWhenLocalDemoSwitchIsEnabled() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();

        new DemoAccountStateInitializer(jdbcTemplate, true).run(null);

        assertThat(jdbcTemplate.sql).contains("status = 'ACTIVE'");
        assertThat(jdbcTemplate.argument).startsWith("$2a$10$");
    }

    @Test
    void locksAndInvalidatesDemoCredentialsByDefault() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();

        new DemoAccountStateInitializer(jdbcTemplate, false).run(null);

        assertThat(jdbcTemplate.sql).contains("status = 'LOCKED'");
        assertThat(jdbcTemplate.argument).isEqualTo("!demo-account-disabled!");
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private String sql;
        private String argument;

        @Override
        public int update(String sql, Object... args) {
            this.sql = sql;
            this.argument = (String) args[0];
            return 2;
        }
    }
}
