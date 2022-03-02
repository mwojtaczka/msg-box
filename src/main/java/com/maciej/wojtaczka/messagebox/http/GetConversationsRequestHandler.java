package com.maciej.wojtaczka.messagebox.http;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

public class GetConversationsRequestHandler implements HandlerFunction<ServerResponse> {

	static final String CONVERSATIONS_URL = "/v1/conversations";

	private final ConversationService conversationService;

	public GetConversationsRequestHandler(ConversationService conversationService) {
		this.conversationService = conversationService;
	}


	@Override
	public Mono<ServerResponse> handle(ServerRequest request) {

		Optional<UUID> userId = request.queryParam("userId")
									   .flatMap(this::toUUID);

		if (userId.isEmpty()) {
			return ServerResponse.badRequest()
								 .build();
		}

		Flux<Conversation> userConversations = conversationService.getUserConversations(userId.get());

		return ServerResponse.ok()
							 .body(userConversations, Conversation.class);
	}

	private Optional<UUID> toUUID(String s) {
		try {
			return Optional.of(UUID.fromString(s));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
