package com.github.popularityscore.service;

import com.github.popularityscore.client.RestTemplateClient;
import com.github.popularityscore.model.GitHubRepositoryData;
import com.github.popularityscore.model.GitHubSearchResponse;
import com.github.popularityscore.model.PopularityScoreResponse;
import com.github.popularityscore.service.PopularityScoreService.GitHubRepositoryWithScore;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepositorySearchService {

    private final RestTemplateClient restTemplateClient;
    private final PopularityScoreService scoringService;

    public RepositorySearchService(RestTemplateClient restTemplateClient, PopularityScoreService popularityScoreService) {
        this.restTemplateClient = restTemplateClient;
        this.scoringService = popularityScoreService;
    }

    public List<PopularityScoreResponse> search(String query, int page, int perPage) {
        GitHubSearchResponse response = restTemplateClient.searchRepositories(query, page, perPage);
        List<GitHubRepositoryData> items = response == null || response.getItems() == null ? List.of() : response.getItems();
        List<GitHubRepositoryWithScore> withScores = items.stream()
                .map(GitHubRepositoryWithScore::new)
                .collect(Collectors.toList());
        double maxRaw = withScores.stream().mapToDouble(r -> r.score).max().orElse(0.0);
        scoringService.assignNormalizedScores(withScores, maxRaw);

        return withScores.stream()
                .sorted(Comparator.comparingDouble((GitHubRepositoryWithScore r) -> r.score).reversed())
                .map(item -> PopularityScoreResponse.builder()
                        .name(item.repo.getName())
                        .fullName(item.repo.getFullName())
                        .htmlUrl(item.repo.getHtmlUrl())
                        .stars(item.repo.getStargazersCount())
                        .forks(item.repo.getForksCount())
                        .updatedAt(item.repo.getUpdatedAt())
                        .rawScore(item.score)
                        .normalizedScore(item.normalizedScore)
                        .build())
                .collect(Collectors.toList());
    }

    public List<PopularityScoreResponse> search(String language, String createdAfter, int page, int perPage) {
        StringBuilder q = new StringBuilder();
        if (language != null && !language.isBlank()) {
            q.append("language:").append(language.trim());
        }
        if (createdAfter != null && !createdAfter.isBlank()) {
            if (q.length() > 0) q.append(' ');     // <-- add a space between qualifiers
            q.append("created:>").append(createdAfter.trim());
        }

        return search(q.toString(), page, perPage);
    }
}



