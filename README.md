# Github Popularity Score Service

**Github Popularity Score Service** is a backend Spring Boot application that fetches GitHub repositories for a given language, earliest creation date, page number and per page limit. 
It calculates a **popularity score** based on Stars, Forks and Recency of updates.

## 🧮 **Scoring Algorithm & Configuration**
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
    Based on days since last push, we normalize by a half-life.
    Half-life means the freshness value halves after every configured period (e.g., 90 days). This exponential term always generate a value between 0 and 1 (1 when updated today, approaching 0 as it gets older).
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

### **Sample Calculations (small → million-scale)**

Example Weighted Score (weights: stars=0.6, forks=0.25, recency=0.15):

- Given: stargazers=2,000 → log10(2001)=3.301; forks=300 → log10(301)=2.479; days_since=45 → exp(-ln2*45/90)=0.707
- Raw Score ≈ 0.6×3.301 + 0.25×2.479 + 0.15×0.707 = 1.9806 + 0.6198 + 0.1061 = 2.7065

| Stars      | Forks   | Days since | StarsScore log10(1+S) | ForksScore log10(1+F) | RecencyScore e^(-ln2*d/90) | RawScore = 0.6*S + 0.25*F + 0.15*R |
|-----------:|--------:|-----------:|-----------------------:|-----------------------:|----------------------------:|-------------------------------------:|
|        10  |       3 |         15 |                 1.041 |                 0.602 |                       0.891 |                                0.909 |
|       100  |      10 |         30 |                 2.004 |                 1.041 |                       0.794 |                                1.582 |
|     1,000  |     120 |         45 |                 3.000 |                 2.083 |                       0.707 |                                2.427 |
|   100,000  |   5,000 |        120 |                 5.000 |                 3.699 |                       0.397 |                                3.984 |
| 1,000,000  |  20,000 |        365 |                 6.000 |                 4.301 |                       0.082 |                                4.688 |

> RawScore is unnormalized; final normalized output will scale results between 0–100 across the search set considering the maxRawScore.


## 🌟 **Features**

- **REST API for GitHub Repository Search**  
  - Retrieves repositories by language (list of enums), creation date, page number and per page limit, sorted by stars.
    > Languages like: Java, JavaScript, TypeScript, Kotlin, Go, C, C++, C#, Python, Ruby, Swift, PHP,
  HTML, CSS, Shell, Rust, Dart, Scala, R, Objective-C, Groovy, Perl, etc.
  - GitHub Search API limits total results to 1000 records → `page × per_page ≤ 1000`.

- **Configurable via `application.yml`**  
  Customize scoring weights, half-life factors, and GitHub endpoint configuration.

- **Clean HTTP Client (RestTemplate-based)**  
  Uses a lightweight `RestTemplate` client with a custom `ResponseErrorHandler` to translate GitHub API errors (rate limits, 403/404, 5xx) into domain-specific exceptions.

- **Graceful Error & Exception Handling**  
  - All exceptions are handled by a dedicated `GlobalExceptionHandler` class (extending `GlobalExceptionHandlerBase`), which intercepts errors raised anywhere in the application and maps them to descriptive HTTP responses.
  - Includes below GitHub exception hierarchy as GitHubException:
    - `GitHubAuthException`
    - `RateLimitExceededException`
    - `GitHubNotFoundException`
    - `GitHubServerException`
  - This approach prevents unstructured stack traces and guarantees that API consumers always receive clear, predictable JSON error objects.
  - Example Response Structure:
    ```json
    {
      "status": 400,
      "error": "Invalid Date",
      "message": "Parameter 'created_after' cannot be greater than today's date (2025-11-03).",
      "path": "/api/v1/repo/popularityScore"
    }
    ```

- **Swagger UI Integration**  
  Interactive API documentation available at **`/swagger-ui.html`** for quick testing.

- **Caching-Ready**  
  Designed for Spring Cache — supports in-memory or Redis cache if enabled.

- **Unit-Tested with JUnit & Mockito**  
  Includes clean test cases for controller and service layers to ensure correctness and maintainability.

  - **Automated CI/CD with GitHub Actions**  
    The project includes a GitHub Actions workflow (`.github/workflows/build-and-run.yml`) that automates the complete build, test, and deployment pipeline:
    - 🧩 **Triggers** automatically on every commit or pull request across all branches.
    - ⚙️ **Builds** the project using Maven and executes unit tests to ensure code quality.
    - 🐳 **Builds & validates** the Docker image to confirm successful containerization.
    - 🚀 **Publishes the Docker image** to Docker Hub for deployment-ready distribution.
    - ✅ Guarantees consistent, repeatable builds and zero manual intervention during deployment.

    This ensures a fully automated **CI/CD workflow** — from source code changes → build → test → image publish → deploy — delivering a reliable and production-ready pipeline.

              ┌───────────────────────────────┐
              │          Developer            │
              │        (Commit / PR)          │
              └──────────────┬────────────────┘
                             │
                             ▼
               ┌─────────────────────────┐
               │  GitHub Actions Trigger │
               └──────────────┬──────────┘
                              │
                              ▼
             ┌───────────────────────────────┐
             │ 🔧 Build & Test (Maven)       │
             │ - Compile code                │
             │ - Run JUnit tests             │
             └──────────────┬────────────────┘
                            │
                            ▼
             ┌─────────────────────────────────┐
             │ 🐳 Docker Build & Validation    │
             │ - Build Docker image            │
             │ - Run container for verification│
             └──────────────┬──────────────────┘
                            │
                            ▼
             ┌────────────────────────────────┐
             │ 🚀 Push Image to Docker Hub    │
             │ - Tag versioned image          │
             │ - Publish for deployment use   │
             └──────────────┬─────────────────┘
                            │
                            ▼
             ┌───────────────────────────────────┐
             │ ☁️ Deploy / Pull from Registry    │
             │ - Container ready to run in CI/CD │
             └───────────────────────────────────┘

- **Lightweight & Extensible Architecture**  
  Layered design with clear separation of concerns between Controller, Service, and Exception Handling layers — easy to extend for additional GitHub APIs (e.g., commits, contributors, issues).

## 🌟 **Configurable Popularity Scoring**

The service computes a **popularity score** for each GitHub repository based on three weighted factors:  
**stars**, **forks**, and **recency**.  
These weights are fully configurable in `src/main/resources/application.yml`, allowing flexibility without code changes.

### ⚙️ **Configuration Example**

```yaml
popularity.score:
  stars-weight: 0.6          # Weightage of stars in total score
  forks-weight: 0.25         # Weightage of forks in total score
  recency-weight: 0.15       # Weightage of recency in total score
  recency-half-life-days: 90 # Days it takes for recency value to halve
```

### 🧮 **How It Works**

- Stars (0.6) → Highest influence on score. Popular repositories with large star counts rank higher.
- Forks (0.25) → Indicates community adoption and contribution, moderately affects total score.
- Recency (0.15) → Encourages repositories with active development. Newer commits boost the score.
- Half-life concept (90 days) → After every 90 days, the recency influence is halved, ensuring older repos naturally lose recency value.
- This approach ensures a balanced popularity score, prioritizing popular repositories while still considering community engagement and recency updates.


## 🧰 **Tech Stack**

| Component | Technology |
|------------|-------------|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.x |
| **HTTP Client** | RestTemplate |
| **Documentation** | Springdoc OpenAPI (Swagger UI) |
| **Build Tool** | Maven |
| **Testing** | JUnit 5 + Mockito |
| **Containerization** | Docker |
| **CI/CD** | GitHub Actions |
| **Logging** | SLF4J + Logback |

## 📘 **API Endpoint**

### `GET /api/v1/repo/popularityScore`

| Parameter | Type | Description |
|------------|------|-------------|
| `language` | String | GitHub repository language (e.g., Java, Node) |
| `created_after` | String (YYYY-MM-DD) | Earliest repository creation date |
| `page` | int | Page number *(page × per_page ≤ 1000)* |
| `perPage` | int | Results per page *(max 100)* |

**Example Request:**
```bash
curl "http://localhost:8080/api/v1/repo/popularityScore?language=Java&created_after=2024-01-01&page=1&perPage=10"
```

## 🧪 **Running Locally**

### 🧰 **Prerequisites**
Make sure the following are installed:
- **Java 17+**
- **Maven 3.8+**
- **Docker** *(optional, for containerized run)*


### ▶️ **Run using Maven**

```bash
mvn clean spring-boot:run
```
This will start the application on http://localhost:8080.


### 🏗️ **Or build the executable JAR**

- mvn clean package
- java -jar target/github-popularity-score-service-0.0.1.jar


### 🧭 **Access Swagger UI**

Once the app is running, explore and test the API directly from the Swagger interface. The Swagger UI provides an interactive API console to execute requests and view live responses.

👉 http://localhost:8080/swagger-ui.html


### 🐳 **Docker Support**

- 🏗️ **Build Docker Image**
```bash
docker build -t github-popularity-score-service .
```

- ▶️ **Run Container**
```bash
docker run -p 8080:8080 github-popularity-score-service
```
Then visit:
👉 http://localhost:8080/swagger-ui.html


## 🧠 **Future Enhancements**

🔐 GitHub Token Authentication
To increase API rate limits for authenticated users.

⚡ Reactive WebClient Integration
Switch to asynchronous HTTP for non-blocking performance.

🧩 Redis Caching Support
Cache frequent GitHub queries for faster responses and API efficiency.

📊 Extended Scoring Model
Include metrics such as issues, pull requests, and recency decay improvements.

🧱 Composite Scoring Dashboard
Visualize repository popularity across languages and time using aggregated scoring data.