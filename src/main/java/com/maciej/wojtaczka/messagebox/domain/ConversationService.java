package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageStatusUpdated;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ConversationService {

	private final ConversationStorage conversationStorage;
	private final PostMan postMan;

	public ConversationService(ConversationStorage conversationStorage, PostMan postMan) {
		this.conversationStorage = conversationStorage;
		this.postMan = postMan;
	}

	public Mono<Void> createFaceToFaceConversation(UserConnection connection) {
		log.info("Creating face to face conversations for connection: {}", connection);
		Conversation newConversation = Conversation.createFaceToFace(connection);

		return conversationStorage.insertConversation(newConversation)
				.doFinally(signalType -> {
					switch (signalType) {
						case ON_COMPLETE:
							log.info("Conversation created for connection: {}", connection);
							break;
						case ON_ERROR:
							log.error("Conversation NOT created for connection: {}", connection);
					}
				});
	}

	public Mono<Conversation> createGroupConversation(List<UUID> interlocutorsIndices) {
		Conversation newConversation = Conversation.createGroup(new HashSet<>(interlocutorsIndices));

		return conversationStorage.insertConversation(newConversation)
								  .then(Mono.just(newConversation));
	}

	public Mono<Void> acceptMessage(Message message) {
		return conversationStorage.getConversation(message.getConversationId())
								  .filter(conversation -> conversation.doesMsgBelong(message))
								  .map(conversation -> conversation.accept(message))
								  .flatMap(envelope -> postMan.deliver(envelope)
															  .then(conversationStorage.storeNewMessage(envelope))
								  );
	}

	public Mono<Void> updateMessageStatus(MessageStatusUpdated newStatus) {

		return conversationStorage.getConversation(newStatus.getConversationId())
								  .filter(conversation -> conversation.isValid(newStatus))
								  .map(conversation -> conversation.accept(newStatus))
								  .flatMap(postMan::notifyAboutMsgStatusUpdated)
								  .then(conversationStorage.updateMessageSeen(newStatus));
	}

	public Flux<Conversation> getUserConversations(UUID userId) { //TODO introduce pagination
		return conversationStorage.getUserConversations(userId)
								  .sort(Comparator.comparing(Conversation::getLastActivity).reversed());
	}

	public Flux<Message> getConversationMessages(UUID conversationId) { //TODO introduce pagination
		return conversationStorage.getMessages(conversationId);
	}

	public Mono<Long> getUnreadConversationsCount(UUID userId) {
		return conversationStorage.getUnreadConversationsIndices(userId)
								  .count();
	}
}
