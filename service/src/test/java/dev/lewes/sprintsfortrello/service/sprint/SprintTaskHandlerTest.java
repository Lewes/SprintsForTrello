package dev.lewes.sprintsfortrello.service.sprint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItemInArray;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.sprint.SprintTaskHandlerTest.TrelloCardsSprintTaskHandlerEndpointMock;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask.Status;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import dev.lewes.sprintsfortrello.service.utils.RestExchangeTestUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, TrelloCardsSprintTaskHandlerEndpointMock.class})
public class SprintTaskHandlerTest {

    @LocalServerPort
    private int serverPort;

    private RestExchangeTestUtils restUtils;

    @Autowired
    private TrelloProperties trelloProperties;

    public static List<TrelloCard> trelloCards = new ArrayList<>();

    @BeforeEach
    public void before() {
        trelloCards.clear();

        trelloProperties.setUrl("http://localhost:" + serverPort + "/");

        restUtils = new RestExchangeTestUtils(trelloProperties);
    }

    @Test
    public void tasksOutsideOfColumnsAreUnknownStatus() {
        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, "random_unknown_column")));

        ResponseEntity<SprintTask[]> tasks = invokeSyncTasksWithTrelloEndpoint();

        assertThat(Arrays.stream(tasks.getBody()).map(SprintTask::getStatus).toList(), containsInAnyOrder(
            trelloCards.stream().map(card -> Status.UNKNOWN).toArray()
        ));
    }

    @Test
    public void tasksInBacklogAreStatusNotStarted() {
        ResponseEntity<Sprint> sprintResponseEntity = createSprintWithName("Test Sprint 1");
        String id = sprintResponseEntity.getBody().getId();

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId())));

        invokeSyncTasksWithTrelloEndpoint();

        ResponseEntity<SprintTask[]> tasks = restUtils.postAtUrl("sprints/" + id + "/tasks", null, SprintTask[].class);

        assertThat(Arrays.stream(tasks.getBody()).map(SprintTask::getStatus).toList(), containsInAnyOrder(
            trelloCards.stream().map(card -> Status.NOT_STARTED).toArray()
        ));
    }

    @Test
    public void cardsAreSetAsDoneWhenMovedToDone() {
        ResponseEntity<Sprint> sprintResponseEntity = createSprintWithName("Test Sprint 1");
        String id = sprintResponseEntity.getBody().getId();

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId())));

        invokeSyncTasksWithTrelloEndpoint();

        restUtils.postAtUrl("sprints/" + id + "/tasks", null, SprintTask[].class);

        trelloCards.get(0).setIdList(trelloProperties.getDoneColumnId());

        invokeSyncTasksWithTrelloEndpoint();

        ResponseEntity<SprintTask[]> tasks =restUtils.getAtUrl("sprints/" + id + "/tasks", SprintTask[].class);

        assertThat(Arrays.stream(tasks.getBody()).map(SprintTask::getStatus).toArray(), hasItemInArray(
            Status.DONE
        ));
    }

    private ResponseEntity<Sprint> createSprintWithName(String name) {
        return restUtils.postAtUrl("sprints", Map.of("name", name), Sprint.class);
    }

    private ResponseEntity<SprintTask[]> invokeSyncTasksWithTrelloEndpoint() {
        return restUtils.postAtUrl("tasks", null, SprintTask[].class);
    }

    @RestController
    public static class TrelloCardsSprintTaskHandlerEndpointMock {

        @Autowired
        private TrelloProperties trelloProperties;

        @GetMapping("/1/boards/{id}/cards")
        public ResponseEntity<JsonNode> boardsGet(@PathVariable String id) {
            if(!id.equalsIgnoreCase(trelloProperties.getBoardId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ArrayNode results = new ObjectMapper().convertValue(trelloCards, ArrayNode.class);

            return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(results);
        }

    }

}
