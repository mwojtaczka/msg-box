package com.maciej.wojtaczka.messagebox.messaging;

import com.datastax.oss.driver.api.core.CqlSession;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
import com.maciej.wojtaczka.messagebox.persistence.CassandraConversationStorage;
import org.assertj.core.api.Assertions;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@SpringBootTest
@WebAppConfiguration
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
class ConnectionListenerTest {

	@Autowired
	private KafkaTemplate<String, UserConnection> kafkaTemplate;

	@Autowired
	private CassandraConversationStorage storage;

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
	void shouldCreateNewFaceToFaceConversation() throws ExecutionException, InterruptedException {
		//given
		UUID user1 = UUID.randomUUID();
		UUID user2 = UUID.randomUUID();
		UserConnection givenConnection = UserConnection.builder()
													   .user1(user1)
													   .user2(user2)
													   .connectionDate(Instant.parse("2007-12-03T10:15:30.00Z"))
													   .build();

		//when
		kafkaTemplate.send(MessagingConfiguration.CONNECTION_CREATED_TOPIC, givenConnection).get();

		//then
		Thread.sleep(1000);
		StepVerifier.create(Flux.merge(storage.getUserConversations(user1, 1), storage.getUserConversations(user2, 1)))
					.assertNext(conversation -> Assertions.assertThat(conversation.getInterlocutors()).containsExactlyInAnyOrder(user1, user2))
					.assertNext(conversation -> Assertions.assertThat(conversation.getInterlocutors()).containsExactlyInAnyOrder(user1, user2))
					.verifyComplete();
	}

}
