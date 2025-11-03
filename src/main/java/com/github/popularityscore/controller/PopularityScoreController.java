package com.github.popularityscore.controller;

import com.github.popularityscore.enums.GitHubLanguage;
import com.github.popularityscore.exception.InvalidDateException;
import com.github.popularityscore.model.PopularityScoreResponse;
import com.github.popularityscore.service.RepositorySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequestMapping(path = "/api/v1/repo")
@RestController
@Validated
@Tag(name="Github Popularity Score Manager", description = "APIs for getting popularity score for each repositories")
public class PopularityScoreController {

    private final RepositorySearchService repositorySearchService;

    public PopularityScoreController(RepositorySearchService repositorySearchService) {
        this.repositorySearchService = repositorySearchService;
    }

    @GetMapping("/popularityScore")
    @Operation(operationId = "getPopularityScore", summary = "Get popularity score for each github repositories", description = """
            Retrieves GitHub repositories created after a given date and sorted by stars.
            Note: GitHub API limits total search results to 1000 records
            (page × per_page ≤ 1000).
            """)
    public List<PopularityScoreResponse> getPopularityScore(@RequestParam("language") @Parameter(
                                                                        description = "GitHub Programming Language (case-insensitive)",
                                                                        schema = @Schema(implementation = GitHubLanguage.class)) GitHubLanguage language,
                                                            @RequestParam("created_after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                            @Parameter(description = "Earliest created date in YYYY-MM-DD format (e.g., 2024-01-01)")
                                                            LocalDate createdAfter,
                                                            @RequestParam(name = "page", defaultValue = "10") @Parameter(description = "Page number (page × per_page ≤ 1000 for GitHub API)") int page,
                                                            @RequestParam(name = "perPage", defaultValue = "100") @Parameter(description = "Results per page (max 100, page × per_page ≤ 1000 for GitHub API)") int perPage) {
        // 🛡️ Validate the date
        if (createdAfter.isAfter(LocalDate.now())) {
            throw new InvalidDateException("Parameter 'created_after' cannot be greater than today's date (" + LocalDate.now() + ").");
        }
        return repositorySearchService.search(language.getDisplayName(), createdAfter.toString(), page, perPage);
    }
}
