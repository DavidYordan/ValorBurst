package com.valorburst.service.impl;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.valorburst.config.AppProperties;
import com.valorburst.model.local.AuthModel;
import com.valorburst.repository.local.AuthModelRepository;
import com.valorburst.service.AuthService;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthModelRepository authModelRepository;

    private final AppProperties appProperties;
    private Key secretKey;

    @PostConstruct
    public void init() {
        String secretKeyString = appProperties.getAuth().getSecretKey();
        secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secretKeyString.getBytes());
        log.info("JWT密钥初始化完成");
    }

    /**
     * 服务器端验证用户名、密码、机器码是否正确
     * 若正确则生成 JWT Token 返回；否则抛异常或返回null
     */
    public ResponseEntity<?> login(String username, String password, String machineCode) {
        AuthModel authModel = authModelRepository.findByUsername(username);

        if (authModel == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }

        if (!authModel.getPassword().equals(password)) {
            return ResponseEntity.badRequest().body("密码错误");
        }

        if (!authModel.getMachineCode().equals(machineCode)) {
            return ResponseEntity.badRequest().body("设备未绑定");
        }

        if (authModel.getPermission() == 0) {
            return ResponseEntity.badRequest().body("请等待授权");
        }

        String token = Jwts.builder()
                .setSubject(username)
                .claim("machineCode", machineCode)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .signWith(secretKey)
                .compact();

        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * 注册用户
     */
    public ResponseEntity<?> register(String username, String password, String machineCode) {
        AuthModel authModel = authModelRepository.findByUsername(username);

        if (authModel != null) {
            if (authModel.getPermission() == 0) {
                return ResponseEntity.ok("登记成功，请等待授权");
            } else {
                return login(username, password, machineCode);
            }
        }

        authModel = AuthModel.builder()
                .username(username)
                .password(password)
                .machineCode(machineCode)
                .permission(0)
                .build();
        authModelRepository.save(authModel);

        return ResponseEntity.ok("登记成功，请等待授权");
    }

    /**
     * 用于后续在WebSocket握手或消息处理中验证 Token 的有效性
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析获取机器码 (如果需要做二次校验)
     */
    public String getMachineCode(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("machineCode", String.class);
        } catch (Exception e) {
            log.error("获取machineCode失败: {}", e.getMessage());
            return null;
        }
    }
}
