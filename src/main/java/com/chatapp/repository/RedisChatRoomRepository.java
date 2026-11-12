package com.chatapp.repository;

import com.chatapp.dto.RoomSummary;
import com.chatapp.util.RedisKeys;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisChatRoomRepository implements ChatRoomRepository {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_CREATED_AT = "createdAt";

    private final StringRedisTemplate redis;

    public RedisChatRoomRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean createIfAbsent(String roomId, String roomName, Instant createdAt) {
        // SADD is atomic: only one concurrent caller gets 1 for the same room id.
        Long added = redis.opsForSet().add(RedisKeys.ROOM_INDEX, roomId);
        if (added == null || added == 0L) {
            return false;
        }
        redis.opsForHash().putAll(RedisKeys.meta(roomId), Map.of(
                FIELD_NAME, roomName,
                FIELD_CREATED_AT, createdAt.toString()));
        return true;
    }

    @Override
    public boolean exists(String roomId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(RedisKeys.ROOM_INDEX, roomId));
    }

    @Override
    public boolean addParticipant(String roomId, String participant) {
        Long added = redis.opsForSet().add(RedisKeys.participants(roomId), participant);
        return added != null && added > 0L;
    }

    @Override
    public boolean isParticipant(String roomId, String participant) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(RedisKeys.participants(roomId), participant));
    }

    @Override
    public Set<String> participants(String roomId) {
        Set<String> members = redis.opsForSet().members(RedisKeys.participants(roomId));
        return members == null ? Collections.emptySet() : new TreeSet<>(members);
    }

    @Override
    public Optional<RoomSummary> findSummary(String roomId) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        Map<String, String> meta = hash.entries(RedisKeys.meta(roomId));
        if (meta.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RoomSummary(roomId, meta.get(FIELD_NAME), meta.get(FIELD_CREATED_AT)));
    }

    @Override
    public List<RoomSummary> findAll() {
        Set<String> ids = redis.opsForSet().members(RedisKeys.ROOM_INDEX);
        if (ids == null) {
            return Collections.emptyList();
        }
        List<RoomSummary> rooms = new ArrayList<>();
        for (String id : new TreeSet<>(ids)) {
            findSummary(id).ifPresent(rooms::add);
        }
        return rooms;
    }

    @Override
    public boolean delete(String roomId) {
        // Remove from the index first so no new activity can start on a half-deleted room.
        Long removed = redis.opsForSet().remove(RedisKeys.ROOM_INDEX, roomId);
        redis.delete(List.of(
                RedisKeys.meta(roomId),
                RedisKeys.participants(roomId),
                RedisKeys.messages(roomId)));
        return removed != null && removed > 0L;
    }
}
