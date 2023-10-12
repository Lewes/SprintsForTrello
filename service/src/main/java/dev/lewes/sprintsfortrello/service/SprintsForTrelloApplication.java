package dev.lewes.sprintsfortrello.service;

import dev.lewes.sprintsfortrello.service.events.EventsManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableMongoRepositories
public class SprintsForTrelloApplication {

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	public EventsManager getEventsManager() {
		return new EventsManager();
	}

	public static void main(String[] args) {
		SpringApplication.run(SprintsForTrelloApplication.class, args);
	}

}
