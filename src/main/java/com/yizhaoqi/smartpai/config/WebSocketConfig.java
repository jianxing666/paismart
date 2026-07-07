package com.yizhaoqi.smartpai.config;

import com.yizhaoqi.smartpai.handler.ChatWebSocketHandler;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

/*
这就是注册路由：告诉 Spring，"凡是 WebSocket 连接到 /chat/xxx 的请求，都交给 ChatWebSocketHandler 处理"。
{token} 是路径变量，前端把 JWT 放在这里传过来做认证。
*/
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Value("${security.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addHandler(chatWebSocketHandler, "/chat/{token}")
                .setAllowedOriginPatterns(origins);
    }
}
