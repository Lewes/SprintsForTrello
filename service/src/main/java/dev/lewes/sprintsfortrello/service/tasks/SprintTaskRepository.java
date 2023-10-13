package dev.lewes.sprintsfortrello.service.tasks;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SprintTaskRepository extends MongoRepository<SprintTask, String> {

    Optional<SprintTask> findByTrelloCardId(String name);

}