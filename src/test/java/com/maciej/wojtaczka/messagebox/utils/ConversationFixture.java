package com.maciej.wojtaczka.messagebox.utils;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.persistence.CassandraConversationStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ConversationFixture {

	@Autowired
	public CassandraConversationStorage cassandraConversationStorage;

	public ConversationBuilder givenConversationWithId(UUID conversationId) {
		return new ConversationBuilder(conversationId);
	}

	public ConversationBuilder givenConversation() {
		return new ConversationBuilder(UUID.randomUUID());
	}

	public class ConversationBuilder {
		private final Conversation.ConversationBuilder conversationBuilder = Conversation.builder();
		private final UUID conversationId;
		private final Set<Message> messages = new HashSet<>();

		public ConversationBuilder(UUID conversationId) {
			this.conversationId = conversationId;
			conversationBuilder.conversationId(conversationId)
							   .lastActivity(Instant.now());
		}

		public ConversationBuilder betweenUsers(UUID... users) {
			Set<UUID> interlocutors = Stream.of(users)
											.collect(Collectors.toSet());
			conversationBuilder.interlocutors(interlocutors);

			return this;
		}

		public MessageBuilder withMessage() {
			return new MessageBuilder(this);
		}

		public void exists() {

			Conversation conversation = conversationBuilder.build();
			cassandraConversationStorage.insertConversation(conversation).block();
			messages.stream()
					.sorted(Comparator.comparing(Message::getTime))
					.peek(conversation::accept)
					.forEach(msg -> cassandraConversationStorage.storeNewMessage(Envelope.wrap(msg, conversation.accept(msg).getReceivers()))
																.block());
		}

	}

	public static class MessageBuilder {
		private final Message.MessageBuilder messageBuilder = Message.builder();
		private final ConversationBuilder conversationBuilder;

		public MessageBuilder(ConversationBuilder conversationBuilder) {
			this.conversationBuilder = conversationBuilder;
			UUID authorId = UUID.randomUUID();
			messageBuilder.conversationId(conversationBuilder.conversationId)
						  .time(Instant.now())
						  .content("Default message content")
						  .seenBy(Set.of(authorId))
						  .authorId(authorId);
		}

		public MessageBuilder withContent(String content) {
			messageBuilder.content(content);
			return this;
		}

		public MessageBuilder writtenBy(UUID authorId) {
			messageBuilder.authorId(authorId).seenBy(Set.of(authorId));
			return this;
		}

		public MessageBuilder atTime(Instant time) {
			messageBuilder.time(time);
			return this;
		}

		public MessageBuilder andMessage() {
			conversationBuilder.messages.add(messageBuilder.build());
			return new MessageBuilder(conversationBuilder);
		}

		public ConversationBuilder andTheConversation() {
			conversationBuilder.messages.add(messageBuilder.build());
			return conversationBuilder;
		}
	}


}
