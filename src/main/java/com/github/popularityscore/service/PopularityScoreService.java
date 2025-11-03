package com.github.popularityscore.service;

import com.github.popularityscore.model.GitHubRepositoryData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PopularityScoreService {

    private final double starsWeight;
    private final double forksWeight;
    private final double recencyWeight;
    private final int recencyHalfLifeDays;

    public PopularityScoreService(
            @Value("${popularity.score.stars-weight}") double starsWeight,
            @Value("${popularity.score.forks-weight}") double forksWeight,
            @Value("${popularity.score.recency-weight}") double recencyWeight,
            @Value("${popularity.score.recency-half-life-days}") int recencyHalfLifeDays) {
        this.starsWeight = starsWeight;
        this.forksWeight = forksWeight;
        this.recencyWeight = recencyWeight;
        this.recencyHalfLifeDays = recencyHalfLifeDays;
    }

    public GitHubRepositoryWithScore score(GitHubRepositoryData repo) {
        double raw = computeRawScore(repo);
        return new GitHubRepositoryWithScore(repo, raw, 0.0);
    }

    public void assignNormalizedScores(List<GitHubRepositoryWithScore> repos, double maxRawScore) {
        if (repos == null || repos.isEmpty() || maxRawScore <= 0.0) return;
        for (GitHubRepositoryWithScore item : repos) {
            item.normalizedScore = 100.0 * item.score / maxRawScore;
        }
    }

    private double computeRawScore(GitHubRepositoryData repo) {
        double stars = Math.log10(1 + repo.getStargazersCount());
        double forks = Math.log10(1 + repo.getForksCount());
        double recency = computeFreshness(repo.getUpdatedAt());
        return starsWeight * stars + forksWeight * forks + recencyWeight * recency;
    }

    private double computeFreshness(String updatedAtIso) {
        var updated = java.time.OffsetDateTime.parse(updatedAtIso);
        long days = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(updated, java.time.OffsetDateTime.now()));
        int halfLife = recencyHalfLifeDays > 0 ? recencyHalfLifeDays : 1; // avoid /0
        return Math.exp(-Math.log(2) * days / halfLife);
    }

    /** Dumb DTO (no logic) */
    public static class GitHubRepositoryWithScore {
        public GitHubRepositoryData repo;
        public double score;
        public double normalizedScore;
        public GitHubRepositoryWithScore(GitHubRepositoryData repo, double score, double normalizedScore) {
            this.repo = repo;
            this.score = score;
            this.normalizedScore = normalizedScore;
        }
    }
}



