package dev.meteor.auth.controller;

import dev.meteor.auth.dto.CsrfTokenResponse;
import dev.meteor.auth.dto.CurrentUserResponse;
import dev.meteor.auth.dto.LoginRequest;
import dev.meteor.auth.security.MeteorUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @GetMapping("/api/auth/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );
    }

    @PostMapping("/api/auth/login")
    public CurrentUserResponse login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.email(), loginRequest.password())
        );
        // 认证成功后更换 Session ID 并刷新 CSRF，防止沿用登录前的会话凭证。
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        // 当前线程的认证上下文不会自动跨请求保存；显式写入 Session 后，下次请求才能恢复用户。
        securityContextRepository.saveContext(context, request, response);

        return toResponse((MeteorUserPrincipal) authentication.getPrincipal());
    }

    @GetMapping("/api/me")
    public CurrentUserResponse me(Authentication authentication) {
        return toResponse((MeteorUserPrincipal) authentication.getPrincipal());
    }

    private CurrentUserResponse toResponse(MeteorUserPrincipal principal) {
        return new CurrentUserResponse(
                principal.id(),
                principal.getUsername(),
                principal.displayName(),
                principal.roles()
        );
    }
}
