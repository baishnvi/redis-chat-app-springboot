package com.chatapp.util;

import java.util.Locale;

/** Room ids are the trimmed, lower-cased room name, so "General" and "general" are the same room. */
public final class RoomIdNormalizer {

    private RoomIdNormalizer() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
