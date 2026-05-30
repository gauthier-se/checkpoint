package com.checkpoint.api.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class TestController {

    @GetMapping("/")
    public String helloWorld() {
        return "Hello, World!";
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "pong");
        return ResponseEntity.ok(response);
    }
}
