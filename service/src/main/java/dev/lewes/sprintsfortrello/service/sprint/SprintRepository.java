package dev.lewes.sprintsfortrello.service.sprint;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SprintRepository extends MongoRepository<Sprint, String> {

    @Query("{current: true}")
    Optional<Sprint> getCurrentSprint();

    Optional<Sprint> findByName(String name);

}
