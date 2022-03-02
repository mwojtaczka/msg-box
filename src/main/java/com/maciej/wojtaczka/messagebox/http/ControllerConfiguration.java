package com.maciej.wojtaczka.messagebox.http;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.maciej.wojtaczka.messagebox.http.GetConversationsRequestHandler.CONVERSATIONS_URL;

@Configuration
public class ControllerConfiguration {

	@Bean
	RouterFunction<ServerResponse> conversationRoutes(ConversationService conversationService) {

		var getConversationsRequestHandler = new GetConversationsRequestHandler(conversationService);

		return RouterFunctions
				.route(RequestPredicates.GET(CONVERSATIONS_URL), getConversationsRequestHandler);
	}
}
