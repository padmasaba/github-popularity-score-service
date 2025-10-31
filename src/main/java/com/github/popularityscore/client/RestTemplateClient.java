package com.github.popularityscore.client;

import com.github.popularityscore.model.GitHubSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class RestTemplateClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String token;

    public RestTemplateClient(
            @Value("${github.api.base-url:https://api.github.com}") String baseUrl,
            @Value("${github.token:}") String token
    ) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    public GitHubSearchResponse searchRepositories(String query, int page, int perPage) {
        System.out.println(baseUrl);
        System.out.println(query);
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/search/repositories")
                .queryParam("q", query) // ✅ only the search query
                .queryParam("sort", "stars")
                .queryParam("order", "desc")
                .queryParam("page", page)       // ✅ pagination outside q
                .queryParam("per_page", perPage)  // max 100
                .build().toUri();

        System.out.println("url::"+uri.toASCIIString());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (token != null && !token.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<GitHubSearchResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                GitHubSearchResponse.class
        );
        return response.getBody();
    }
}


