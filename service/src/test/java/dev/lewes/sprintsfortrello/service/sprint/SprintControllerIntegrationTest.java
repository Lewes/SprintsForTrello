package dev.lewes.sprintsfortrello.service.sprint;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.RequestEntity.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.sprint.SprintControllerIntegrationTest.TrelloCardsSprintEndpointMock;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
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
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, SprintController.class, TrelloCardsSprintEndpointMock.class})
public class SprintControllerIntegrationTest {

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
    public void createSprint() {
        ResponseEntity<Sprint> responseEntity = createSprintWithName();

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));
        assertThat(sprintRepository.findByName("Test Sprint 1"), hasProperty("present", is(true)));
    }

    @Test
    public void getNonExistentSprintReturns404() {
        ResponseEntity<Sprint> currentSprintResponse = restTemplate.exchange(get(URI.create(buildApiUrl("sprints/" + UUID.randomUUID())))
            .accept(MediaType.APPLICATION_JSON).build(), Sprint.class);

        assertThat(currentSprintResponse.getStatusCode().is4xxClientError(), is(true));
    }

    @Test
    public void createSprintAsCurrent() {
        ResponseEntity<Sprint> responseEntity = restTemplate.exchange(post(URI.create(buildApiUrl("sprints")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1",
                "current", true)), Sprint.class);

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));

        ResponseEntity<Sprint> currentSprintResponse = restTemplate.exchange(get(URI.create(buildApiUrl("sprints/current")))
            .accept(MediaType.APPLICATION_JSON).build(), Sprint.class);

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));
        assertThat(responseEntity.getBody().getId(), is(currentSprintResponse.getBody().getId()));
    }

    @Test
    public void noCurrentSprintReturns404UponFetching() {
        List<ResponseEntity<String>> responses = Stream.of(
            "sprints/current",
            "sprints/current/tasks"
        ).map(path -> restTemplate.exchange(get(URI.create(buildApiUrl(path))).build(), String.class))
            .toList();

        responses.get(0).getStatusCode().is4xxClientError();

        for(ResponseEntity<String> responseEntity : responses) {
            assertThat(responseEntity.getStatusCode().is4xxClientError(), is(true));
        }
    }

    @Test
    public void createSprintAndSetAsCurrentAndFetchCurrent() {
        ResponseEntity<Sprint> responseEntity = createSprintWithName();

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));
        assertThat(sprintRepository.findByName("Test Sprint 1"), hasProperty("present", is(true)));

        String sprintId = responseEntity.getBody().getId();

        responseEntity = restTemplate.exchange(patch(URI.create(buildApiUrl("sprints/" + sprintId + "")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("current", true)), Sprint.class);

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));

        responseEntity = restTemplate.exchange(get(URI.create(buildApiUrl("sprints/current")))
            .accept(MediaType.APPLICATION_JSON).build(), Sprint.class);

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));
        assertThat(responseEntity.getBody().getId(), is(sprintId));
    }

    @Test
    public void updateSprintWithOnlyBackloggedTrelloCards() {
        ResponseEntity<Sprint> createResponseEntity = createSprintWithName();

        String id = createResponseEntity.getBody().getId();

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, connectionProperties.getBacklogColumnId())));

        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Compelted Test Card 1", connectionProperties.getDoneColumnId()));

        ResponseEntity<SprintTask[]> tasks = invokeSyncTasksWithTrelloEndpoint();

        ResponseEntity<SprintTask[]> responseEntity = restTemplate.exchange(post(URI.create(buildApiUrl("sprints/" + id + "/tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), SprintTask[].class);

        assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));

        assertThat(Arrays.stream(responseEntity.getBody()).toList(), containsInAnyOrder(
            Arrays.stream(tasks.getBody())
                .filter(task -> task.getTrelloCard().getIdList().equals(connectionProperties.getBacklogColumnId()))
                .toArray()
        ));
    }

    @Test
    public void updateNonExistentSprintWithTrelloCardsReturns404() {
        ResponseEntity<Sprint> responseEntity = restTemplate.exchange(post(URI.create(buildApiUrl("sprints/" + UUID.randomUUID() + "/tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);

        assertThat(responseEntity.getStatusCode().is4xxClientError(), is(true));
    }

    @Test
    public void createAndGetSprintTasks() {
        ResponseEntity<Sprint> createResponseEntity = createSprintWithName();
        String id = createResponseEntity.getBody().getId();

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, connectionProperties.getBacklogColumnId())));

        ResponseEntity<SprintTask[]> tasks = invokeSyncTasksWithTrelloEndpoint();

        restTemplate.exchange(post(URI.create(buildApiUrl("sprints/" + id + "/tasks"))).build(), SprintTask[].class);

        ResponseEntity<SprintTask[]> response = restTemplate.exchange(get(URI.create(buildApiUrl("sprints/" + id + "/tasks"))).build(), SprintTask[].class);
        assertThat(response.getStatusCode().is2xxSuccessful(), is(true));

        assertThat(Arrays.stream(response.getBody()).toList(), containsInAnyOrder(
            Arrays.stream(tasks.getBody()).toArray()
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
    public static class TrelloCardsSprintEndpointMock {

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
