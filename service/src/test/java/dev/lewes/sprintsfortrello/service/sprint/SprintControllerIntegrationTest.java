package dev.lewes.sprintsfortrello.service.sprint;


import static org.hamcrest.Matchers.is;

import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import java.net.URI;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, SprintController.class})
public class SprintControllerIntegrationTest {

    @LocalServerPort
    private int serverPort;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private SprintRepository sprintRepository;

    @Test
    public void createSprint() {
        ResponseEntity<Sprint> responseEntity = restTemplate.exchange(RequestEntity.post(URI.create(buildApiUrl("sprints")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);

        MatcherAssert.assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));
        MatcherAssert.assertThat(sprintRepository.findByName("Test Sprint 1"), Matchers.hasProperty("present", is(true)));
    }

    @Test
    public void updateSprintWithTrelloCards() {
        ResponseEntity<Sprint> createResponseEntity = restTemplate.exchange(RequestEntity.post(URI.create(buildApiUrl("sprints")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);

        MatcherAssert.assertThat(createResponseEntity.getStatusCode().is2xxSuccessful(), is(true));

        String id = sprintRepository.findByName("Test Sprint 1").get().getId();

        ResponseEntity<Sprint> responseEntity = restTemplate.exchange(RequestEntity.post(URI.create(buildApiUrl("sprints/" + id + "/updateCardsFromTrello")))
            .accept(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Test Sprint 1")), Sprint.class);

        MatcherAssert.assertThat(responseEntity.getStatusCode().is2xxSuccessful(), is(true));
    }

    public String buildApiUrl(String path) {
        return "http://localhost:" + serverPort + "/" + path;
    }

}
