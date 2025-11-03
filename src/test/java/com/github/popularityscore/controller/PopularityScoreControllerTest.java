package com.github.popularityscore.controller;

import com.github.popularityscore.enums.GitHubLanguage;
import com.github.popularityscore.model.PopularityScoreResponse;
import com.github.popularityscore.service.RepositorySearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for PopularityScoreController (/api/v1/repo/popularityScore).
 *
 * Notes:
 * - Expects InvalidDateException to map to 400. If you haven’t annotated it with @ResponseStatus(BAD_REQUEST)
 *   or don’t have a @ControllerAdvice for it, change the status expectation accordingly.
 */
@WebMvcTest(controllers = PopularityScoreController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PopularityScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositorySearchService repositorySearchService;

    // 1) Happy path — all params provided, 200 OK, service invoked with displayName + ISO date
    @Test
    void getPopularityScore_ok() throws Exception {
        GitHubLanguage lang = GitHubLanguage.JAVA; // change if your enum uses a different constant
        String createdAfter = "2024-01-01";
        int page = 2;
        int perPage = 50;

        List<PopularityScoreResponse> stub = Collections.emptyList();
        when(repositorySearchService.search(eq(lang.getDisplayName()), eq(createdAfter), eq(page), eq(perPage)))
                .thenReturn(stub);

        mockMvc.perform(get("/api/v1/repo/popularityScore")
                        .param("language", lang.name())
                        .param("created_after", createdAfter)
                        .param("page", String.valueOf(page))
                        .param("perPage", String.valueOf(perPage))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(repositorySearchService).search(eq(lang.getDisplayName()), eq(createdAfter), eq(page), eq(perPage));
    }

    // 2) Future date — controller throws InvalidDateException → expect 400 (adjust if mapped differently)
    @Test
    void getPopularityScore_futureDate_returnsBadRequest() throws Exception {
        GitHubLanguage lang = GitHubLanguage.JAVA;
        String future = LocalDate.now().plusDays(1).toString();

        // Service shouldn't be called
        mockMvc.perform(get("/api/v1/repo/popularityScore")
                        .param("language", lang.name())
                        .param("created_after", future)
                        .param("page", "1")
                        .param("perPage", "10"))
                .andExpect(status().isBadRequest()); // requires InvalidDateException -> 400 mapping

        verifyNoInteractions(repositorySearchService);
    }

    // 3) Defaults — omit page & perPage → should use page=10, perPage=100
    @Test
    void getPopularityScore_usesDefaultPaging() throws Exception {
        GitHubLanguage lang = GitHubLanguage.JAVA;
        String createdAfter = "2024-01-01";

        when(repositorySearchService.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/repo/popularityScore")
                        .param("language", lang.name())
                        .param("created_after", createdAfter))
                .andExpect(status().isOk());

        // capture to assert defaults
        ArgumentCaptor<String> langCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> perPageCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(repositorySearchService).search(
                langCaptor.capture(),
                dateCaptor.capture(),
                pageCaptor.capture(),
                perPageCaptor.capture()
        );

        // Language is passed as displayName from controller
        org.junit.jupiter.api.Assertions.assertEquals(lang.getDisplayName(), langCaptor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals(createdAfter, dateCaptor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals(10, pageCaptor.getValue().intValue());     // default from controller
        org.junit.jupiter.api.Assertions.assertEquals(100, perPageCaptor.getValue().intValue()); // default from controller
    }

    // 4) Minimal valid request — only required params
    @Test
    void getPopularityScore_minimalValidRequest() throws Exception {
        GitHubLanguage lang = GitHubLanguage.JAVA;
        String createdAfter = "2023-12-31";

        when(repositorySearchService.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/repo/popularityScore")
                        .param("language", lang.name())
                        .param("created_after", createdAfter))
                .andExpect(status().isOk());

        verify(repositorySearchService, times(1)).search(anyString(), anyString(), anyInt(), anyInt());
    }

    // 5) Verifies JSON body shape when service returns data
    @Test
    void getPopularityScore_returnsBodyFromService() throws Exception {
        GitHubLanguage lang = GitHubLanguage.JAVA;
        String createdAfter = "2024-01-01";

        PopularityScoreResponse r = PopularityScoreResponse.builder()
                .name("repo1")
                .fullName("owner/repo1")
                .htmlUrl("https://github.com/owner/repo1")
                .stars(100)
                .forks(10)
                .updatedAt("2024-05-01T00:00:00Z")
                .rawScore(1.58)
                .normalizedScore(88.0)
                .build();

        when(repositorySearchService.search(eq(lang.getDisplayName()), eq(createdAfter), eq(10), eq(100)))
                .thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/repo/popularityScore")
                        .param("language", lang.name())
                        .param("created_after", createdAfter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("repo1"))
                .andExpect(jsonPath("$[0].fullName").value("owner/repo1"))
                .andExpect(jsonPath("$[0].stars").value(100))
                .andExpect(jsonPath("$[0].forks").value(10))
                .andExpect(jsonPath("$[0].normalizedScore").value(88.0));
    }

    // 6) Ensures service is NOT called when validation fails
    @Test
    void getPopularityScore_futureDate_doesNotCallService() throws Exception {
        GitHubLanguage lang = GitHubLanguage.JAVA;
        String future = LocalDate.now().plusDays(5).toString();

        mockMvc.perform(get("/api/v1/repo/popularityScore")
                        .param("language", lang.name())
                        .param("created_after", future))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(repositorySearchService);
    }
}
