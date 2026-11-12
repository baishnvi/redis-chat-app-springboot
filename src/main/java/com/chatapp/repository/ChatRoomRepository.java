package com.chatapp.repository;

import com.chatapp.dto.RoomSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Storage abstraction for chat rooms (metadata Hash + participants Set). */
public interface ChatRoomRepository {

    /** Atomically creates the room. @return false if a room with this id already exists. */
    boolean createIfAbsent(String roomId, String roomName, Instant createdAt);

    boolean exists(String roomId);

    /** @return true if the participant was newly added, false if already present. */
    boolean addParticipant(String roomId, String participant);

    boolean isParticipant(String roomId, String participant);

    Set<String> participants(String roomId);

    Optional<RoomSummary> findSummary(String roomId);

    List<RoomSummary> findAll();

    /** Removes every key of the room. @return false if the room did not exist. */
    boolean delete(String roomId);
}
