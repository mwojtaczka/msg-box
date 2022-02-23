package com.maciej.wojtaczka.messagebox.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Builder
@Value
public class UserConnection {

	UUID user1;
	UUID user2;
	Instant connectionDate;
}
