package com.maciej.wojtaczka.messagebox.config;

import com.datastax.oss.driver.api.core.CqlSession;
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
import reactor.core.publisher.Mono;

@Configuration
public class TestConfig {

	@Bean
	KafkaTemplate<String, String> kafkaTestMessageTemplate(KafkaProperties properties) {
		DefaultKafkaProducerFactory<String, String> producerFactory =
				new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), new StringSerializer());

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
