package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ConversationStorage {

    Mono<Void> storeNewMessage(Message message);

	Flux<Conversation> getUserConversations(UUID userId, int count);

	Mono<Void> insertConversation(Conversation conversation);

    Mono<Conversation> getConversation(UUID conversationId);
}
