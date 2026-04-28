package com.klu.config;

import com.klu.entity.AccountStatus;
import com.klu.entity.User;
import com.klu.repo.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String redirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 authentication required");
            return;
        }

        OAuth2User principal = oauthToken.getPrincipal();
        Map<String, Object> attrs = principal.getAttributes();

        String email = extractEmail(oauthToken, attrs);
        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth2 provider did not return an email address");
            return;
        }

        String name = extractName(oauthToken, attrs, email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(name);
            u.setRole("USER");
            u.setAccountStatus(AccountStatus.APPROVED);
            u.setPassword("");
            return u;
        });

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(name);
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        if (user.getAccountStatus() == null) {
            user.setAccountStatus(AccountStatus.APPROVED);
        }

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String target = UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("token", token)
                .build()
                .toUriString();

        response.sendRedirect(target);
    }

    private String extractEmail(OAuth2AuthenticationToken token, Map<String, Object> attrs) {
        Object email = attrs.get("email");
        if (email instanceof String s && !s.isBlank()) {
            return s;
        }
        // Some providers may use different keys; try a couple common ones.
        Object preferred = attrs.get("preferred_username");
        if (preferred instanceof String s && s.contains("@")) {
            return s;
        }
        Object login = attrs.get("login");
        if ("github".equals(token.getAuthorizedClientRegistrationId()) && login instanceof String s && !s.isBlank()) {
            // GitHub may not return email unless user allows it; without email we cannot map reliably.
            return null;
        }
        return null;
    }

    private String extractName(OAuth2AuthenticationToken token, Map<String, Object> attrs, String fallbackEmail) {
        Object name = attrs.get("name");
        if (name instanceof String s && !s.isBlank()) {
            return s;
        }
        Object given = attrs.get("given_name");
        if (given instanceof String s && !s.isBlank()) {
            return s;
        }
        Object login = attrs.get("login");
        if ("github".equals(token.getAuthorizedClientRegistrationId()) && login instanceof String s && !s.isBlank()) {
            return s;
        }
        return fallbackEmail;
    }
}

