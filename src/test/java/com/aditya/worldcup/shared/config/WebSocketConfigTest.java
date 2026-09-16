package com.aditya.worldcup.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = WebSocketConfig.class)
class WebSocketConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Test
    void webSocketConfigBeanExists() {
        assertThat(context.containsBean("webSocketConfig")).isTrue();
    }

    @Test
    void testMessageBrokerRegistry() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        when(registry.enableSimpleBroker("/topic")).thenReturn(null);
        when(registry.setApplicationDestinationPrefixes("/app")).thenReturn(registry);

        webSocketConfig.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }
}
