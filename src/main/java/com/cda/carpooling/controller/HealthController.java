package com.cda.carpooling.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public String health() {
        try (Connection connection = dataSource.getConnection()) {
            return "✅ API is running and connected to PostgreSQL!";
        } catch (Exception e) {
            return "❌ Database connection failed: " + e.getMessage();
        }
    }
}