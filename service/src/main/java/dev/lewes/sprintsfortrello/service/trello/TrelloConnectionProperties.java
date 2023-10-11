package dev.lewes.sprintsfortrello.service.trello;

import org.springframework.beans.factory.annotation.Value;

public class TrelloConnectionProperties {

    @Value("${trello.url}")
    private String url;

    @Value("${trello.apiKey}")
    private String apiKey;

    @Value("${trello.token}")
    private String token;

    @Value("${trello.boardId}")
    private String boardId;

    public String getApiKey() {
        return apiKey;
    }

    public String getToken() {
        return token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getBoardId() {
        return boardId;
    }
}