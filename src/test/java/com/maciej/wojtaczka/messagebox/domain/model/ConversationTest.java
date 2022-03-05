package com.maciej.wojtaczka.messagebox.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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
		assertThat(newConversation.getInterlocutors()).containsExactlyInAnyOrder(user1, user2);
	}

	@Test
	void shouldMessageBelongToConversation() {
		UUID msgAuthorId = UUID.randomUUID();
		UUID receiver1 = UUID.randomUUID();
		UUID receiver2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, receiver1, receiver2))
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
		UUID receiver1 = UUID.randomUUID();
		UUID receiver2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(receiver1, receiver2))
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
		UUID receiver1 = UUID.randomUUID();
		UUID receiver2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, receiver1, receiver2))
							.build();

		Message givenMessage = Message.builder()
									  .authorId(msgAuthorId)
									  .content("Hello")
									  .conversationId(conversationId)
									  .build();

		//when
		Envelope<Message> toBeSent = givenConversation.accept(givenMessage);

		//then
		assertThat(toBeSent.getReceivers()).containsExactlyInAnyOrder(receiver1, receiver2);
		assertThat(toBeSent.getPayload().getAuthorId()).isEqualTo(msgAuthorId);
		assertThat(toBeSent.getPayload().getConversationId()).isEqualTo(conversationId);
		assertThat(toBeSent.getPayload().getTime()).isNotNull();
		assertThat(toBeSent.getPayload().getContent()).isEqualTo("Hello");
		assertThat(toBeSent.getPayload().getSeenBy()).containsExactly(msgAuthorId);
	}

	@Test
	void shouldMessageSeenBeValid() {
		//given
		UUID msgAuthorId = UUID.randomUUID();
		UUID receiver1 = UUID.randomUUID();
		UUID receiver2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, receiver1, receiver2))
							.build();
		var messageSeen = MessageSeen.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .seenBy(receiver1)
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
		UUID receiver2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(seenBy, receiver2))
							.build();
		var messageSeen = MessageSeen.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .seenBy(seenBy)
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
		UUID receiver2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, receiver2))
							.build();
		var messageSeen = MessageSeen.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .seenBy(seenBy)
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
		UUID receiver2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId(conversationId)
							.interlocutors(Set.of(msgAuthorId, seenBy, receiver2))
							.build();
		var messageSeen = MessageSeen.builder()
									 .conversationId(conversationId)
									 .authorId(msgAuthorId)
									 .seenBy(seenBy)
									 .build();
		//when
		Envelope<MessageSeen> toBeSent = givenConversation.accept(messageSeen);

		//then
		assertThat(toBeSent.getReceivers()).containsExactlyInAnyOrder(msgAuthorId, receiver2);
		assertThat(toBeSent.getPayload()).isEqualTo(messageSeen);
	}

}
