package com.maciej.wojtaczka.messagebox.http;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.Conversation;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

public class GetMessagesRequestHandler implements HandlerFunction<ServerResponse> {

	static final String MESSAGES_URL = "/v1/conversations/{conversation_id}/messages";

	private final ConversationService conversationService;

	public GetMessagesRequestHandler(ConversationService conversationService) {
		this.conversationService = conversationService;
	}


	@Override
	public Mono<ServerResponse> handle(ServerRequest request) {

		String conversationId = request.pathVariable("conversation_id");

		Optional<UUID> convUuid = toUUID(conversationId);
		if (convUuid.isEmpty()) {
			return ServerResponse.badRequest()
								 .build();
		}

		Flux<Message> userConversations = conversationService.getConversationMessages(convUuid.get());

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
