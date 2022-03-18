package com.maciej.wojtaczka.messagebox.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.maciej.wojtaczka.messagebox.domain.model.MessageStatusUpdated.Status.SEEN;
import static org.assertj.core.api.Assertions.assertThat;

class ConversationTest {

	@Test
	void shouldCreateNewFaceToFaceConversation() {
		//given
		UUID user1 = UUID.randomUUID();
		UUID user2 = UUID.randomUUID();
		UserConnection givenConnection = UserConnection.builder()
													   .user1(user1)
													   .user2(user2)
													   .connectionDate(Instant.parse("2007-12-03T10:15:30.00Z"))
													   .build();
		//when
		Conversation newConversation = Conversation.createFaceToFace(givenConnection);

		//then
		assertThat(newConversation.getConversationId()).isNotNull();
		assertThat(newConversation.getLastActivity()).isNotNull();
		assertThat(newConversation.getInterlocutors()).containsExactlyInAnyOrder(user1, user2);
	}

	@Test
	void shouldCreateNewGroupConversation() {
		//given
		UUID user1 = UUID.randomUUID();
		UUID user2 = UUID.randomUUID();
		UUID user3 = UUID.randomUUID();
		Set<UUID> groupMembers = Set.of(user1, user2, user3);

		//when
		Conversation newConversation = Conversation.createGroup(groupMembers);

		//then
		assertThat(newConversation.getConversationId()).isNotNull();
		assertThat(newConversation.getLastActivity()).isNotNull();
		assertThat(newConversation.getInterlocutors()).containsExactlyInAnyOrder(user1, user2, user3);
	}

	@Test
	void shouldMessageBelongToConversation() {
		UUID msgAuthorId = UUID.randomUUID();
		UUID recipient1 = UUID.randomUUID();
		UUID recipient2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, recipient1, recipient2))
							.build();

		Message givenMessage = Message.builder()
									  .authorId(msgAuthorId)
									  .content("Hello")
									  .conversationId(conversationId)
									  .build();

		//when
		boolean doesMsgBelong = givenConversation.doesMsgBelong(givenMessage);

		//then
		assertThat(doesMsgBelong).isTrue();
	}

	@Test
	void shouldMessageDoesNotBelongToConversation_whenAuthorIsNotWithinInterlocutors() {
		UUID msgAuthorId = UUID.randomUUID();
		UUID recipient1 = UUID.randomUUID();
		UUID recipient2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(recipient1, recipient2))
							.build();

		Message givenMessage = Message.builder()
									  .authorId(msgAuthorId)
									  .content("Hello")
									  .conversationId(conversationId)
									  .build();

		//when
		boolean doesMsgBelong = givenConversation.doesMsgBelong(givenMessage);

		//then
		assertThat(doesMsgBelong).isFalse();
	}

	@Test
	void shouldAcceptMessageAndCreateEnvelope() {
		//given
		UUID msgAuthorId = UUID.randomUUID();
		UUID recipient1 = UUID.randomUUID();
		UUID recipient2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, recipient1, recipient2))
							.build();

		Message givenMessage = Message.builder()
									  .authorId(msgAuthorId)
									  .content("Hello")
									  .conversationId(conversationId)
									  .build();

		//when
		Envelope<Message> toBeSent = givenConversation.accept(givenMessage);

		//then
		assertThat(toBeSent.getRecipients()).containsExactlyInAnyOrder(recipient1, recipient2);
		assertThat(toBeSent.getPayload().getAuthorId()).isEqualTo(msgAuthorId);
		assertThat(toBeSent.getPayload().getConversationId()).isEqualTo(conversationId);
		assertThat(toBeSent.getPayload().getTime()).isNotNull();
		assertThat(toBeSent.getPayload().getContent()).isEqualTo("Hello");
		assertThat(toBeSent.getPayload().getStatusByInterlocutor()).containsExactly(Map.entry(msgAuthorId, SEEN));
	}

	@Test
	void shouldMessageSeenBeValid() {
		//given
		UUID msgAuthorId = UUID.randomUUID();
		UUID recipient1 = UUID.randomUUID();
		UUID recipient2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, recipient1, recipient2))
							.build();
		var messageSeen = MessageStatusUpdated.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .updatedBy(recipient1)
									 .build();
		//when
		boolean result = givenConversation.isValid(messageSeen);

		//then
		assertThat(result).isTrue();
	}

	@Test
	void shouldMessageSeenBeInvalid_whenMsgAuthorDoesntBelongToConversation() {
		//given
		UUID msgAuthorId = UUID.randomUUID();
		UUID seenBy = UUID.randomUUID();
		UUID anotherRecipient = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(seenBy, anotherRecipient))
							.build();
		var messageSeen = MessageStatusUpdated.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .updatedBy(seenBy)
									 .build();
		//when
		boolean result = givenConversation.isValid(messageSeen);

		//then
		assertThat(result).isFalse();
	}

	@Test
	void shouldMessageSeenBeInvalid_whenUserThatSawMsgDoesntBelongToConversation() {
		//given
		UUID msgAuthorId = UUID.randomUUID();
		UUID seenBy = UUID.randomUUID();
		UUID anotherRecipient = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, anotherRecipient))
							.build();
		var messageSeen = MessageStatusUpdated.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .updatedBy(seenBy)
									 .build();
		//when
		boolean result = givenConversation.isValid(messageSeen);

		//then
		assertThat(result).isFalse();
	}

	@Test
	void shouldCreateEnvelopeWithNotification() {
		//given
		UUID msgAuthorId = UUID.randomUUID();
		UUID seenBy = UUID.randomUUID();
		UUID anotherRecipient = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, seenBy, anotherRecipient))
							.build();
		var messageSeen = MessageStatusUpdated.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .updatedBy(seenBy)
									 .build();
		//when
		Envelope<MessageStatusUpdated> toBeSent = givenConversation.accept(messageSeen);

		//then
		assertThat(toBeSent.getRecipients()).containsExactlyInAnyOrder(msgAuthorId, anotherRecipient);
		assertThat(toBeSent.getPayload()).isEqualTo(messageSeen);
	}

}
