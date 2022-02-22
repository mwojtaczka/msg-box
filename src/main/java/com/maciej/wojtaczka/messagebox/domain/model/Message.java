package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@With
public class Message {

    UUID authorId;
	Instant time;
	String content;
	String conversationId;
}
