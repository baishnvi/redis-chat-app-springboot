package com.chatapp.repository;

import com.chatapp.util.RedisKeys;
import java.util.Collections;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisMessageRepository implements MessageRepository {

    private final StringRedisTemplate redis;

    public RedisMessageRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void append(String roomId, String messageJson, int maxMessages) {
        String key = RedisKeys.messages(roomId);
        redis.opsForList().rightPush(key, messageJson);          // O(1) append, keeps chronological order
        if (maxMessages > 0) {
            redis.opsForList().trim(key, -maxMessages, -1);       // keep only the newest N
        }
    }

    @Override
    public List<String> lastN(String roomId, int n) {
        // LRANGE key -n -1 returns the last n elements in order, O(n) regardless of list size.
        List<String> result = redis.opsForList().range(RedisKeys.messages(roomId), -n, -1);
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public long count(String roomId) {
        Long size = redis.opsForList().size(RedisKeys.messages(roomId));
        return size == null ? 0L : size;
    }
}
