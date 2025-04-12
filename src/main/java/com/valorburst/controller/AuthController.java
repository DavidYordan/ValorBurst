package com.valorburst.controller;

import com.valorburst.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用于处理登录请求
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Request request) {
        return authService.login(request.getUsername(), request.getPassword(), request.getMachineCode());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Request request) {
        return authService.register(request.getUsername(), request.getPassword(), request.getMachineCode());
    }

    @Data
    public static class Request {
        private String username;
        private String password;
        private String machineCode;
    }
}
