package dev.lewes.sprintsfortrello.service;

import static org.hamcrest.Matchers.is;

import dev.lewes.sprintsfortrello.service.TrelloConnectionIntegrationTest.TrelloEndpointMock;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.json.JSONException;
import org.json.JSONObject;
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

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {SprintsForTrelloApplication.class, TrelloConnectionProperties.class, TrelloEndpointMock.class})
public class TrelloConnectionIntegrationTest {

    @Autowired
    private TrelloConnection uut;

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

        MatcherAssert.assertThat(uut.connect(), is(true));
    }

    @Test
    public void login_invalidToken() {
        connectionProperties.setToken("INVALID_TOKEN");

        MatcherAssert.assertThat(uut.connect(), is(false));
    }

    @Test
    public void login_invalidKey() {
        connectionProperties.setApiKey("INVALID_API_KEY");

        MatcherAssert.assertThat(uut.connect(), is(false));
    }

    @RestController
    public static class TrelloEndpointMock {

        private String validToken = "VALID_TOKEN";
        private String validApiKey = "VALID_API_KEY";

        @GetMapping("/1/members/me")
        public ResponseEntity<JSONObject> oneMembersMe(@RequestParam("token") String token, @RequestParam("key") String apiKey) throws JSONException {
            if(!token.equals(validToken) || !apiKey.equals(validApiKey)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            JSONObject parentObject = new JSONObject();
            parentObject.put("id", UUID.randomUUID().toString());

            return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .build();
        }

    }

}
