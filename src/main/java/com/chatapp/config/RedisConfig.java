package com.chatapp.config;

import com.chatapp.realtime.RedisMessageSubscriber;
import com.chatapp.util.RedisKeys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@EnableConfigurationProperties(ChatProperties.class)
public class RedisConfig {

    /**
     * One listener container subscribed to the pattern "chat:room:*:channel", so every
     * room's Pub/Sub channel is covered without registering listeners per room.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, RedisMessageSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(RedisKeys.CHANNEL_PATTERN));
        return container;
    }
}
