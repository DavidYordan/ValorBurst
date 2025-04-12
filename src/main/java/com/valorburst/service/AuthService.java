package com.valorburst.service;

import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> login(String username, String password, String machineCode);
    ResponseEntity<?> register(String username, String password, String machineCode);
    Boolean validateToken(String token);
    String getMachineCode(String token);
}
