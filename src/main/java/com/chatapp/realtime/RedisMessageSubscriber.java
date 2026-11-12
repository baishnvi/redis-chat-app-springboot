package com.chatapp.realtime;

import com.chatapp.util.RedisKeys;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/** Redis Pub/Sub subscriber: receives every room's events and forwards them to open SSE connections. */
@Component
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final SseSubscriptionRegistry registry;

    public RedisMessageSubscriber(SseSubscriptionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String roomId = RedisKeys.roomIdFromChannel(channel);
        log.debug("Pub/Sub event on room {}", roomId);

        if (MessageBroadcaster.ROOM_DELETED_EVENT.equals(body)) {
            registry.closeRoom(roomId);
        } else {
            registry.broadcast(roomId, body);
        }
    }
}
