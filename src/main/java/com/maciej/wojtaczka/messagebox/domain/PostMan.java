package com.maciej.wojtaczka.messagebox.domain;

import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import reactor.core.publisher.Mono;

public interface PostMan {

    Mono<Void> deliver(Envelope message);
}
