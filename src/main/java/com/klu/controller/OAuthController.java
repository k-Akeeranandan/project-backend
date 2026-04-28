package com.klu.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class OAuthController {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public OAuthController(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    @GetMapping("/google")
    public Object google() {
        if (clientRegistrationRepositoryProvider.getIfAvailable() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "error", "OAuth is not configured. Set spring.security.oauth2.client.registration.google.client-id and client-secret in application.properties"
                    )
            );
        }
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/github")
    public Object github() {
        if (clientRegistrationRepositoryProvider.getIfAvailable() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "error", "OAuth is not configured. Set spring.security.oauth2.client.registration.github.client-id and client-secret in application.properties"
                    )
            );
        }
        return "redirect:/oauth2/authorization/github";
    }
}

