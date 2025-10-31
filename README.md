# Github Popularity Score Service

**Github Popularity Score Service** is a backend Spring Boot application that fetches GitHub repositories for a given language, earliest creation date and page limit. 
It calculates a **popularity score** based on Stars, Forks and Recency of updates.

---

## **Scoring Algorithm & Configuration**
The popularity score is calculated by weighted formula (assumed weights: stars 60%, forks 25%, recency 15%, configurable). We normalize and transform the data using logarithmic and exponential calculation.

1. **Stars (`stargazers_count`)** – measures the repository’s popularity.
2. **Forks (`forks_count`)** – indicates community engagement and adoption.
3. **Recency of updates (`updated_at`)** – freshness factor that denotes when the repository is last updated.

### **Score Calculation Steps**

1. **Compute individual component scores:**

  - **Stars Score (logarithmically scaled):**
    Stargazer counts are unbounded and can vary by orders of magnitude. Log scaling compresses huge values into a small range so extremes do not dominate. For example: `log10(100) = 2`, `log10(1,000,000) = 6`.
    ```text
    starsScore = log10(1 + stargazers_count)
    ```
    
  - **Forks Score (logarithmically scaled):**
    Fork counts are also unbounded; using `log10` keeps very large numbers comparable to smaller ones.
    ```text
    forksScore = log10(1 + forks_count)
    ```
    
  - **Recency Score (exponential scaled):**
    Based on days since last push, we normalize by a half-life:
    Half-life means the freshness value halves every configured period (e.g., 90 days). This exponential term always yields a value between 0 and 1 (1 when updated today, approaching 0 as it gets older).
    ```text
    freshness_decay = math.exp(-math.log(2) * days_since_update / half_life_days)
    ```

2. **Combine component scores into a weighted raw score:**
```text
   rawScore = (starsWeight * starsScore) +(forksWeight * forksScore) +(recencyWeight * recencyScore)
   ```

3. **Normalize the scores to a 0–100 range:**
```text
normalizedScore = 100 * rawScore / maxRawScore
```
`maxRawScore` is the highest raw score among the fetched repositories.


### Sample Calculations (small → million-scale)

Example Weighted Score (weights: stars=0.6, forks=0.25, recency=0.15):

- Given: stargazers=2,000 → log10(2001)=3.301, forks=300 → log10(301)=2.479, days_since=45 → exp(-ln2*45/90)=0.707
- Raw Score ≈ 0.6×3.301 + 0.25×2.479 + 0.15×0.707 = 1.9806 + 0.6198 + 0.1061 = 2.7065

| Stars      | Forks   | Days since | StarsScore log10(1+S) | ForksScore log10(1+F) | RecencyScore e^(-ln2*d/90) | RawScore = 0.6*S + 0.25*F + 0.15*R |
|-----------:|--------:|-----------:|-----------------------:|-----------------------:|----------------------------:|-------------------------------------:|
|        10  |       3 |         15 |                 1.041 |                 0.602 |                       0.891 |                                0.909 |
|       100  |      10 |         30 |                 2.004 |                 1.041 |                       0.794 |                                1.582 |
|     1,000  |     120 |         45 |                 3.000 |                 2.083 |                       0.707 |                                2.427 |
|   100,000  |   5,000 |        120 |                 5.000 |                 3.699 |                       0.397 |                                3.984 |
| 1,000,000  |  20,000 |        365 |                 6.000 |                 4.301 |                       0.082 |                                4.688 |

> RawScore is unnormalized; final presentation will scale results between 0–100 across the search set considering the maxRawScore.

## **Features**
- Fetch repositories via GitHub Search API
- Filter by creation date and programming language
- Compute popularity score from stars, forks, and recency (half-life 90 days)
- Clean HTTP client (RestTemplate-based)
- Configurable scoring weights and half-life via `application.yml`
- Graceful handling of GitHub search/rate limits
- Ready for caching with Spring Cache (in-memory or Redis) if enabled
- Tested and easy to extend

---

---

### **Configuration via `application.yml`**

- Scoring parameters are configurable in `src/main/resources/application.yml`:

  ```yaml
  popularity.score:
  stars-weight: 0.6          # Weightage of stars in total score
  forks-weight: 0.25         # Weightage of forks in total score
  recency-weight: 0.15       # Weightage of recency in total score
  recency-half-life-days: 90 # Days it takes for recency value to halve
  ```
- This configuration allows flexibility to change the weightage contribution without modifying code.
- recency-half-life-days controls how quickly a repository’s recency decays.
- Stars (0.6) dominate and are weighted highest because highly starred repositories are typically more popular.
- Forks (0.25) contribute moderately. It denotes code adoption and engagement.
- Recency (0.15) has smaller influence favors actively maintained repositories.
- This approach ensures a balanced popularity score, prioritizing popular repositories while still considering community engagement and recency updates.

---

## **Technologies Used**

- Java 17
- Spring Boot 3.5.7
- Swagger for API testing
- WebFlux for API calls
- Jackson for JSON processing
- Maven for project management
- Docker for containerization

---