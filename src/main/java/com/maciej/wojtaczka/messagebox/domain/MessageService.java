package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Message;
import reactor.core.publisher.Mono;

public class MessageService {

	private final ConversationStorage conversationStorage;
	private final PostMan postMan;

	public MessageService(ConversationStorage conversationStorage, PostMan postMan) {
		this.conversationStorage = conversationStorage;
		this.postMan = postMan;
	}

	public Mono<Void> acceptMessage(Message message) {
		return conversationStorage.getConversation(message.getConversationId())
								  .filter(conversation -> conversation.doesMsgBelong(message))
								  .map(conversation -> conversation.accept(message))
								  .flatMap(envelope -> postMan.deliver(envelope)
															  .then(conversationStorage.storeNewMessage(envelope.getMessage()))
								  );
	}
}
