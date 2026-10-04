package dev.meteor.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.auth.mapper.AccountMapper;
import dev.meteor.auth.mapper.projection.AccountCredentialsProjection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class DatabaseUserDetailsServiceTest {

    private final StubAccountMapper accountMapper = new StubAccountMapper();
    private final DatabaseUserDetailsService userDetailsService = new DatabaseUserDetailsService(accountMapper, true);

    @Test
    void loadsActiveAccountAndRolesUsingNormalizedEmail() {
        accountMapper.account = Optional.of(new AccountCredentialsProjection(
                2L,
                "buyer@meteor.local",
                "password-hash",
                "Meteor Buyer",
                "ACTIVE"
        ));
        accountMapper.roles = List.of("USER");

        MeteorUserPrincipal result = (MeteorUserPrincipal) userDetailsService
                .loadUserByUsername("  BUYER@METEOR.LOCAL ");

        assertThat(accountMapper.lastEmail).isEqualTo("buyer@meteor.local");
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.displayName()).isEqualTo("Meteor Buyer");
        assertThat(result.roles()).containsExactly("USER");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
    }

    @Test
    void rejectsUnknownAccountWithoutLoadingRoles() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@meteor.local"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Account not found");
        assertThat(accountMapper.rolesQueried).isFalse();
    }

    @Test
    void rejectsKnownDemoAccountWhenDemoAccountsAreDisabled() {
        DatabaseUserDetailsService disabledService = new DatabaseUserDetailsService(accountMapper, false);

        assertThatThrownBy(() -> disabledService.loadUserByUsername("buyer@meteor.local"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Account not found");
        assertThat(accountMapper.lastEmail).isNull();
        assertThat(accountMapper.rolesQueried).isFalse();
    }

    private static final class StubAccountMapper implements AccountMapper {
        private Optional<AccountCredentialsProjection> account = Optional.empty();
        private List<String> roles = List.of();
        private String lastEmail;
        private boolean rolesQueried;

        @Override
        public Optional<AccountCredentialsProjection> findByEmail(String email) {
            lastEmail = email;
            return account;
        }

        @Override
        public List<String> findRolesByUserId(long userId) {
            rolesQueried = true;
            return roles;
        }
    }
}
