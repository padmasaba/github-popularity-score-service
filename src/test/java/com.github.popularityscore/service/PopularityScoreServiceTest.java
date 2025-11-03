package com.github.popularityscore.service;

import com.github.popularityscore.model.GitHubRepositoryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PopularityScoreService.
 * Using weights: stars=0.6, forks=0.25, recency=0.15, recencyHalfLifeDays=90
 * A close tolerance value of 0.03 is considered to match the close value
 */
@ExtendWith(MockitoExtension.class)
class PopularityScoreServiceTest {

    private PopularityScoreService service;

    @BeforeEach
    void setUp() {
        // Initialize with test weights
        service = new PopularityScoreService(0.6, 0.25, 0.15, 90);
    }

    // 1️⃣ S=10, F=3, D=15  → Expected ≈ 0.909, Actual = 0.9089854166479765
    @Test
    void rawScore_smallRepo_case1() {
        GitHubRepositoryData repo = mock(GitHubRepositoryData.class);
        when(repo.getStargazersCount()).thenReturn(10);
        when(repo.getForksCount()).thenReturn(3);
        when(repo.getUpdatedAt()).thenReturn(OffsetDateTime.now().minusDays(15).toString());

        PopularityScoreService.GitHubRepositoryWithScore scored = service.score(repo);
        assertEquals(0.909, scored.score, 0.03);
    }

    // 2️⃣ S=100, F=10, D=30 → Expected ≈ 1.582, Actual = 1.581996074456757
    @Test
    void rawScore_smallRepo_case2() {
        GitHubRepositoryData repo = mock(GitHubRepositoryData.class);
        when(repo.getStargazersCount()).thenReturn(100);
        when(repo.getForksCount()).thenReturn(10);
        when(repo.getUpdatedAt()).thenReturn(OffsetDateTime.now().minusDays(30).toString());

        PopularityScoreService.GitHubRepositoryWithScore scored = service.score(repo);
        assertEquals(1.582, scored.score, 0.03);
    }

    // 3️⃣ S=1_000, F=120, D=45 → Expected ≈ 2.427, Actual = 2.4270228062446857
    @Test
    void rawScore_midRepo_case3() {
        GitHubRepositoryData repo = mock(GitHubRepositoryData.class);
        when(repo.getStargazersCount()).thenReturn(1_000);
        when(repo.getForksCount()).thenReturn(120);
        when(repo.getUpdatedAt()).thenReturn(OffsetDateTime.now().minusDays(45).toString());

        PopularityScoreService.GitHubRepositoryWithScore scored = service.score(repo);
        assertEquals(2.427, scored.score, 0.03);
    }

    // 4️⃣ S=100_000, F=5_000, D=120 → Expected ≈ 3.984, Actual = 3.9842943588395876
    @Test
    void rawScore_largeRepo_case4() {
        GitHubRepositoryData repo = mock(GitHubRepositoryData.class);
        when(repo.getStargazersCount()).thenReturn(100_000);
        when(repo.getForksCount()).thenReturn(5_000);
        when(repo.getUpdatedAt()).thenReturn(OffsetDateTime.now().minusDays(120).toString());

        PopularityScoreService.GitHubRepositoryWithScore scored = service.score(repo);
        assertEquals(3.984, scored.score, 0.03);
    }

    // 5️⃣ S=1_000_000, F=20_000, D=365 → Expected ≈ 4.688, Actual = 4.684284036508748
    @Test
    void rawScore_megaRepo_case5() {
        GitHubRepositoryData repo = mock(GitHubRepositoryData.class);
        when(repo.getStargazersCount()).thenReturn(1_000_000);
        when(repo.getForksCount()).thenReturn(20_000);
        when(repo.getUpdatedAt()).thenReturn(OffsetDateTime.now().minusDays(365).toString());

        PopularityScoreService.GitHubRepositoryWithScore scored = service.score(repo);
        assertEquals(4.688, scored.score, 0.03);
    }

    // 6️⃣ Normalization: two repos → top one should be ~100, other proportional
    @Test
    void normalization_scalesMaxTo100() {
        GitHubRepositoryData repoA = mock(GitHubRepositoryData.class);
        when(repoA.getStargazersCount()).thenReturn(5_000);
        when(repoA.getForksCount()).thenReturn(300);
        when(repoA.getUpdatedAt()).thenReturn(OffsetDateTime.now().toString());

        GitHubRepositoryData repoB = mock(GitHubRepositoryData.class);
        when(repoB.getStargazersCount()).thenReturn(500);
        when(repoB.getForksCount()).thenReturn(30);
        when(repoB.getUpdatedAt()).thenReturn(OffsetDateTime.now().toString());

        PopularityScoreService.GitHubRepositoryWithScore scoredA = service.score(repoA);
        PopularityScoreService.GitHubRepositoryWithScore scoredB = service.score(repoB);

        List<PopularityScoreService.GitHubRepositoryWithScore> list = java.util.Arrays.asList(scoredA, scoredB);
        double maxRaw = list.stream().mapToDouble(s -> s.score).max().orElse(0.0);

        service.assignNormalizedScores(list, maxRaw);

        // A should normalize to ~100
        assertEquals(100.0, scoredA.normalizedScore, 1e-6);

        // B should be proportional
        double expectedNormalizedB = (scoredB.score / scoredA.score) * 100.0;
        assertEquals(expectedNormalizedB, scoredB.normalizedScore, 1e-6);
    }
}
