package com.maciej.wojtaczka.messagebox.http;

import com.datastax.oss.driver.api.core.CqlSession;
import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
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
import java.util.List;
import java.util.UUID;

import static com.maciej.wojtaczka.messagebox.http.GetConversationsRequestHandler.CONVERSATIONS_URL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext
class CreateConversationsRequestHandlerTest {

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
	void shouldCreateGroupConversation() {
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
