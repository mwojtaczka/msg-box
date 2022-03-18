package com.maciej.wojtaczka.messagebox.http;

import com.datastax.oss.driver.api.core.CqlSession;
import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.utils.ConversationFixture;
import org.assertj.core.api.Assertions;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.maciej.wojtaczka.messagebox.http.GetConversationsRequestHandler.CONVERSATIONS_URL;
import static com.maciej.wojtaczka.messagebox.http.GetMessagesRequestHandler.MESSAGES_URL;
import static com.maciej.wojtaczka.messagebox.http.GetUnreadConversationsCountRequestHandler.UNREAD_CONVERSATIONS_COUNT_URL;
import static java.time.Instant.parse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext
class ConversationsRequestHandlerTest {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private ConversationFixture $;

	@BeforeAll
	static void startCassandra() throws IOException, InterruptedException {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		CqlSession session = EmbeddedCassandraServerHelper.getSession();
		new CQLDataLoader(session).load(new ClassPathCQLDataSet("schema.cql"));
	}

	@AfterAll
	static void cleanCassandra() {
		EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
	}

	@Test
	void getConversations_shouldReturnUserConversationOrderedByLastMessageTime() {
		//given
		UUID userId = UUID.randomUUID();
		UUID firstConversation = UUID.randomUUID();
		UUID secondConversation = UUID.randomUUID();
		UUID thirdConversation = UUID.randomUUID();
		UUID forthConversation = UUID.randomUUID();
		Instant time = parse("2007-12-03T10:15:30.00Z");

		UUID userId2 = UUID.randomUUID();
		$.givenConversationWithId(secondConversation).betweenUsers(userId, userId2)
		 .withMessage().writtenBy(userId2).atTime(time.plusSeconds(2))
		 .andTheConversation().exists();

		$.givenConversationWithId(firstConversation).betweenUsers(userId, UUID.randomUUID())
		 .withMessage().writtenBy(userId).atTime(time.plusSeconds(1))
		 .andTheConversation().exists();

		$.givenConversationWithId(thirdConversation).betweenUsers(userId, UUID.randomUUID())
		 .withMessage().writtenBy(userId).atTime(time.plusSeconds(3))
		 .andTheConversation().exists();

		$.givenConversationWithId(forthConversation).betweenUsers(userId, UUID.randomUUID())
		 .withMessage().writtenBy(userId).atTime(time.plusSeconds(4))
		 .andTheConversation().exists();

		//when
		WebTestClient.ResponseSpec result = webClient.get()
													 .uri(uriBuilder -> uriBuilder.path(CONVERSATIONS_URL)
																				  .queryParam("userId", userId)
																				  .build())
													 .exchange();
		//then
		result.expectStatus().isOk()
			  .expectBodyList(Conversation.class)
			  .hasSize(4)
			  .value(conversations -> {
				  assertThat(conversations.get(0).getConversationId()).isEqualTo(forthConversation);
				  assertThat(conversations.get(1).getConversationId()).isEqualTo(thirdConversation);
				  assertThat(conversations.get(2).getConversationId()).isEqualTo(secondConversation);
				  assertThat(conversations.get(3).getConversationId()).isEqualTo(firstConversation);
			  });
	}

	@Test //TODO
	void getMessages_shouldReturnMessage() {

	}

	@Test
	void getMessages_shouldReturnUserConversationMessagesOrderedByMessageTimeDesc() {
		//given
		UUID userId1 = UUID.randomUUID();
		UUID userId2 = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();
		Instant time = parse("2007-12-03T10:15:30.00Z");

		$.givenConversationWithId(conversationId).betweenUsers(userId1, userId2)
		 .withMessage().writtenBy(userId1).withContent("Hello").atTime(time.plusSeconds(1))
		 .andMessage().writtenBy(userId2).withContent("Hi!").atTime(time.plusSeconds(2))
		 .andMessage().writtenBy(userId1).withContent("What's up?").atTime(time.plusSeconds(3))
		 .andMessage().writtenBy(userId2).withContent("Fine :)").atTime(time.plusSeconds(4))
		 .andMessage().writtenBy(userId1).withContent("Good, bye ;)").atTime(time.plusSeconds(5))
		 .andMessage().writtenBy(userId2).withContent("Bye xD").atTime(time.plusSeconds(6))
		 .andTheConversation().exists();

		//when
		WebTestClient.ResponseSpec result = webClient.get()
													 .uri(uriBuilder -> uriBuilder.path(MESSAGES_URL)
																				  .build(conversationId.toString()))
													 .exchange();
		//then
		result.expectStatus().isOk()
			  .expectBodyList(Message.class)
			  .hasSize(6)
			  .value(messages -> {
				  assertThatMessageHasExpected(messages.get(0), conversationId, userId2, time.plusSeconds(6), "Bye xD");
				  assertThatMessageHasExpected(messages.get(1), conversationId, userId1, time.plusSeconds(5), "Good, bye ;)");
				  assertThatMessageHasExpected(messages.get(2), conversationId, userId2, time.plusSeconds(4), "Fine :)");
				  assertThatMessageHasExpected(messages.get(3), conversationId, userId1, time.plusSeconds(3), "What's up?");
				  assertThatMessageHasExpected(messages.get(4), conversationId, userId2, time.plusSeconds(2), "Hi!");
				  assertThatMessageHasExpected(messages.get(5), conversationId, userId1, time.plusSeconds(1), "Hello");
			  });
	}

	private void assertThatMessageHasExpected(Message message, UUID conversationId, UUID author, Instant time, String content) {
		assertThat(message.getConversationId()).isEqualTo(conversationId);
		assertThat(message.getAuthorId()).isEqualTo(author);
		assertThat(message.getContent()).isEqualTo(content);
		assertThat(message.getTime()).isEqualTo(time);
	}

	@Test
	void getUnreadConversations_shouldReturnCountOfUnreadConversations() {
		//given
		UUID userId = UUID.randomUUID();
		UUID firstConversation = UUID.randomUUID();
		UUID secondConversation = UUID.randomUUID();
		UUID thirdConversation = UUID.randomUUID();
		UUID forthConversation = UUID.randomUUID();
		Instant time = parse("2007-12-03T10:15:30.00Z");

		UUID userId2 = UUID.randomUUID();
		$.givenConversationWithId(secondConversation).betweenUsers(userId, userId2)
		 .withMessage().writtenBy(userId2).atTime(time.plusSeconds(2))
		 .andTheConversation().exists();

		UUID userId3 = UUID.randomUUID();
		$.givenConversationWithId(firstConversation).betweenUsers(userId, userId3)
		 .withMessage().writtenBy(userId3).atTime(time.plusSeconds(1))
		 .andTheConversation().exists();

		UUID userId4 = UUID.randomUUID();
		$.givenConversationWithId(thirdConversation).betweenUsers(userId, userId4)
		 .withMessage().writtenBy(userId4).atTime(time.plusSeconds(3)).seenBy(userId)
		 .andTheConversation().exists();

		$.givenConversationWithId(forthConversation).betweenUsers(userId, UUID.randomUUID())
		 .withMessage().writtenBy(userId).atTime(time.plusSeconds(4))
		 .andTheConversation().exists();

		//when
		WebTestClient.ResponseSpec result = webClient.get()
													 .uri(uriBuilder -> uriBuilder.path(UNREAD_CONVERSATIONS_COUNT_URL)
																				  .queryParam("userId", userId)
																				  .build())
													 .exchange();
		//then
		result.expectStatus().isOk()
			  .expectBody(String.class).isEqualTo("2");
	}

	@Test
	void postConversations_shouldCreateGroupConversation() {
		//given
		UUID groupMember1 = UUID.randomUUID();
		UUID groupMember2 = UUID.randomUUID();
		UUID groupMember3 = UUID.randomUUID();
		UUID groupMember4 = UUID.randomUUID();

		//when
		WebTestClient.ResponseSpec result = webClient.post()
													 .uri(uriBuilder -> uriBuilder.path(CONVERSATIONS_URL)
																				  .build())
													 .bodyValue(List.of(groupMember1, groupMember2, groupMember3, groupMember4))
													 .exchange();
		//then
		Conversation created = result.expectStatus().isOk()
									 .returnResult(Conversation.class)
									 .getResponseBody()
									 .blockFirst();

		assertThat(created).isNotNull();
		assertThat(created.getConversationId()).isNotNull();
		assertThat(created.getInterlocutors()).containsExactlyInAnyOrder(groupMember1, groupMember2, groupMember3, groupMember4);

		StepVerifier.create($.cassandraConversationStorage.getConversation(created.getConversationId()))
					.assertNext(conversation -> Assertions.assertThat(conversation.getInterlocutors())
														  .containsExactlyInAnyOrder(groupMember1, groupMember2, groupMember3, groupMember4))
					.verifyComplete();
	}
}
