package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageSeen;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ConversationStorage {

    Mono<Void> storeNewMessage(Envelope<Message> envelope);

	Flux<Conversation> getUserConversations(UUID userId);

	Mono<Void> insertConversation(Conversation conversation);

    Mono<Conversation> getConversation(UUID conversationId);

	Flux<Message> getMessages(UUID conversationId);

	Mono<Void> updateMessageSeen(MessageSeen messageSeen);

	Flux<UUID> getUnreadConversationsIndices(UUID userId);
}
