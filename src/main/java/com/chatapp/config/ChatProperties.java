package com.chatapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tunable chat settings (prefix "chat" in application.properties). */
@ConfigurationProperties(prefix = "chat")
public record ChatProperties(
        @DefaultValue("1000") int maxMessagesPerRoom,
        @DefaultValue("10") int defaultHistoryLimit,
        @DefaultValue("100") int maxHistoryLimit,
        @DefaultValue("true") boolean enforceMembership,
        @DefaultValue("1800000") long sseTimeoutMs) {
}
