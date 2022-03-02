package com.maciej.wojtaczka.messagebox.http;

import com.datastax.oss.driver.api.core.CqlSession;
import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.utils.ConversationFixture;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static com.maciej.wojtaczka.messagebox.http.GetConversationsRequestHandler.CONVERSATIONS_URL;
import static java.time.Instant.parse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext
class GetConversationsRequestHandlerTest {

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
	@Ignore
	void shouldReturnUserConversationOrderedByLastMessageTime() {
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
}
