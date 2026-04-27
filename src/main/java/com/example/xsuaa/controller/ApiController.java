package com.example.xsuaa.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
    @GetMapping("/open")
    public String openEndpoint() {
        return "This is an open endpoint. Anyone can access it.";
    }

    @GetMapping("/secure")
    public String secureEndpoint(@AuthenticationPrincipal Jwt jwt) {
        String user = (jwt != null) ? jwt.getClaimAsString("user_name") : "unknown";
        return "This is a secure endpoint! Hello, " + user + ". You have successfully authenticated using SAP BTP XSUAA.";
    }

}
