package com.chatapp.realtime;

/** Publishes room events so every subscribed participant (on any app instance) receives them. */
public interface MessageBroadcaster {

    String ROOM_DELETED_EVENT = "__ROOM_DELETED__";

    void publishMessage(String roomId, String messageJson);

    void publishRoomDeleted(String roomId);
}
