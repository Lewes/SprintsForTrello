package dev.lewes.sprintsfortrello.service.sprint;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.sprint.TasksControllerIntegrationTest.TrelloCardsTasksEndpointMock;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloConnectionProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, SprintController.class, TrelloCardsTasksEndpointMock.class})
public class TasksControllerIntegrationTest {

    private TestRestTemplate restTemplate = new TestRestTemplate();

    public static List<TrelloCard> trelloCards = new ArrayList<>();

    @Autowired
    private TrelloConnectionProperties connectionProperties;

    @LocalServerPort
    private int serverPort;

    @BeforeEach
    public void before() {
        trelloCards.clear();

        connectionProperties.setUrl("http://localhost:" + serverPort + "/");
    }

    @Test
    public void addTasksWithLatestTrelloCards() {
        String backlog_id = "BACKLOG";

        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Test Card 1", backlog_id));
        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Test Card 2", backlog_id));

        ResponseEntity<SprintTask[]> responseEntity = postTasksEndpoint();

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));

        List<SprintTask> actualTasks = Arrays.asList(responseEntity.getBody());

        assertThat(trelloCards, containsInAnyOrder(
            actualTasks.stream()
                .map(SprintTask::getTrelloCard)
                .toArray()));
    }

    @Test
    public void getTasksReturnsAll() {
        String backlog_id = "BACKLOG";

        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Test Card 1", backlog_id));
        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Test Card 2", backlog_id));

        postTasksEndpoint();

        ResponseEntity<SprintTask[]> responseEntity = restTemplate.exchange(RequestEntity.get(URI.create(buildApiUrl("tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .build(), SprintTask[].class);

        List<SprintTask> actualTasks = Arrays.asList(responseEntity.getBody());

        assertThat(trelloCards, containsInAnyOrder(
            actualTasks.stream()
                .map(SprintTask::getTrelloCard)
                .toArray()));
    }

    @Test
    public void updateExistingTasksDoesNotAddDuplicates() {
        String backlog_id = "BACKLOG";

        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Test Card 1", backlog_id));
        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Test Card 2", backlog_id));
        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Test Card 2", backlog_id));

        postTasksEndpoint();

        trelloCards.get(0).setName("Test Card 1 Renamed");
        trelloCards.get(1).setIdList("another_column_id");

        ResponseEntity<SprintTask[]> responseEntity = postTasksEndpoint();

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));

        List<SprintTask> actualTasks = Arrays.asList(responseEntity.getBody());

        assertThat(trelloCards, containsInAnyOrder(
            actualTasks.stream()
                .map(SprintTask::getTrelloCard)
                .toArray()));
    }

    private ResponseEntity<SprintTask[]> postTasksEndpoint() {
        return restTemplate.exchange(RequestEntity.post(URI.create(buildApiUrl("tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .build(), SprintTask[].class);
    }

    @RestController
    public static class TrelloCardsTasksEndpointMock {

        @Autowired
        private TrelloConnectionProperties connectionProperties;

        @GetMapping("/1/cards/{id}")
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

    public String buildApiUrl(String path) {
        return "http://localhost:" + serverPort + "/" + path;
    }

}
