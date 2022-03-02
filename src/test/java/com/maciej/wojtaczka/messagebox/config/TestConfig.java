package com.maciej.wojtaczka.messagebox.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
import org.apache.kafka.common.serialization.StringSerializer;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.ReactiveSession;
import org.springframework.data.cassandra.ReactiveSessionFactory;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.cql.session.DefaultBridgedReactiveSession;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.core.publisher.Mono;

@Configuration
public class TestConfig {

	@Bean
	KafkaTemplate<String, Message> kafkaMessageTemplate(KafkaProperties properties) {
		DefaultKafkaProducerFactory<String, Message> producerFactory =
				new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), new JsonSerializer<>());

		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	KafkaTemplate<String, UserConnection> kafkaConnectionTemplate(KafkaProperties properties) {
		DefaultKafkaProducerFactory<String, UserConnection> producerFactory =
				new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), new JsonSerializer<>());

		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	ReactiveCassandraOperations cassandraOperations(CassandraConverter cassandraConverter) {
		return new ReactiveCassandraTemplate(new TestSessionFactory(), cassandraConverter);
	}

	static class TestSessionFactory implements ReactiveSessionFactory {

		CqlSession session;

		public TestSessionFactory() {
			this.session = EmbeddedCassandraServerHelper.getSession();
		}

		@Override
		public Mono<ReactiveSession> getSession() {

			if (session.isClosed()) {
				session = EmbeddedCassandraServerHelper.getSession();
			}
			return Mono.just(new DefaultBridgedReactiveSession(session));
		}
	}

}
