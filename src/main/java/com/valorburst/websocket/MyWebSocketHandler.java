package com.valorburst.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valorburst.dto.AddMissionRequestDto;
import com.valorburst.dto.MissionResponseDto;
import com.valorburst.dto.PircesResponseDto;
import com.valorburst.dto.UserResponseDto;
import com.valorburst.dto.WebSocketDto;
import com.valorburst.service.MissionService;
import com.valorburst.service.WebSocketService;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final MissionService missionService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // 握手成功后调用
        Map<String, Object> attrs = session.getAttributes();
        String machineCode = (String) attrs.get("machineCode");
        log.info("[WebSocket] 连接已建立, machineCode={}", machineCode);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("[WebSocket] 收到消息: {}", payload);

        WebSocketDto<?> request;
        try {
            request = objectMapper.readValue(payload, WebSocketDto.class);
        } catch (Exception e) {
            log.warn("无法解析请求: {}", e.getMessage());
            session.sendMessage(new TextMessage("格式错误: " + e.getMessage()));
            return;
        }

        String type = request.getType();
        String requestId = request.getRequestId();

        String responseJson = null;
        if (type.equals("GET_ALL_USERS")) {
            List<UserResponseDto> data = webSocketService.getAllUsers();
            responseJson = objectMapper.writeValueAsString(
                    WebSocketDto.<List<UserResponseDto>>builder()
                        .type(type)
                        .requestId(requestId)
                        .message("获取成功")
                        .data(data)
                        .build());
        } else if (type.equals("UPDATE_USER_BY_INPUT")) {
            if (request.getData() instanceof String input) {
                UserResponseDto data = webSocketService.updateUserByInput(input);
                responseJson = objectMapper.writeValueAsString(
                        WebSocketDto.<UserResponseDto>builder()
                            .type(type)
                            .requestId(requestId)
                            .message("更新成功")
                            .data(data)
                            .build());
            }
        } else if (type.equals("GET_MISSIONS")) {
            Integer input = objectMapper.convertValue(request.getData(), Integer.class);
            List<MissionResponseDto> data = webSocketService.getAllMissions(input);
            responseJson = objectMapper.writeValueAsString(
                    WebSocketDto.<List<MissionResponseDto>>builder()
                        .type(type)
                        .requestId(requestId)
                        .message("获取成功")
                        .data(data)
                        .build());
        } else if (type.equals("GET_PRICES")) {
            PircesResponseDto data = webSocketService.getPrices();
            responseJson = objectMapper.writeValueAsString(
                    WebSocketDto.<PircesResponseDto>builder()
                        .type(type)
                        .requestId(requestId)
                        .message("获取成功")
                        .data(data)
                        .build());
        } else if (type.equals("ADD_MISSION")) {
            AddMissionRequestDto data = objectMapper.convertValue(
                request.getData(),
                new TypeReference<AddMissionRequestDto>() {}
            );
            try {
                missionService.addMissions(data);
                responseJson = objectMapper.writeValueAsString(
                        WebSocketDto.<List<AddMissionRequestDto>>builder()
                            .type(type)
                            .requestId(requestId)
                            .message("添加成功")
                            .build());
            } catch (Exception e) {
                log.error("添加任务失败: {}", e.getMessage());
                responseJson = objectMapper.writeValueAsString(
                        WebSocketDto.<List<AddMissionRequestDto>>builder()
                            .type(type)
                            .requestId(requestId)
                            .message("添加失败: " + e.getMessage())
                            .build());
            }
        }

        if (responseJson == null) {
            log.warn("[WebSocket] 未知消息: {}", payload);
            responseJson = objectMapper.writeValueAsString(WebSocketDto.builder()
                    .type("ERROR")
                    .requestId(requestId)
                    .message("未知消息: " + type)
                    .build());
            session.sendMessage(new TextMessage(responseJson));
        } else {
            session.sendMessage(new TextMessage(responseJson));
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("[WebSocket] 异常", exception);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("[WebSocket] 连接关闭, status={}", status);
    }
}
