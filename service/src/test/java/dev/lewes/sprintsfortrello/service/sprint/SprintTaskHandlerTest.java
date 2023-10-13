package dev.lewes.sprintsfortrello.service.sprint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.RequestEntity.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.sprint.SprintControllerIntegrationTest.TrelloCardsSprintEndpointMock;
import dev.lewes.sprintsfortrello.service.sprint.SprintTaskHandlerTest.TrelloCardsSprintTaskHandlerEndpointMock;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask.Status;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, SprintController.class, TrelloCardsSprintTaskHandlerEndpointMock.class})
public class SprintTaskHandlerTest {

    @LocalServerPort
    private int serverPort;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TrelloProperties connectionProperties;

    public static List<TrelloCard> trelloCards = new ArrayList<>();

    @BeforeEach
    public void before() {
        trelloCards.clear();

        connectionProperties.setUrl("http://localhost:" + serverPort + "/");
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
        ResponseEntity<Sprint> sprintResponseEntity = createSprintWithName();
        String id = sprintResponseEntity.getBody().getId();

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, connectionProperties.getBacklogColumnId())));

        invokeSyncTasksWithTrelloEndpoint();

        ResponseEntity<SprintTask[]> tasks = restTemplate.exchange(RequestEntity.post(URI.create(buildApiUrl("sprints/" + id + "/tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .build(), SprintTask[].class);

        assertThat(Arrays.stream(tasks.getBody()).map(SprintTask::getStatus).toList(), containsInAnyOrder(
            trelloCards.stream().map(card -> Status.NOT_STARTED).toArray()
        ));
    }

    @Test
    public void cardsAreSetAsDoneWhenMovedToDone() {
        ResponseEntity<Sprint> sprintResponseEntity = createSprintWithName();
        String id = sprintResponseEntity.getBody().getId();

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, connectionProperties.getBacklogColumnId())));

        invokeSyncTasksWithTrelloEndpoint();

        restTemplate.exchange(RequestEntity.post(URI.create(buildApiUrl("sprints/" + id + "/tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .build(), SprintTask[].class);

        trelloCards.get(0).setIdList(connectionProperties.getDoneColumnId());

        invokeSyncTasksWithTrelloEndpoint();

        ResponseEntity<SprintTask[]> tasks = restTemplate.exchange(RequestEntity.get(URI.create(buildApiUrl("sprints/" + id + "/tasks"))).build(), SprintTask[].class);

        assertThat(Arrays.stream(tasks.getBody()).map(SprintTask::getStatus).toArray(), hasItemInArray(
            Status.DONE
        ));
    }

    private ResponseEntity<Sprint> createSprintWithName() {
        return restTemplate.exchange(post(URI.create(buildApiUrl("sprints")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);
    }

    private ResponseEntity<SprintTask[]> invokeSyncTasksWithTrelloEndpoint() {
        return restTemplate.exchange(RequestEntity.post(URI.create(buildApiUrl("tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .build(), SprintTask[].class);
    }


    public String buildApiUrl(String path) {
        return "http://localhost:" + serverPort + "/" + path;
    }


    @RestController
    public static class TrelloCardsSprintTaskHandlerEndpointMock {

        @Autowired
        private TrelloProperties connectionProperties;

        @GetMapping("/1/boards/{id}/cards")
        public ResponseEntity<JsonNode> boardsGet(@PathVariable String id) {
            if(!id.equalsIgnoreCase(connectionProperties.getBoardId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ArrayNode results = new ObjectMapper().convertValue(trelloCards, ArrayNode.class);

            return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(results);
        }

    }

}
