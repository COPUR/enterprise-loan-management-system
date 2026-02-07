package com.amanahfi.gateway.actuator;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Profile("test")
@RestController
@RequestMapping("/actuator")
public class TestAuditEventsController {

    @GetMapping("/auditevents")
    public Map<String, Object> auditEvents() {
        return Map.of("events", List.of());
    }
}
