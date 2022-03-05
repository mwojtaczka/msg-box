package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Value
public class Envelope<T> {
	T payload;
	List<UUID> receivers;

	public static <T> Envelope<T> wrap(T payload, Collection<UUID> interlocutors) {
		return new Envelope<>(payload, List.copyOf(interlocutors));
	}

	public List<UUID> getReceivers() {
		return List.copyOf(receivers);
	}
}
