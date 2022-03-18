package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageStatusUpdated;
import reactor.core.publisher.Mono;

public interface PostMan {

    Mono<Void> deliver(Envelope<Message> message);

	Mono<Void> notifyAboutMsgStatusUpdated(Envelope<MessageStatusUpdated> messageSeen);
}
