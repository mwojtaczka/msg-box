package com.maciej.wojtaczka.messagebox.utils;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageStatusUpdated;
import com.maciej.wojtaczka.messagebox.persistence.CassandraConversationStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.maciej.wojtaczka.messagebox.domain.model.MessageStatusUpdated.Status.SEEN;

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
					.forEach(msg -> cassandraConversationStorage.storeNewMessage(Envelope.wrap(msg, conversation.accept(msg).getRecipients()))
																.block());

			removeUnreadConversationForUsersWhoHaveSeenAllMessages(conversation);
		}

		private void removeUnreadConversationForUsersWhoHaveSeenAllMessages(Conversation conversation) {
			Map<UUID, List<Message>> messagesThatUserSaw = conversation.getInterlocutors()
																	   .stream()
																	   .collect(Collectors.toMap(Function.identity(), this::getMessagesSeenBy));
			int allMessagesInConversationCount = messages.size();

			List<UUID> usersWhoSawAllMessages = messagesThatUserSaw.entrySet().stream()
																   .filter(entry -> entry.getValue().size() == allMessagesInConversationCount)
																   .map(Map.Entry::getKey)
																   .collect(Collectors.toList());
			usersWhoSawAllMessages.forEach(userId -> cassandraConversationStorage.removeUnreadConversation(conversationId, userId).block());
		}

		private List<Message> getMessagesSeenBy(UUID userId) {
			return messages.stream().filter(message -> message.getStatusByInterlocutor().containsKey(userId)).collect(Collectors.toList());
		}

	}

	public static class MessageBuilder {
		private final Message.MessageBuilder messageBuilder = Message.builder();
		private final ConversationBuilder conversationBuilder;
		private Map<UUID, MessageStatusUpdated.Status> seenBy = new HashMap<>();

		public MessageBuilder(ConversationBuilder conversationBuilder) {
			this.conversationBuilder = conversationBuilder;
			UUID authorId = UUID.randomUUID();
			messageBuilder.conversationId(conversationBuilder.conversationId)
						  .time(Instant.now())
						  .content("Default message content")
						  .statusByInterlocutor(Map.of(authorId, SEEN))
						  .authorId(authorId);
		}

		public MessageBuilder withContent(String content) {
			messageBuilder.content(content);
			return this;
		}

		public MessageBuilder writtenBy(UUID authorId) {
			messageBuilder.authorId(authorId).statusByInterlocutor(Map.of(authorId, SEEN));
			messageBuilder.authorId(authorId);
			seenBy.put(authorId, SEEN);
			return this;
		}

		public MessageBuilder atTime(Instant time) {
			messageBuilder.time(time);
			return this;
		}

		public MessageBuilder seenBy(UUID... interlocutors) {
			Map<UUID, MessageStatusUpdated.Status> collect = Arrays.stream(interlocutors)
																   .collect(Collectors.toMap(Function.identity(), x -> SEEN));
			seenBy.putAll(collect);
			return this;
		}

		public MessageBuilder andMessage() {
			build();
			return new MessageBuilder(conversationBuilder);
		}

		public ConversationBuilder andTheConversation() {
			build();
			return conversationBuilder;
		}

		private void build() {
			seenBy.put(messageBuilder.build().getAuthorId(), SEEN);
			messageBuilder.statusByInterlocutor(seenBy);
			conversationBuilder.messages.add(messageBuilder.build());
		}
	}


}
