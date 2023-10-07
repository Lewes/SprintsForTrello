package dev.lewes.sprintsfortrello.service.trello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
public class TrelloConnectionProperties {

    public void setUrl(String url) {
        this.url = url;
    }

    @Value("${trello.url}")
    private String url;

    @Value("${trello.apiKey}")
    private String apiKey;

    @Value("${trello.token}")
    private String token;

    public String getApiKey() {
        return apiKey;
    }

    public String getToken() {
        return token;
    }

    public String getUrl() {
        return url;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String buildApiUrl(String path) {
        return getUrl() + path + "?key=" + getApiKey() + "&token=" + getToken();
    }

}