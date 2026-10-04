package dev.meteor.auth.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class MeteorUserPrincipal implements UserDetails {

    private final long id;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final String status;
    private final List<String> roles;
    private final List<GrantedAuthority> authorities;

    public MeteorUserPrincipal(
            long id,
            String email,
            String passwordHash,
            String displayName,
            String status,
            List<String> roles
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
        this.roles = List.copyOf(roles);
        this.authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    public long id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> roles() {
        return roles;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equals(status);
    }
}
