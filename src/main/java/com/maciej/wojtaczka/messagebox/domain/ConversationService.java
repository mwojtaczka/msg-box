package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageSeen;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.UUID;

public class ConversationService {

	private final ConversationStorage conversationStorage;
	private final PostMan postMan;

	public ConversationService(ConversationStorage conversationStorage, PostMan postMan) {
		this.conversationStorage = conversationStorage;
		this.postMan = postMan;
	}

	public Mono<Void> createFaceToFaceConversation(UserConnection connection) {
		Conversation newConversation = Conversation.createFaceToFace(connection);

		return conversationStorage.insertConversation(newConversation);
	}

	public Mono<Void> acceptMessage(Message message) {
		return conversationStorage.getConversation(message.getConversationId())
								  .filter(conversation -> conversation.doesMsgBelong(message))
								  .map(conversation -> conversation.accept(message))
								  .flatMap(envelope -> postMan.deliver(envelope)
															  .then(conversationStorage.storeNewMessage(envelope))
								  );
	}

	public Mono<Void> updateMessageStatus(MessageSeen messageSeen) {

		return conversationStorage.getConversation(messageSeen.getConversationId())
								  .filter(conversation -> conversation.isValid(messageSeen))
								  .map(conversation -> conversation.accept(messageSeen))
								  .flatMap(postMan::notifyAboutMsgStatusUpdated)
								  .then(conversationStorage.updateMessageSeen(messageSeen));
	}

	public Flux<Conversation> getUserConversations(UUID userId) { //TODO introduce pagination
		return conversationStorage.getUserConversations(userId)
								  .sort(Comparator.comparing(Conversation::getLastActivity).reversed());
	}

	public Flux<Message> getConversationMessages(UUID conversationId) { //TODO introduce pagination
		return conversationStorage.getMessages(conversationId);
	}
}
