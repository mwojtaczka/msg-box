package com.maciej.wojtaczka.messagebox.http;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class GetUnreadConversationsCountRequestHandler implements HandlerFunction<ServerResponse> {

	static final String UNREAD_CONVERSATIONS_COUNT_URL = "/v1/conversations/unread-count";

	private final ConversationService conversationService;

	public GetUnreadConversationsCountRequestHandler(ConversationService conversationService) {
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

		Mono<String> unreadConversationsCount = conversationService.getUnreadConversationsCount(userId.get())
																   .map(Objects::toString);

		return ServerResponse.ok()
							 .contentType(MediaType.TEXT_PLAIN)
							 .body(unreadConversationsCount, String.class);
	}

	private Optional<UUID> toUUID(String s) {
		try {
			return Optional.of(UUID.fromString(s));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
