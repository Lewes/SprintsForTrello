package dev.lewes.sprintsfortrello.service;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableMongoRepositories
public class SprintsForTrelloApplication {

	@Bean
	public RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		restTemplate.setRequestFactory(requestFactory);

		return restTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(SprintsForTrelloApplication.class, args);
	}

}
