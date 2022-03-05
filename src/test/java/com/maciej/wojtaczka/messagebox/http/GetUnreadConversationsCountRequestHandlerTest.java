package com.maciej.wojtaczka.messagebox.http;

import com.datastax.oss.driver.api.core.CqlSession;
import com.maciej.wojtaczka.messagebox.utils.ConversationFixture;
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

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static com.maciej.wojtaczka.messagebox.http.GetUnreadConversationsCountRequestHandler.UNREAD_CONVERSATIONS_COUNT_URL;
import static java.time.Instant.parse;

@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext
class GetUnreadConversationsCountRequestHandlerTest {

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
	void shouldReturnCountOfUnreadConversations() {
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
}
