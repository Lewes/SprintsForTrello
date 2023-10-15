package dev.lewes.sprintsfortrello.service.sprint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.events.EventsManager;
import dev.lewes.sprintsfortrello.service.events.LazyListener;
import dev.lewes.sprintsfortrello.service.sprint.TasksControllerIntegrationTest.TrelloCardsTasksEndpointMock;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.tasks.events.NewSprintTaskEvent;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import dev.lewes.sprintsfortrello.service.utils.RestExchangeTestUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, SprintController.class, TrelloCardsTasksEndpointMock.class})
public class TasksControllerIntegrationTest {

    private TestRestTemplate restTemplate = new TestRestTemplate();
    private RestExchangeTestUtils restUtils;

    public static List<TrelloCard> trelloCards = new ArrayList<>();

    @Autowired
    private TrelloProperties trelloProperties;

    @LocalServerPort
    private int serverPort;

    @Autowired
    private EventsManager eventsManager;

    @BeforeEach
    public void before() {
        trelloCards.clear();

        trelloProperties.setUrl("http://localhost:" + serverPort + "/");

        restUtils = new RestExchangeTestUtils(trelloProperties);
    }

    @Test
    public void addTasksWithNewTrelloCards() {
        trelloCards.addAll(List.of("Test Card 1",
            "Test Card 2").stream()
            .map(name -> new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId()))
            .toList()
        );

        ResponseEntity<SprintTask[]> tasksResponse = postTasksEndpoint();

        assertThat(tasksResponse.getStatusCode().is2xxSuccessful(), is(true));

        List<SprintTask> actualTasks = Arrays.asList(tasksResponse.getBody());

        assertThat(trelloCards, containsInAnyOrder(
            actualTasks.stream()
                .map(SprintTask::getTrelloCard)
                .toArray())
        );
    }

    @Test
    public void addTasksWithNewTrelloCardsFiresEvent() {
        List<SprintTask> newSprintTasksReceivedByEventListener = new ArrayList<>();

        eventsManager.registerListener(new LazyListener<NewSprintTaskEvent>() {
            @Override
            public void on(NewSprintTaskEvent newSprintTaskEvent) {
                newSprintTasksReceivedByEventListener.add(newSprintTaskEvent.getSprintTask());
            }
        });

        trelloCards.addAll(List.of("Test Card 1",
                "Test Card 2").stream()
            .map(name -> new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId()))
            .toList()
        );

        postTasksEndpoint();

        assertThat(trelloCards, containsInAnyOrder(
            newSprintTasksReceivedByEventListener.stream()
                .map(SprintTask::getTrelloCard)
                .toArray())
        );
    }

    @Test
    public void getTasksReturnsAll() {
        trelloCards.addAll(List.of("Test Card 1",
                "Test Card 2").stream()
            .map(name -> new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId()))
            .toList()
        );

        postTasksEndpoint();

        ResponseEntity<SprintTask[]> tasksResponse = restUtils.getAtUrl("tasks", SprintTask[].class);

        List<SprintTask> actualTasks = Arrays.asList(tasksResponse.getBody());

        assertThat(trelloCards, containsInAnyOrder(
            actualTasks.stream()
                .map(SprintTask::getTrelloCard)
                .toArray()));
    }

    @Test
    public void updateExistingTasksDoesNotAddDuplicates() {
        List.of("Test Card 1",
            "Test Card 2",
            "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId())));

        postTasksEndpoint();

        trelloCards.get(0).setName("Test Card 1 Renamed");
        trelloCards.get(1).setIdList("another_column_id");

        ResponseEntity<SprintTask[]> tasksResponse = postTasksEndpoint();

        assertThat(tasksResponse.getStatusCode().is2xxSuccessful(), is(true));

        List<SprintTask> actualTasks = Arrays.asList(tasksResponse.getBody());

        assertThat(trelloCards, containsInAnyOrder(
            actualTasks.stream()
                .map(SprintTask::getTrelloCard)
                .toArray())
        );
    }

    @Test
    public void addedTasksParsesPoints() {
        trelloCards.addAll(List.of("Test Card 1 [3]",
                "Test Card 2 [2]").stream()
            .map(name -> new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId()))
            .toList()
        );

        ResponseEntity<SprintTask[]> tasksResponse = postTasksEndpoint();

        assertThat(tasksResponse.getStatusCode().is2xxSuccessful(), is(true));

        List<SprintTask> actualTasks = Arrays.asList(tasksResponse.getBody());

        assertThat(trelloCards.stream()
            .map(this::cardNameToPoints)
            .toList(), containsInAnyOrder(
            actualTasks.stream()
                .map(SprintTask::getPoints)
                .toArray())
        );
    }

    public int cardNameToPoints(TrelloCard card) {
        Pattern pattern = Pattern.compile("\\[(.*?)]");
        Matcher matcher = pattern.matcher(card.getName());

        if(matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

    private ResponseEntity<SprintTask[]> postTasksEndpoint() {
        return restUtils.postAtUrl("tasks", null, SprintTask[].class);
    }

    @RestController
    public static class TrelloCardsTasksEndpointMock {

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