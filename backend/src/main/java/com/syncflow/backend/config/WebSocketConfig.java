package com.syncflow.backend.config;

import com.syncflow.backend.repository.UserRepository;
import com.syncflow.backend.repository.BoardRepository;
import com.syncflow.backend.repository.WorkspaceMemberRepository;
import com.syncflow.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final String frontendOrigin;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WebSocketConfig(@Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin,
                           JwtService jwtService, UserRepository userRepository,
                           BoardRepository boardRepository, WorkspaceMemberRepository memberRepository) {
        this.frontendOrigin = frontendOrigin;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(frontendOrigin);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("WebSocket CONNECT requires a Bearer access token");
                    }
                    Long userId = jwtService.getUserId(authorization.substring(7));
                    var user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Unknown user"));
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null, AuthorityUtils.NO_AUTHORITIES));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (accessor.getUser() == null) {
                        throw new IllegalArgumentException("An authenticated session is required to subscribe");
                    }
                    if ("/user/queue/notifications".equals(destination)) return message;
                    if (destination != null && destination.matches("/topic/boards/[0-9]+")) {
                        Long boardId = Long.valueOf(destination.substring("/topic/boards/".length()));
                        var board = boardRepository.findWithWorkspace(boardId).orElse(null);
                        if (board == null || memberRepository.findByWorkspace_IdAndUser_Email(
                            board.getWorkspace().getId(), accessor.getUser().getName()).isEmpty()) {
                            throw new IllegalArgumentException("Board subscription is not allowed");
                        }
                        return message;
                    }
                    throw new IllegalArgumentException("Subscription destination is not allowed");
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    throw new IllegalArgumentException("Client messages are not accepted");
                }
                return message;
            }
        });
    }
}
