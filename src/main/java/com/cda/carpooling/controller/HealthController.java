package com.cda.carpooling.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@Slf4j
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public String health() {
        try (Connection connection = dataSource.getConnection()) {
            log.debug("Health check OK");
            return "✅ API is running and connected to PostgreSQL!";
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return "❌ Database connection failed: " + e.getMessage();
        }
    }
}