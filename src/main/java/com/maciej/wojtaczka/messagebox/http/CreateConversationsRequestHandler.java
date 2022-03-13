package com.maciej.wojtaczka.messagebox.http;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class CreateConversationsRequestHandler implements HandlerFunction<ServerResponse> {

	static final String CONVERSATIONS_URL = "/v1/conversations";

	private final ConversationService conversationService;

	public CreateConversationsRequestHandler(ConversationService conversationService) {
		this.conversationService = conversationService;
	}


	@Override
	public Mono<ServerResponse> handle(ServerRequest request) {

		Mono<Conversation> conversation = request.bodyToFlux(UUID.class)
											 .collectList()
											 .flatMap(conversationService::createGroupConversation);

		return ServerResponse.ok()
							 .body(conversation, Conversation.class);
	}
}
