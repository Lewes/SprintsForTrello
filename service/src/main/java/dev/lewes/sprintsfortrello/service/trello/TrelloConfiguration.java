package dev.lewes.sprintsfortrello.service.trello;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrelloConfiguration {

    @Bean
    public TrelloService getTrelloService(TrelloConnectionProperties trelloConnectionProperties) {
        return new TrelloService(trelloConnectionProperties);
    }

    @Bean
    public TrelloConnectionProperties getTrelloConfigurationProperties() {
        return new TrelloConnectionProperties();
    }

}
