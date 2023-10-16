package dev.lewes.sprintsfortrello.service.trello;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrelloConfiguration {

    @Bean
    public TrelloProperties getTrelloConfigurationProperties() {
        return new TrelloProperties();
    }

}
