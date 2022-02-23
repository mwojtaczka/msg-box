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
		Envelope toBeSent = givenConversation.accept(givenMessage);

		//then
		assertThat(toBeSent.getReceivers()).containsExactlyInAnyOrder(receiver1, receiver2);
		assertThat(toBeSent.getMessage().getAuthorId()).isEqualTo(msgAuthorId);
		assertThat(toBeSent.getMessage().getConversationId()).isEqualTo(conversationId);
		assertThat(toBeSent.getMessage().getTime()).isNotNull();
		assertThat(toBeSent.getMessage().getContent()).isEqualTo("Hello");
	}

}
