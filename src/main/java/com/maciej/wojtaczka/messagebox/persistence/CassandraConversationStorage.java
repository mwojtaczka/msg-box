package com.maciej.wojtaczka.messagebox.persistence;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.maciej.wojtaczka.messagebox.domain.ConversationStorage;
import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageSeen;
import org.springframework.data.cassandra.ReactiveResultSet;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Repository
public class CassandraConversationStorage implements ConversationStorage {

	private final ReactiveCassandraOperations cassandraOperations;

	public CassandraConversationStorage(ReactiveCassandraOperations cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	@Override
	public Mono<Void> storeNewMessage(Envelope<Message> envelope) {
		BatchStatementBuilder statementsBuilder = BatchStatement.builder(BatchType.LOGGED);

		Message message = envelope.getPayload();
		SimpleStatement messageInsert = QueryBuilder.insertInto("message_box", "message")
													.value("author_id", literal(message.getAuthorId()))
													.value("time", literal(message.getTime()))
													.value("content", literal(message.getContent()))
													.value("conversation_id", literal(message.getConversationId()))
													.value("seen_by", literal(message.getSeenBy()))
													.build();
		statementsBuilder.addStatement(messageInsert);

		SimpleStatement updateConversationLastActivity =
				QueryBuilder.update("message_box", "conversation")
							.setColumn("last_activity", literal(message.getTime()))
							.whereColumn("conversation_id").isEqualTo(literal(message.getConversationId()))
							.build();
		statementsBuilder.addStatements(updateConversationLastActivity);

		ArrayList<UUID> usersIdsWeNeedToUpdateConversations = new ArrayList<>(envelope.getRecipients());
		usersIdsWeNeedToUpdateConversations.add(message.getAuthorId());

		for (UUID userId : usersIdsWeNeedToUpdateConversations) {

			SimpleStatement insertUpdatedWithLastActivityConversationByUser =
					QueryBuilder.insertInto("message_box", "conversation_by_user")
								.value("conversation_id", literal(message.getConversationId()))
								.value("last_activity", literal(message.getTime()))
								.value("user_id", literal(userId))
								.build();

			SimpleStatement insertUnreadConversation = QueryBuilder.insertInto("message_box", "conversation_unread")
																   .value("user_id", literal(userId))
																   .value("conversation_id", literal(message.getConversationId()))
																   .build();

			if (userId.equals(message.getAuthorId())) {
				statementsBuilder.addStatements(insertUpdatedWithLastActivityConversationByUser);
			} else {
				statementsBuilder.addStatements(insertUpdatedWithLastActivityConversationByUser, insertUnreadConversation);
			}
		}

		return cassandraOperations.execute(statementsBuilder.build())
								  .flatMap(result -> {
									  if (result.wasApplied()) {
										  return Mono.empty();
									  } else {
										  List<Map.Entry<Node, Throwable>> errors = result.getExecutionInfo().getErrors();
										  if (errors.size() > 1) {
											  Throwable throwable = errors.get(0).getValue();
											  return Mono.error(throwable);
										  }
										  return Mono.error(() -> new RuntimeException("Unknown cql error"));
									  }
								  });
	}

	public Flux<Message> fetchConversationMessages(UUID conversationId) {
		SimpleStatement selectMessages = QueryBuilder.selectFrom("message_box", "message")
													 .all()
													 .whereColumn("conversation_id").isEqualTo(literal(conversationId))
													 .build();

		return cassandraOperations.execute(selectMessages)
								  .flatMapMany(ReactiveResultSet::rows)
								  .map(row -> Message.builder()
													 .conversationId(row.getUuid("conversation_id"))
													 .authorId(row.getUuid("author_id"))
													 .time(row.getInstant("time"))
													 .content(row.getString("content"))
													 .seenBy(row.getSet("seen_by", UUID.class))
													 .build());
	}

	@Override
	public Mono<Conversation> getConversation(UUID conversationId) {
		SimpleStatement selectConversation = QueryBuilder.selectFrom("message_box", "conversation")
														 .all()
														 .whereColumn("conversation_id").isEqualTo(literal(conversationId))
														 .limit(1)
														 .build();

		return cassandraOperations.execute(selectConversation)
								  .flatMapMany(ReactiveResultSet::rows)
								  .next()
								  .map(row -> {
									  UUID id = row.getUuid("conversation_id");
									  Set<UUID> interlocutors = row.getSet("interlocutors", UUID.class);
									  Instant lastActivityTime = row.getInstant("last_activity");
									  return Conversation.builder().conversationId(id)
														 .interlocutors(interlocutors)
														 .lastActivity(lastActivityTime)
														 .build();
								  });
	}

	@Override
	public Flux<Message> getMessages(UUID conversationId) {
		SimpleStatement selectMessages = QueryBuilder.selectFrom("message_box", "message")
													 .all()
													 .whereColumn("conversation_id").isEqualTo(literal(conversationId))
													 .build();

		return cassandraOperations.execute(selectMessages)
								  .flatMapMany(ReactiveResultSet::rows)
								  .map(row -> Message.builder()
													 .conversationId(row.getUuid("conversation_id"))
													 .authorId(row.getUuid("author_id"))
													 .content(row.getString("content"))
													 .time(row.getInstant("time"))
													 .build());
	}

	@Override
	public Mono<Void> updateMessageSeen(MessageSeen messageSeen) {
		SimpleStatement updateSeenBy = QueryBuilder.update("message_box", "message")
												   .appendSetElement("seen_by", literal(messageSeen.getSeenBy()))
												   .whereColumn("conversation_id").isEqualTo(literal(messageSeen.getConversationId()))
												   .whereColumn("time").isEqualTo(literal(messageSeen.getTime()))
												   .whereColumn("author_id").isEqualTo(literal(messageSeen.getAuthorId()))
												   .build();
		SimpleStatement removeUnreadConversation = QueryBuilder.deleteFrom("message_box", "conversation_unread")
															   .whereColumn("user_id").isEqualTo(literal(messageSeen.getSeenBy()))
															   .whereColumn("conversation_id").isEqualTo(literal(messageSeen.getConversationId()))
															   .build();
		BatchStatement statements = BatchStatement.builder(BatchType.LOGGED)
												  .addStatements(updateSeenBy, removeUnreadConversation)
												  .build();
		return cassandraOperations.execute(statements)
								  .then();
	}

	public Mono<Void> removeUnreadConversation(UUID conversationId, UUID userId) {
		SimpleStatement removeUnreadConversation = QueryBuilder.deleteFrom("message_box", "conversation_unread")
															   .whereColumn("user_id").isEqualTo(literal(userId))
															   .whereColumn("conversation_id").isEqualTo(literal(conversationId))
															   .build();
		return cassandraOperations.execute(removeUnreadConversation)
								  .then();
	}

	@Override
	public Flux<UUID> getUnreadConversationsIndices(UUID userId) {
		SimpleStatement getUnreadConversations = QueryBuilder.selectFrom("message_box", "conversation_unread")
															 .all()
															 .whereColumn("user_id").isEqualTo(literal(userId))
															 .build();

		return cassandraOperations.execute(getUnreadConversations)
								  .flatMapMany(ReactiveResultSet::rows)
								  .mapNotNull(row -> row.getUuid("conversation_id"));
	}

	@Override
	public Flux<Conversation> getUserConversations(UUID userId) {
		SimpleStatement selectConversations = QueryBuilder.selectFrom("message_box", "conversation_by_user")
														  .all()
														  .whereColumn("user_id").isEqualTo(literal(userId))
														  .build();

		return cassandraOperations.execute(selectConversations)
								  .flatMapMany(ReactiveResultSet::rows)
								  .mapNotNull(row -> row.getUuid("conversation_id"))
								  .flatMap(this::getConversation)
								  //TODO think off better solution - should be sorted in DB already in case pagination introduction
								  .sort(Comparator.comparing(Conversation::getLastActivity).reversed());
	}

	@Override
	public Mono<Void> insertConversation(Conversation conversation) {
		BatchStatementBuilder batchStatementBuilder = BatchStatement.builder(BatchType.LOGGED);

		SimpleStatement insertConversation = QueryBuilder.insertInto("message_box", "conversation")
														 .value("conversation_id", literal(conversation.getConversationId()))
														 .value("interlocutors", literal(conversation.getInterlocutors()))
														 .value("last_activity", literal(conversation.getLastActivity()))
														 .build();
		batchStatementBuilder.addStatements(insertConversation);

		for (UUID userId : conversation.getInterlocutors()) {
			SimpleStatement insertConversationByUser = QueryBuilder.insertInto("message_box", "conversation_by_user")
																   .value("conversation_id", literal(conversation.getConversationId()))
																   .value("last_activity", literal(conversation.getLastActivity())) //todo
																   .value("user_id", literal(userId))
																   .build();
			batchStatementBuilder.addStatements(insertConversationByUser);
		}

		BatchStatement batchStatement = batchStatementBuilder.build();

		return cassandraOperations.execute(batchStatement)
								  .flatMap(result -> Mono.empty());
	}
}
