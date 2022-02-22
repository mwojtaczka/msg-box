package com.maciej.wojtaczka.messagebox.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationTest {

	@Test
	void shouldMessageBelongToConversation() {
		UUID msgAuthorId = UUID.randomUUID();
		UUID receiver1 = UUID.randomUUID();
		UUID receiver2 = UUID.randomUUID();

		Conversation givenConversation =
				Conversation.builder()
							.conversationId("conversation id")
							.interlocutors(Set.of(msgAuthorId, receiver1, receiver2))
							.build();

		Message givenMessage = Message.builder()
									  .authorId(msgAuthorId)
									  .content("Hello")
									  .conversationId("conversation id")
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

		Conversation givenConversation =
				Conversation.builder()
							.conversationId("conversation id")
							.interlocutors(Set.of(receiver1, receiver2))
							.build();

		Message givenMessage = Message.builder()
									  .authorId(msgAuthorId)
									  .content("Hello")
									  .conversationId("conversation id")
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

		Conversation givenConversation =
				Conversation.builder()
							.conversationId("conversation id")
							.interlocutors(Set.of(msgAuthorId, receiver1, receiver2))
							.build();

		Message givenMessage = Message.builder()
									  .authorId(msgAuthorId)
									  .content("Hello")
									  .conversationId("conversation id")
									  .build();

		//when
		Envelope toBeSent = givenConversation.accept(givenMessage);

		//then
		assertThat(toBeSent.getReceivers()).containsExactlyInAnyOrder(receiver1, receiver2);
		assertThat(toBeSent.getMessage().getAuthorId()).isEqualTo(msgAuthorId);
		assertThat(toBeSent.getMessage().getConversationId()).isEqualTo("conversation id");
		assertThat(toBeSent.getMessage().getTime()).isNotNull();
		assertThat(toBeSent.getMessage().getContent()).isEqualTo("Hello");
	}

}
