package dev.lewes.sprintsfortrello.service.sprint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SprintConfiguration {

    @Bean
    public SprintManager getSprintManager(SprintRepository sprintRepository) {
        return new SprintManager(sprintRepository);
    }

}
