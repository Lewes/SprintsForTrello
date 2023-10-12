package dev.lewes.sprintsfortrello.service.sprint;


import static org.hamcrest.Matchers.containsInAnyOrder;
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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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
        ResponseEntity<Sprint> responseEntity = restTemplate.exchange(post(URI.create(buildApiUrl("sprints")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);

        MatcherAssert.assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));
        MatcherAssert.assertThat(sprintRepository.findByName("Test Sprint 1"), Matchers.hasProperty("present", is(true)));
    }

    @Test
    public void updateSprintWithOnlyBackloggedTrelloCards() {
        ResponseEntity<Sprint> createResponseEntity = restTemplate.exchange(post(URI.create(buildApiUrl("sprints")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);

        String id = createResponseEntity.getBody().getId();

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, connectionProperties.getBacklogColumnId())));

        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), "Compelted Test Card 1", connectionProperties.getDoneColumnId()));

        ResponseEntity<SprintTask[]> tasks = postTasksEndpoint();

        ResponseEntity<Sprint> responseEntity = restTemplate.exchange(post(URI.create(buildApiUrl("sprints/" + id + "/tasks")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);

        MatcherAssert.assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));

        MatcherAssert.assertThat(responseEntity.getBody().getTaskIds(), containsInAnyOrder(
            Arrays.stream(tasks.getBody())
                .filter(task -> task.getTrelloCard().getIdList().equals(connectionProperties.getBacklogColumnId()))
                .map(SprintTask::getId).toArray()
        ));
    }

    private ResponseEntity<SprintTask[]> postTasksEndpoint() {
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

}
