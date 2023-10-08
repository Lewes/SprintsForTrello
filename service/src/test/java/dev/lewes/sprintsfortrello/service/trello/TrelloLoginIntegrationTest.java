package dev.lewes.sprintsfortrello.service.trello;

import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.lewes.sprintsfortrello.service.SprintsForTrelloApplication;
import dev.lewes.sprintsfortrello.service.trello.TrelloLoginIntegrationTest.TrelloEndpointMock;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.json.JSONException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, TrelloEndpointMock.class})
public class TrelloLoginIntegrationTest {

    @Autowired
    private TrelloService trelloService;

    @Autowired
    private TrelloConnectionProperties connectionProperties;

    @LocalServerPort
    private int serverPort;

    @BeforeEach
    public void before() {
        connectionProperties.setToken("VALID_TOKEN");
        connectionProperties.setApiKey("VALID_API_KEY");
        connectionProperties.setUrl("http://localhost:" + serverPort + "/");
    }

    @Test
    public void login_success() {
        connectionProperties.setToken("VALID_TOKEN");
        connectionProperties.setApiKey("VALID_API_KEY");

        MatcherAssert.assertThat(trelloService.connect(), is(true));
    }

    @Test
    public void login_invalidToken() {
        connectionProperties.setToken("INVALID_TOKEN");

        MatcherAssert.assertThat(trelloService.connect(), is(false));
    }

    @Test
    public void login_invalidKey() {
        connectionProperties.setApiKey("INVALID_API_KEY");

        MatcherAssert.assertThat(trelloService.connect(), is(false));
    }

    @RestController
    public static class TrelloEndpointMock {

        private String validToken = "VALID_TOKEN";
        private String validApiKey = "VALID_API_KEY";

        @GetMapping("/1/members/me")
        public ResponseEntity<JsonNode> oneMembersMe(@RequestParam("token") String token, @RequestParam("key") String apiKey) throws JSONException {
            if(!token.equals(validToken) || !apiKey.equals(validApiKey)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            ObjectNode parentObject = new ObjectMapper().createObjectNode();
            parentObject.put("id", UUID.randomUUID().toString());

            ResponseEntity entity = ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(parentObject);

            return entity;
        }

    }

}
