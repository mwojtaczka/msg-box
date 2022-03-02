package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Value
public class Envelope {
	Message message;
	List<UUID> receivers;

	public static Envelope wrap(Message message, Collection<UUID> interlocutors) {
		return new Envelope(message, List.copyOf(interlocutors));
	}

	public List<UUID> getReceivers() {
		return List.copyOf(receivers);
	}
}
