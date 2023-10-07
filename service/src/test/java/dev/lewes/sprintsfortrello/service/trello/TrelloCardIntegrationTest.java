package dev.lewes.sprintsfortrello.service.trello;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.trello.TrelloCardIntegrationTest.TrelloCardsEndpointMock;
import java.util.List;
import org.hamcrest.Matchers;
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
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, TrelloConnectionProperties.class, TrelloCardsEndpointMock.class})
public class TrelloCardIntegrationTest {

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

        List<TrelloCard> cards = trelloService.getCards(boardId);

        assertThat(cards, Matchers.containsInAnyOrder(allOf(
            hasProperty("id", is("test_card_id")),
            hasProperty("name", is("Test Card")),
            hasProperty("idList", is("test_list_id"))
        )));
    }

    @Test
    public void nonexistent_fetch_success() {
        String boardId = "no_such_board_id";

        List<TrelloCard> cards = trelloService.getCards(boardId);

        assertThat(cards, Matchers.empty());
    }

    @RestController
    public static class TrelloCardsEndpointMock {

        @GetMapping("/1/cards/{id}")
        public ResponseEntity<JsonNode> boardsGet(@PathVariable String id) {
            if(!id.equalsIgnoreCase("test_board_id")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ObjectNode cardObject = new ObjectMapper().createObjectNode();
            cardObject.put("id", "test_card_id");
            cardObject.put("name", "Test Card");
            cardObject.put("idList", "test_list_id");

            ArrayNode parentNode = new ObjectMapper().createArrayNode();
            parentNode.add(cardObject);

            return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(parentNode);
        }

    }

}
