package com.github.popularityscore.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Popularity Score Response for each github repository")
public class PopularityScoreResponse {

    @Schema(description = "Repo user name")
    private String name;
    @Schema(description = "Repo user fullname")
    private String fullName;
    @Schema(description = "Repo url")
    private String htmlUrl;
    @Schema(description = "Repo star count")
    private int stars;
    @Schema(description = "Repo fork count")
    private int forks;
    @Schema(description = "Repo last update date")
    private String updatedAt;
    @Schema(description = "Computed raw score")
    private double rawScore;
    @Schema(description = "Normalized score")
    private double normalizedScore;
}



