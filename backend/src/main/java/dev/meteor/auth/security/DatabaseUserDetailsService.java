package dev.meteor.auth.security;

import dev.meteor.auth.mapper.AccountMapper;
import dev.meteor.auth.mapper.projection.AccountCredentialsProjection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final Set<String> DEMO_ACCOUNT_EMAILS = Set.of(
            "buyer@meteor.local",
            "creator@meteor.local"
    );

    private final AccountMapper accountMapper;
    private final boolean demoAccountsEnabled;

    public DatabaseUserDetailsService(
            AccountMapper accountMapper,
            @Value("${meteor.demo-accounts.enabled:false}") boolean demoAccountsEnabled
    ) {
        this.accountMapper = accountMapper;
        this.demoAccountsEnabled = demoAccountsEnabled;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedEmail = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (!demoAccountsEnabled && DEMO_ACCOUNT_EMAILS.contains(normalizedEmail)) {
            throw new UsernameNotFoundException("Account not found");
        }
        AccountCredentialsProjection account = accountMapper.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        List<String> roles = accountMapper.findRolesByUserId(account.id());
        return new MeteorUserPrincipal(
                account.id(),
                account.email(),
                account.passwordHash(),
                account.displayName(),
                account.status(),
                roles
        );
    }
}
