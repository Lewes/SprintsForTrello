package dev.lewes.sprintsfortrello.service.sprint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.RequestEntity.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.sprint.Sprint.SprintStatus;
import dev.lewes.sprintsfortrello.service.sprint.SprintControllerIntegrationTest.TrelloCardsSprintEndpointMock;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import dev.lewes.sprintsfortrello.service.utils.RestExchangeTestUtils;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, TrelloCardsSprintEndpointMock.class})
public class SprintControllerIntegrationTest {

    @LocalServerPort
    private int serverPort;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private TrelloProperties trelloProperties;

    public static List<TrelloCard> trelloCards = new ArrayList<>();

    private RestExchangeTestUtils restUtils;

    @BeforeEach
    public void before() {
        trelloCards.clear();

        trelloProperties.setUrl("http://localhost:" + serverPort + "/");

        restUtils = new RestExchangeTestUtils(trelloProperties);
    }

    @Test
    public void createSprint() {
        ResponseEntity<Sprint> createdSprint = createTestSprintAndReturn();
        ResponseEntity<Sprint> fetchSprint = restUtils.getAtUrl("sprints/" + createdSprint.getBody().getId(), Sprint.class);;

        assertThat(fetchSprint.getBody(), is(createdSprint.getBody()));
    }

    @Test
    public void getNonExistentSprintReturns404() {
        ResponseEntity<Sprint> nonExistentSprintResponse = restUtils.getAtUrl("sprints/" + UUID.randomUUID(), Sprint.class);

        assertThatResponseStatusCodeIsBadRequest(nonExistentSprintResponse);
    }

    @Test
    public void createSprintAsCurrent() {
        ResponseEntity<Sprint> createSprintResponse = restUtils.postAtUrl("sprints",
            Map.of("name", "Test Sprint 1",
                "current", true),
            Sprint.class);
        Sprint createdSprint = createSprintResponse.getBody();

        ResponseEntity<Sprint> currentSprintResponse = getCurrentSprint();
        Sprint currentSprint = currentSprintResponse.getBody();

        assertThat(currentSprint, is(createdSprint));
    }

    private static void assertThatResponseStatusCodeIsOk(ResponseEntity<?> createSprintResponse) {
        assertThat(createSprintResponse.getStatusCode().is2xxSuccessful(), is(true));
    }

    private ResponseEntity<Sprint> getCurrentSprint() {
        return restUtils.getAtUrl("sprints/current", Sprint.class);
    }

    @Test
    public void noCurrentSprintReturns404UponFetching() {
        List<ResponseEntity<String>> sprintResponses = Stream.of(
            "sprints/current",
            "sprints/current/tasks"
        ).map(path -> restUtils.getAtUrl(path, String.class)).toList();

        for(ResponseEntity<String> sprintResponse : sprintResponses) {
            assertThatResponseStatusCodeIsBadRequest(sprintResponse);
        }
    }

    private static void assertThatResponseStatusCodeIsBadRequest(ResponseEntity<?> sprintResponse) {
        assertThat(sprintResponse.getStatusCode().is4xxClientError(), is(true));
    }

    @Test
    public void createSprintAndSetAsCurrentAndFetchCurrent() {
        Sprint createdSprint = createTestSprintAndReturn().getBody();

        ResponseEntity<Sprint> setSprintAsCurrentResponse = setSprintAsCurrent(createdSprint.getId());

        ResponseEntity<Sprint> currentSprintResponse = getCurrentSprint();
        assertThat(currentSprintResponse.getBody(), is(setSprintAsCurrentResponse.getBody()));
    }

    private ResponseEntity<Sprint> createTestSprintAndReturn() {
        return createSprintWithName("Test Sprint 1");
    }

    private ResponseEntity<Sprint> setSprintAsCurrent(String sprintId) {
        return restTemplate.exchange(patch(URI.create(restUtils.buildApiUrl("sprints/" + sprintId)))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("current", true)), Sprint.class);
    }

    @Test
    public void updateSprintWithOnlyBackloggedTrelloCards() {
        ResponseEntity<Sprint> createResponseEntity = createTestSprintAndReturn();
        String sprintId = createResponseEntity.getBody().getId();

        generateTestTrelloCards();

        trelloCards.add(new TrelloCard(UUID.randomUUID().toString(),
            "Completed Test Card 1",
            trelloProperties.getDoneColumnId()));

        ResponseEntity<SprintTask[]> tasks = syncTasksWithTrello();
        ResponseEntity<SprintTask[]> sprintTasksResponse = addCardsToSprint(sprintId);

        assertThatResponseStatusCodeIsOk(sprintTasksResponse);

        assertThat(Arrays.stream(sprintTasksResponse.getBody()).toList(), containsInAnyOrder(
            Arrays.stream(tasks.getBody())
                .filter(task -> task.getTrelloCard().getIdList().equals(trelloProperties.getBacklogColumnId()))
                .toArray()
        ));
    }

    private void generateTestTrelloCards() {
        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(),
                name,
                trelloProperties.getBacklogColumnId())));
    }

    @Test
    public void updateNonExistentSprintWithTrelloCardsReturns404() {
        ResponseEntity<Sprint> sprintResponse = restUtils.postAtUrl("sprints/" + UUID.randomUUID() + "/tasks", null, Sprint.class);

        assertThat(sprintResponse.getStatusCode().is4xxClientError(), is(true));
    }

    @Test
    public void createAndGetSprintTasks() {
        ResponseEntity<Sprint> createResponseEntity = createTestSprintAndReturn();
        String sprintId = createResponseEntity.getBody().getId();

        generateTestTrelloCards();

        ResponseEntity<SprintTask[]> tasks = syncTasksWithTrello();
        addCardsToSprint(sprintId);

        ResponseEntity<SprintTask[]> response = getTasksInSprint(sprintId);

        assertThat(Arrays.stream(response.getBody()).toList(), containsInAnyOrder(
            tasks.getBody()
        ));
    }

    private ResponseEntity<SprintTask[]> getTasksInSprint(String sprintId) {
        return restUtils.getAtUrl("sprints/" + sprintId + "/tasks", SprintTask[].class);
    }

    private ResponseEntity<SprintTask[]> addCardsToSprint(String sprintId) {
        return restUtils.postAtUrl("sprints/" + sprintId + "/tasks", null, SprintTask[].class);
    }

    @Test
    public void startSprint() {
        long preStartTime = System.currentTimeMillis();

        ResponseEntity<Sprint> createResponse = createTestSprintAndReturn();
        String sprintId = createResponse.getBody().getId();

        ResponseEntity<Sprint> startSprintResponse = changeSprintStatus(sprintId, SprintStatus.IN_PROGRESS);

        assertThat(startSprintResponse.getBody(), hasProperty("status", is(SprintStatus.IN_PROGRESS)));
        assertThat(startSprintResponse.getBody().getStartTime() > preStartTime &&
            startSprintResponse.getBody().getStartTime() < System.currentTimeMillis(), is(true));
        assertThat(startSprintResponse.getBody().getEstimatedDurationInDays(), is(trelloProperties.getSprintLengthInDays()));

        ResponseEntity<SprintTask[]> sprintTasksResponse = getTasksInSprint(sprintId);

        assertThat(startSprintResponse.getBody().getStartingPoints(), is(Arrays.stream(sprintTasksResponse.getBody()).mapToInt(task -> task.getPoints()).sum()));
    }

    @Test
    public void startingSprintTwiceReturnsBadRequest() {
        ResponseEntity<Sprint> createResponse = createTestSprintAndReturn();
        String sprintId = createResponse.getBody().getId();

        changeSprintStatus(sprintId, SprintStatus.IN_PROGRESS);
        ResponseEntity<Sprint> startSprintAgainResponse = changeSprintStatus(sprintId, SprintStatus.IN_PROGRESS);

        assertThat(startSprintAgainResponse.getStatusCode().is4xxClientError(), is(true));
    }

    @Test
    public void startingSprintWhenItsFinishedReturnsBadRequest() {
        ResponseEntity<Sprint> createResponse = createTestSprintAndReturn();
        String sprintId = createResponse.getBody().getId();

        changeSprintStatus(sprintId, SprintStatus.IN_PROGRESS);
        changeSprintStatus(sprintId, SprintStatus.ENDED);
        ResponseEntity<Sprint> startSprintAgainResponse = changeSprintStatus(sprintId, SprintStatus.IN_PROGRESS);

        assertThat(startSprintAgainResponse.getStatusCode().is4xxClientError(), is(true));
    }

    private ResponseEntity<Sprint> changeSprintStatus(String sprintId, SprintStatus status) {
        return restUtils.patchAtUrl("sprints/" + sprintId,
            Map.of("status", status),
            Sprint.class);
    }

    private ResponseEntity<Sprint> createSprintWithName(String name) {
        return restUtils.postAtUrl("sprints", Map.of("name", name), Sprint.class);
    }

    private ResponseEntity<SprintTask[]> syncTasksWithTrello() {
        return restUtils.postAtUrl("tasks", null, SprintTask[].class);
    }

    @RestController
    public static class TrelloCardsSprintEndpointMock {

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
