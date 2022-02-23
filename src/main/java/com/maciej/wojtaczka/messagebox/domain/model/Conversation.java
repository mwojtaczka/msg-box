package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class Conversation {

	private final UUID conversationId;
	private Set<UUID> interlocutors;

	public boolean doesMsgBelong(Message message) {
		return interlocutors.contains(message.getAuthorId());
	}

	public static Conversation createFaceToFace(UserConnection connection) {

		return Conversation.builder()
						   .conversationId(UUID.randomUUID())
						   .interlocutors(Set.of(connection.getUser1(), connection.getUser2()))
						   .build();
	}

	public Envelope accept(Message message) {
		if (!doesMsgBelong(message)) {
			throw new RuntimeException("Message cannot be applied to conversation");
		}
		Message withTime = message.withTime(Instant.now());
		Set<UUID> receivers = interlocutors.stream()
										   .filter(interlocutor -> !message.getAuthorId().equals(interlocutor))
										   .collect(Collectors.toSet());
		return Envelope.wrap(withTime, receivers);
	}
}
