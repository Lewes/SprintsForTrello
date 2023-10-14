package dev.lewes.sprintsfortrello.service.sprint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.sprint.Sprint.SprintStatus;
import dev.lewes.sprintsfortrello.service.sprint.SprintTaskHandlerTest.TrelloCardsSprintTaskHandlerEndpointMock;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask;
import dev.lewes.sprintsfortrello.service.tasks.SprintTask.Status;
import dev.lewes.sprintsfortrello.service.tasks.SprintTaskRepository;
import dev.lewes.sprintsfortrello.service.trello.TrelloCard;
import dev.lewes.sprintsfortrello.service.trello.TrelloProperties;
import dev.lewes.sprintsfortrello.service.utils.RestExchangeTestUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private SprintTaskRepository sprintTaskRepository;

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
        long startedTime = System.currentTimeMillis();

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

        assertThat(Arrays.stream(tasks.getBody()).toArray(), hasItemInArray(
            allOf(
                hasProperty("status", is(Status.DONE)),
                hasProperty("timeCompleted", is(allOf(
                    greaterThan(startedTime),
                    lessThan(System.currentTimeMillis())
                )))
            )
        ));
    }

    @Test
    public void progressOfNonStartedSprintReturnsBadRequest() {
        ResponseEntity<Sprint> createResponseEntity = createSprintWithName("Test Sprint 1");
        Sprint sprint = createResponseEntity.getBody();

        ResponseEntity<SprintProgress> sprintProgressResponse = restUtils.getAtUrl("sprints/" + sprint.getId() + "/progress", SprintProgress.class);

        assertThat(sprintProgressResponse.getStatusCode().is4xxClientError(), is(true));
    }

    @Test
    public void progressOfNonExistentSprintReturns404() {
        ResponseEntity<SprintProgress> sprintProgressResponse = restUtils.getAtUrl("sprints/" + UUID.randomUUID() + "/progress", SprintProgress.class);

        assertThat(sprintProgressResponse.getStatusCode().is4xxClientError(), is(true));
    }

    @Test
    public void onlyOneDayEntryForSprintProgressForNewSprint() throws ParseException {
        ResponseEntity<Sprint> createResponseEntity = createSprintWithName("Test Sprint 1");
        Sprint sprint = createResponseEntity.getBody();

        restUtils.patchAtUrl("sprints/" + sprint.getId(), Map.of("status", SprintStatus.IN_PROGRESS), Sprint.class);

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId())));

        invokeSyncTasksWithTrelloEndpoint();

        restUtils.postAtUrl("sprints/" + sprint.getId() + "/tasks", null, SprintTask[].class);

        ResponseEntity<SprintProgress> sprintProgressResponse = restUtils.getAtUrl("sprints/" + sprint.getId() + "/progress", SprintProgress.class);
        SprintProgress sprintProgress = sprintProgressResponse.getBody();

        assertThat(sprintProgress.getDays2RemainingPoints().size(), is(1));
        assertThat(sprintProgress.getDays2RemainingPoints().values().toArray()[0], is(3));

        for(String date : sprintProgress.getDays2RemainingPoints().keySet()) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date parsedDate = simpleDateFormat.parse(date);

            assertThat(parsedDate, allOf(
                is(notNullValue())
            ));

            assertThat(parsedDate.getTime() < System.currentTimeMillis(), is(true));
            assertThat(parsedDate.getTime() >= sprint.getStartTime(), is(true));
        }
    }

    @Test
    public void partiallyCompletedSprintOnlyReturnsUpToPresent() throws ParseException {
        int daysAgo = 4;

        ResponseEntity<Sprint> createResponseEntity = createSprintWithName("Test Sprint 1");
        Sprint sprint = createResponseEntity.getBody();

        sprint.setStatus(SprintStatus.IN_PROGRESS);
        sprint.setStartTime(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo));

        sprintRepository.save(sprint);

        List.of("Test Card 1",
                "Test Card 2",
                "Test Card 3")
            .forEach(name -> trelloCards.add(new TrelloCard(UUID.randomUUID().toString(), name, trelloProperties.getBacklogColumnId())));

        invokeSyncTasksWithTrelloEndpoint();
        restUtils.postAtUrl("sprints/" + sprint.getId() + "/tasks", null, SprintTask[].class);

        SprintTask sprintTask = sprintTaskRepository.findByTrelloCardId(trelloCards.get(0).getId()).get();
        sprintTask.setTimeCompleted(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
        sprintTask.setStatus(Status.DONE);
        sprintTaskRepository.save(sprintTask);

        ResponseEntity<SprintTask[]> sprintTasks = restUtils.getAtUrl("sprints/" + sprint.getId() + "/tasks", SprintTask[].class);

        ResponseEntity<SprintProgress> sprintProgressResponse = restUtils.getAtUrl("sprints/" + sprint.getId() + "/progress", SprintProgress.class);
        SprintProgress sprintProgress = sprintProgressResponse.getBody();

        assertThat(sprintProgress.getDays2RemainingPoints().size(), is(daysAgo + 1));

        for(String date : sprintProgress.getDays2RemainingPoints().keySet()) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date parsedDate = simpleDateFormat.parse(date);

            assertThat(parsedDate, allOf(
                is(notNullValue())
            ));

            assertThat(parsedDate.getTime() < System.currentTimeMillis(), is(true));
            assertThat(parsedDate.getTime() >= setTimeStampToMidnight(sprint.getStartTime()), is(true));

            int value = sprintProgress.getDays2RemainingPoints().get(date);
            int expectedValue = 0;

            for(SprintTask task : sprintTasks.getBody()) {
                if(task.getStatus() == Status.IN_PROGRESS ||
                    task.getStatus() == Status.NOT_STARTED ||
                    (task.getStatus() == Status.DONE && task.getTimeCompleted() < setTimeStampToMidnight(parsedDate.getTime()))) {
                    expectedValue++;
                }
            }

            assertThat(value, is(expectedValue));
        }
    }

    private long setTimeStampToMidnight(long unixTimestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(unixTimestamp);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
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
