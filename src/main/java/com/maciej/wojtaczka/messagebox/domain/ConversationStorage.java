package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import reactor.core.publisher.Mono;

public interface ConversationStorage {

    Mono<Void> storeNewMessage(Message message);

    Mono<Conversation> getConversation(String conversationId);
}
