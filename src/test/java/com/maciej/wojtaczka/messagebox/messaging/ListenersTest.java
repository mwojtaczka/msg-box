package com.maciej.wojtaczka.messagebox.messaging;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
import com.maciej.wojtaczka.messagebox.utils.ConversationFixture;
import com.maciej.wojtaczka.messagebox.utils.KafkaTestListener;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@WebAppConfiguration
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
class ListenersTest {

	@Autowired
	private KafkaTemplate<String, Message> kafkaMessageTemplate;

	@Autowired
	private KafkaTemplate<String, UserConnection> kafkaConnectionTemplate;

	@Autowired
	private KafkaTestListener kafkaTestListener;

	@Autowired
	private ObjectMapper objectMapper;

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
	void shouldSaveAndForwardInboundMessage() throws JsonProcessingException, ExecutionException, InterruptedException {

		kafkaTestListener.listenToTopic(KafkaPostMan.MESSAGE_ACCEPTED_TOPIC, 1);

		UUID conversationId = UUID.randomUUID();
		UUID msgAuthorId = UUID.randomUUID();
		UUID msgReceiver = UUID.randomUUID();

		$.givenConversationWithId(conversationId).betweenUsers(msgAuthorId, msgReceiver).exists();

		Message inboundMsg = Message.builder()
									.authorId(msgAuthorId)
									.content("Hello!")
									.conversationId(conversationId)
									.build();

		//when
		kafkaMessageTemplate.send(MessagingConfiguration.MESSAGE_RECEIVED_TOPIC, inboundMsg).get();

		//then verify message forwarded
		String msgJson = kafkaTestListener.receiveContentFromTopic(KafkaPostMan.MESSAGE_ACCEPTED_TOPIC).orElseThrow();
		Envelope sent = objectMapper.readValue(msgJson, Envelope.class);

		assertThat(sent.getReceivers()).containsExactly(msgReceiver);
		Message message = sent.getMessage();
		assertThat(message.getConversationId()).isEqualTo(conversationId);
		assertThat(message.getAuthorId()).isEqualTo(msgAuthorId);
		assertThat(message.getContent()).isEqualTo("Hello!");
		assertThat(message.getTime()).isNotNull();

		//verify message storage
		Thread.sleep(100);

		StepVerifier.create($.cassandraConversationStorage.fetchConversationMessages(conversationId))
					.assertNext(msg -> assertAll(() -> assertThat(msg.getAuthorId()).isEqualTo(msgAuthorId),
												 () -> assertThat(msg.getConversationId()).isEqualTo(conversationId),
												 () -> assertThat(msg.getContent()).isEqualTo("Hello!"),
												 () -> assertThat(msg.getTime()).isNotNull()
					))
					.verifyComplete();
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
		kafkaConnectionTemplate.send(MessagingConfiguration.CONNECTION_CREATED_TOPIC, givenConnection).get();

		//then
		Thread.sleep(1000);
		StepVerifier.create(Flux.merge($.cassandraConversationStorage.getUserConversations(user1),
									   $.cassandraConversationStorage.getUserConversations(user2)))
					.assertNext(conversation -> Assertions.assertThat(conversation.getInterlocutors()).containsExactlyInAnyOrder(user1, user2))
					.assertNext(conversation -> Assertions.assertThat(conversation.getInterlocutors()).containsExactlyInAnyOrder(user1, user2))
					.verifyComplete();
	}

}
