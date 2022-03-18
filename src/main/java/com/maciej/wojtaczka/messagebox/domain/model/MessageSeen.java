package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
//todo: MessageStatusUpdated
public class MessageSeen {

	UUID conversationId;
	UUID authorId;
	Instant time;

	UUID seenBy;
//updated by
	//status: DELIVERED, SEEN
}
