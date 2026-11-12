package com.chatapp.realtime;

import com.chatapp.util.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis Pub/Sub publisher: PUBLISH chat:room:{id}:channel <json>. */
@Component
public class RedisMessageBroadcaster implements MessageBroadcaster {

    private final StringRedisTemplate redis;

    public RedisMessageBroadcaster(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void publishMessage(String roomId, String messageJson) {
        redis.convertAndSend(RedisKeys.channel(roomId), messageJson);
    }

    @Override
    public void publishRoomDeleted(String roomId) {
        redis.convertAndSend(RedisKeys.channel(roomId), ROOM_DELETED_EVENT);
    }
}
