package com.aditya.worldcup.shared.security;

import com.aditya.worldcup.security.CustomUserDetailsService;
import com.aditya.worldcup.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization == null || authorization.isEmpty() || !authorization.get(0).startsWith("Bearer ")) {
                log.warn("STOMP CONNECT failed: Missing or invalid Authorization header");
                throw new IllegalArgumentException("Invalid authentication");
            }
            
            String token = authorization.get(0).substring(7);
            try {
                String email = jwtService.extractUsername(token);
                if (email != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    
                    StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
                    mutableAccessor.setUser(authentication);
                    log.debug("STOMP CONNECT authenticated for user: {}", email);
                    return org.springframework.messaging.support.MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
                } else {
                    throw new IllegalArgumentException("Invalid token payload");
                }
            } catch (Exception e) {
                log.error("STOMP CONNECT failed: Token validation error");
                throw new IllegalArgumentException("Authentication failed");
            }
        } 
        else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            org.springframework.security.core.Authentication auth = (org.springframework.security.core.Authentication) accessor.getUser();
            if (auth == null || !auth.isAuthenticated()) {
                log.warn("STOMP SUBSCRIBE blocked: Unauthenticated attempt to {}", accessor.getDestination());
                throw new IllegalArgumentException("Unauthorized to subscribe");
            }
            
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/matches/")) {
                // Topic authorization can occur here if specific user-level match access checks are needed
                // Currently, authenticated users can subscribe to any active match topic.
                log.debug("STOMP SUBSCRIBE approved for user: {} on topic {}", accessor.getUser().getName(), destination);
            }
        }

        return message;
    }
}
