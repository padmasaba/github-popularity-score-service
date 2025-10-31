package com.github.popularityscore.service;

import com.github.popularityscore.model.GitHubRepositoryData;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static java.lang.Math.*;
import org.springframework.beans.factory.annotation.Value;


@Service
public class PopularityScoreService {
    private static final int HALF_LIFE_DAYS = 90;
    @Value("${popularity.score.stars-weight}")
    private double starsWeight;

    @Value("${popularity.score.forks-weight}")
    private double forksWeight;

    @Value("${popularity.score.recency-weight}")
    private double recencyWeight;

    @Value("${popularity.score.recency-half-life-days}")
    private static final int recencyHalfLifeDays = 0;

    public void assignNormalizedScores(List<GitHubRepositoryWithScore> repos, double maxRawScore) {
        if (repos == null || repos.isEmpty()) {
            return;
        }
        for (GitHubRepositoryWithScore item : repos) {
            if (maxRawScore > 0.0) {
                item.normalizedScore = 100.0 * item.score / maxRawScore;
            }
        }
    }

    public static class GitHubRepositoryWithScore {
        public GitHubRepositoryData repo;
        public double score;
        public double normalizedScore;

        public GitHubRepositoryWithScore(GitHubRepositoryData repo) {
            this.repo = repo;
            this.score = computeRawScore(repo);
            this.normalizedScore = 0.0;
        }

        private static double computeRawScore(GitHubRepositoryData repo) {
            double starsScore = log10(1 + repo.getStargazersCount());
            double forksScore = log10(1 + repo.getForksCount());
            double recencyScore = computeFreshness(repo.getUpdatedAt());
            return 0.6 * starsScore + 0.25 * forksScore + 0.15 * recencyScore;
        }

        private static double computeFreshness(String updatedAtIso) {
            try {
                OffsetDateTime updated = OffsetDateTime.parse(updatedAtIso);
                long days = max(0, ChronoUnit.DAYS.between(updated, OffsetDateTime.now()));
                return exp(-log(2) * days / HALF_LIFE_DAYS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}



