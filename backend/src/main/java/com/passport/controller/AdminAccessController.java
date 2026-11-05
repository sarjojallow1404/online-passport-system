package com.passport.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAccessController {

    @GetMapping("/access-check")
    public Map<String, String> accessCheck() {
        return Map.of(
            "status", "success",
            "message", "Administrator access verified"
        );
    }
}
