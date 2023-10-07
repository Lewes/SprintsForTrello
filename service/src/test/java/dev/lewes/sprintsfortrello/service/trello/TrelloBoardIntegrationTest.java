package dev.lewes.sprintsfortrello.service.trello;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.trello.TrelloBoardIntegrationTest.TrelloEndpointMock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, TrelloConnectionProperties.class, TrelloEndpointMock.class})
public class TrelloBoardIntegrationTest {

    @Autowired
    private TrelloService trelloService;

    @Autowired
    private TrelloConnectionProperties connectionProperties;

    @LocalServerPort
    private int serverPort;

    @BeforeEach
    public void before() {
        connectionProperties.setUrl("http://localhost:" + serverPort + "/");
    }

    @Test
    public void fetch_success() {
        String boardId = "test_board_id";

        Optional<TrelloBoard> actualBoard = trelloService.getBoard(boardId);

        assertThat(actualBoard, hasProperty("present", is(true)));
        assertThat(actualBoard.get(), hasProperty("id", is(boardId)));
    }

    @Test
    public void nonexistent_fetch_success() {
        String boardId = "no_such_board_id";

        Optional<TrelloBoard> actualBoard = trelloService.getBoard(boardId);

        assertThat(actualBoard, hasProperty("present", is(false)));
    }

    @RestController
    public static class TrelloEndpointMock {

        @GetMapping("/1/boards/{id}")
        public ResponseEntity<JsonNode> boardsGet(@PathVariable String id) {
            if(!id.equalsIgnoreCase("test_board_id")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ObjectNode parentObject = new ObjectMapper().createObjectNode();
            parentObject.put("id", id);
            parentObject.put("name", "Test Board");
            parentObject.put("desc", "Test Board description");

            return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(parentObject);
        }

    }

}
