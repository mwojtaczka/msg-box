package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.PostMan;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaPostMan implements PostMan {

	public static final String MESSAGE_ACCEPTED_TOPIC = "message-accepted";
	private final ReactiveKafkaProducerTemplate<String, Envelope> kafkaProducerTemplate;

	public KafkaPostMan(ReactiveKafkaProducerTemplate<String, Envelope> kafkaProducerTemplate) {
		this.kafkaProducerTemplate = kafkaProducerTemplate;
	}

	@Override
	public Mono<Void> deliver(Envelope message) {
		return kafkaProducerTemplate.send(MESSAGE_ACCEPTED_TOPIC, message)
				.flatMap(result -> {
					if (result.exception() == null) {
						return Mono.empty();
					} else {
						return Mono.error(result::exception);
					}
				});
	}
}
