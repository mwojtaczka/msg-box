package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
@With
public class Message {

    UUID authorId;
	Instant time;
	String content;
	UUID conversationId;
	Map<UUID, MessageStatusUpdated.Status> statusByInterlocutor;
}
