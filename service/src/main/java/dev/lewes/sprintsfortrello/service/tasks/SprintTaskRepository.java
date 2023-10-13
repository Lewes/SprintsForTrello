package dev.lewes.sprintsfortrello.service.tasks;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SprintTaskRepository extends MongoRepository<SprintTask, String> {

    Optional<SprintTask> findByTrelloCardId(String name);

}