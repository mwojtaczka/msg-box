package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ConversationStorage {

    Mono<Void> storeNewMessage(Envelope envelope);

	Flux<Conversation> getUserConversations(UUID userId);

	Mono<Void> insertConversation(Conversation conversation);

    Mono<Conversation> getConversation(UUID conversationId);
}
