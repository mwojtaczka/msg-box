package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class Conversation {

	private final UUID conversationId;
	private Set<UUID> interlocutors;
	private Instant lastActivity;

	public boolean doesMsgBelong(Message message) {
		return interlocutors.contains(message.getAuthorId());
	}

	public static Conversation createFaceToFace(UserConnection connection) {

		return Conversation.builder()
						   .conversationId(UUID.randomUUID())
						   .lastActivity(Instant.now())
						   .interlocutors(Set.of(connection.getUser1(), connection.getUser2()))
						   .build();
	}

	public static Conversation createGroup(Set<UUID> interlocutorsIndices) {
		return Conversation.builder()
						   .conversationId(UUID.randomUUID())
						   .lastActivity(Instant.now())
						   .interlocutors(interlocutorsIndices)
						   .build();

	}

	public Envelope<Message> accept(Message message) {
		if (!doesMsgBelong(message)) {
			throw new RuntimeException("Message cannot be applied to the conversation");
		}
		Message withTime = message.withTime(Instant.now())
								  .withStatusByInterlocutor(Map.of(message.getAuthorId(), MessageStatusUpdated.Status.SEEN));
		Set<UUID> recipients = getRecipients(message.getAuthorId());
		return Envelope.wrap(withTime, recipients);
	}

	private Set<UUID> getRecipients(UUID except) {
		return interlocutors.stream()
							.filter(interlocutor -> !except.equals(interlocutor))
							.collect(Collectors.toSet());
	}

	public Envelope<MessageStatusUpdated> accept(MessageStatusUpdated messageStatus) {
		if (!isValid(messageStatus)) {
			throw new RuntimeException("Message status cannot be applied to the conversation");
		}
		return Envelope.wrap(messageStatus, getRecipients(messageStatus.getUpdatedBy()));
	}

	public boolean isValid(MessageStatusUpdated messageStatus) {
		return interlocutors.contains(messageStatus.getUpdatedBy()) && interlocutors.contains(messageStatus.getAuthorId());
	}

	public Set<UUID> getInterlocutors() {
		return Set.copyOf(interlocutors);
	}
}
