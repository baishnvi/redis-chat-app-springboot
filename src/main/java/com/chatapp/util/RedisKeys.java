package com.chatapp.util;

/** Central place for every Redis key / channel name used by the app. */
public final class RedisKeys {

    private static final String ROOM_PREFIX = "chat:room:";

    /** Set of all room ids (also gives an atomic "create if absent" via SADD). */
    public static final String ROOM_INDEX = "chat:rooms";
    /** Pub/Sub pattern that matches every room channel. */
    public static final String CHANNEL_PATTERN = ROOM_PREFIX + "*:channel";

    private RedisKeys() {
    }

    /** Hash: room metadata (name, createdAt). */
    public static String meta(String roomId) {
        return ROOM_PREFIX + roomId + ":meta";
    }

    /** Set: participants of the room. */
    public static String participants(String roomId) {
        return ROOM_PREFIX + roomId + ":participants";
    }

    /** List: chat messages in chronological order (RPUSH = newest at the tail). */
    public static String messages(String roomId) {
        return ROOM_PREFIX + roomId + ":messages";
    }

    /** Pub/Sub channel of the room. */
    public static String channel(String roomId) {
        return ROOM_PREFIX + roomId + ":channel";
    }

    public static String roomIdFromChannel(String channel) {
        String withoutPrefix = channel.substring(ROOM_PREFIX.length());
        return withoutPrefix.substring(0, withoutPrefix.length() - ":channel".length());
    }
}
