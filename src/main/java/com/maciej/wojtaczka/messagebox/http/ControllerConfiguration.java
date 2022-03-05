package com.maciej.wojtaczka.messagebox.http;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.maciej.wojtaczka.messagebox.http.GetConversationsRequestHandler.CONVERSATIONS_URL;
import static com.maciej.wojtaczka.messagebox.http.GetMessagesRequestHandler.MESSAGES_URL;
import static com.maciej.wojtaczka.messagebox.http.GetUnreadConversationsCountRequestHandler.UNREAD_CONVERSATIONS_COUNT_URL;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class ControllerConfiguration {

	@Bean
	RouterFunction<ServerResponse> conversationRoutes(ConversationService conversationService) {

		var getConversationsRequestHandler = new GetConversationsRequestHandler(conversationService);
		var getMessagesRequestHandler = new GetMessagesRequestHandler(conversationService);
		var getUnreadConversationsCountRequestHandler = new GetUnreadConversationsCountRequestHandler(conversationService);

		return RouterFunctions
				.route(GET(CONVERSATIONS_URL), getConversationsRequestHandler)
				.andRoute(GET(MESSAGES_URL), getMessagesRequestHandler)
				.andRoute(GET(UNREAD_CONVERSATIONS_COUNT_URL), getUnreadConversationsCountRequestHandler);
	}
}
