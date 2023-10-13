package dev.lewes.sprintsfortrello.service.sprint;

import static org.hamcrest.Matchers.is;

import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.sprint.Sprint.Status;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {SprintsForTrelloApplication.class})
@DataMongoTest
@ExtendWith(SpringExtension.class)
public class SprintRepositoryIntegrationTest {

    @Autowired
    private SprintRepository sprintRepository;

    @Test
    public void saveAndFetch_success() {
        Sprint sprint = new Sprint("Test Sprint 1", Status.PLANNING);

        sprintRepository.save(sprint);

        Optional<Sprint> actualSprint = sprintRepository.findById(sprint.getId());

        MatcherAssert.assertThat(actualSprint, Matchers.hasProperty("present", is(true)));
        MatcherAssert.assertThat(actualSprint.get(), Matchers.samePropertyValuesAs(sprint));
    }

    @Test
    public void getCurrentSprint_success() {
        Sprint sprint = new Sprint("Test Sprint 1", Status.IN_PROGRESS);
        sprint.setCurrent(true);

        sprintRepository.save(sprint);

        Optional<Sprint> actualSprint = sprintRepository.getCurrentSprint();

        MatcherAssert.assertThat(actualSprint, Matchers.hasProperty("present", is(true)));
        MatcherAssert.assertThat(actualSprint.get(), Matchers.samePropertyValuesAs(sprint));
    }

}