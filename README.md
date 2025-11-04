# Github Popularity Score Service — Keycloak Integration & Docker Compose Deployment

**Github Popularity Score Service** is a backend Spring Boot application that fetches GitHub repositories for a given language, earliest creation date, page number and per page limit. 
It calculates a **popularity score** based on Stars, Forks and Recency of updates. This project integrates **Keycloak authentication** with Spring Boot and Swagger UI.
Swagger gets loaded publicly, and on clicking Authorize it redirects users to Keycloak OIDC for login (oauth can be enabled or disabled).
The application uses **Docker Compose** to orchestrate both the API and Keycloak for **secure, scalable, and easily reproducible deployments**.

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

- **Keycloak + Swagger Configuration**
  - Integrated Keycloak authentication using Spring Security and OpenID Connect (OIDC). 
  - Security can be toggled via a simple flag in application.yml:
    ```bash
    app:
      auth:
        enabled: true   # or false
    ```
  - Swagger UI (/swagger-ui.html) loads without login; users click Authorize to sign in via Keycloak. 
  - Supports Public client (no client secret) with PKCE for secure token exchange. 
  - After successful login, Swagger stores the access token and attaches it automatically to all API requests. 
  - Keycloak setup:
    - Realm: github-popularity 
    - Client ID: github-popularity-api 
    - Client Type: Public (Client Authentication OFF)
    - Redirect URI: http://localhost:8080/swagger-ui/oauth2-redirect.html
    - Web Origin: http://localhost:8080

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
  Interactive API documentation available at **`/swagger-ui/index.html`** for quick testing.

- **Unit-Tested with JUnit & Mockito**  
  Includes clean test cases for controller and service layers to ensure correctness and maintainability.

- **🧩 Automated CI/CD with GitHub Action Runner: Build, Test & Run Container**
  - This GitHub Actions workflow(`.github/workflows/build-and-run.yml`) automates the full CI/CD process for a Java-based project.
    - 🧩 **Triggers** automatically on every commit or pull request across all branches.
    - ⚙️ **Builds** the project using Maven and executes unit tests to ensure code quality.
    - 🐳 **Builds & validates** the Docker image to confirm successful containerization.
    - 🚀 **Publishes the Docker image** to Docker Hub for deployment-ready distribution.
    - ✅ Guarantees consistent, repeatable builds and zero manual intervention during deployment.
  - Build & Test – Checks out the repository, sets up Java 17, and runs Maven tests. 
  - Docker Build & Push – Builds a Docker image (auto-detects the Dockerfile) and pushes it to GitHub Container Registry (GHCR) when changes are merged to main. 
  - Run & Verify – Runs the container to verify that the image works correctly (both for main and PR branches). 
  - Concurrency Control – Cancels older workflow runs for the same branch or PR to save resources. 
  - This ensures every push or PR results in a validated, deployable container image.

  This ensures a fully automated **CI/CD workflow** — from source code changes → build → test → image publish → deploy — delivering a reliable and production-ready pipeline.

               ┌───────────────────────────────┐
               │          Developer            │
               │        (Commit / PR)          │
               └──────────────┬────────────────┘
                              │
                              ▼
               ┌───────────────────────────────┐
               │  GitHub Actions Trigger       │
               └──────────────┬────────────────┘
                              │
                              ▼
               ┌───────────────────────────────┐
               │    Build & Test (Maven)       │
               │ - Compile code                │
               │ - Run JUnit tests             │
               └──────────────┬────────────────┘
                              │
                              ▼
               ┌───────────────────────────────────┐
               │    Docker Build & Validation      │
               │ - Build Docker image              │
               │ - Run container for verification  │
               └──────────────┬────────────────────┘
                              │
                              ▼
               ┌───────────────────────────────────┐
               │    Push Image to Docker Hub       │
               │ - Tag & Publish image             │
               └──────────────┬────────────────────┘
                              │
                              ▼
               ┌───────────────────────────────────┐
               │   Deploy / Pull from Registry     │
               │ - Container ready to go           │
               └───────────────────────────────────┘
    - How to pull the image from registry and run in docker container
      ```bash
      $TOKEN="ghp_**"
      $USER  = "padmasaba"
      $TOKEN | docker login ghcr.io -u $USER --password-stdin
      docker images
      docker pull ghcr.io/padmasaba/github-popularity-score-service:latest
      docker run -d -p 8080:8080 --name app ghcr.io/padmasaba/github-popularity-score-service:latest
      docker ps -a
      docker start app
      docker stop app
      ```

- **Lightweight & Extensible Architecture**  
  Layered design with clear separation of concerns between Controller, Service, and Exception Handling layers — easy to extend for additional GitHub APIs (e.g., commits, contributors, issues).


## 🐳 Docker Compose Deployment (With & Without Keycloak Security)

The application can be run either **with** or **without Keycloak authentication**, using Docker Compose profiles.

### 🧹 1. File Structure

```
github-popularity-score-service/
├─ docker-compose.yml
├─ keycloak/
│  └─ realm-github-popularity.json
├─ src/
│  └─ main/resources/
│     ├─ application.yml
│     └─ application-keycloak.yml
```

- `application.yml` → runs **without security**
- `application-keycloak.yml` → runs **with Keycloak security**
- `realm-github-popularity.json` → Keycloak realm and client configuration (auto-imported)

---

### 👢 2. Docker Compose Profiles

The compose file defines two profiles:
- `noauth` → starts app without Keycloak
- `secure` → starts Keycloak + secured app

---

### ▶️ 3. Run Without Security
Run only the app with Swagger (no Keycloak):

```bash
docker compose --profile noauth up -d
```

- Starts `app` only
- Loads `application.yml`
- Visit: 🔗 http://localhost:8080/swagger-ui/index.html

To stop:
```bash
docker compose --profile noauth down -v
```

---

### 🔐 4. Run With Keycloak Security
Run with Keycloak authentication enabled:

```bash
docker compose --profile secure up -d
```

- Starts `keycloak` + `app-secure`
- Loads `application-keycloak.yml`
- Keycloak console → http://localhost:8180
    - Admin: `admin` / `admin`
    - Realm: `github-popularity`
    - Client: `github-popularity-api`
- Swagger UI → http://localhost:8080/swagger-ui/index.html
    - Click **Authorize** → Keycloak login (`psa` / `psa` if configured)

To stop:
```bash
docker compose --profile secure down -v
```

### ⚡ 5. Scalability & Performance

Docker Compose allows seamless horizontal scaling of the GitHub Popularity Score Service — each component (API, Keycloak, Swagger UI) runs in its own container for isolation and efficiency.
We can easily scale services for performance testing or production workloads using:
```bash
docker compose --profile secure up -d --scale app-secure=3
```

### 💪 6. Summary Commands
| Task | Command |
|------|----------|
| Start without security | `docker compose --profile noauth up -d` |
| Stop without security | `docker compose --profile noauth down -v` |
| Start with Keycloak security | `docker compose --profile secure up -d` |
| Stop with Keycloak security | `docker compose --profile secure down -v` |

This enables easy switching between unsecured and secured deployments for testing and development.

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
|-------|-------------|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.x |
| **HTTP Client** | RestTemplate |
| **Security & Authentication** | Keycloak (OAuth2 / OpenID Connect) |
| **API Documentation** | Springdoc OpenAPI (Swagger UI) |
| **Build Tool** | Maven |
| **Testing** | JUnit 5 + Mockito |
| **Containerization** | Docker |
| **Orchestration** | Docker Compose (Multi-container setup with Keycloak & API) |
| **Continuous Integration / Delivery** | GitHub Actions |
| **Configuration Management** | Spring Profiles (application.yml, application-keycloak.yml) |
| **Scalability Ready** | Load Balancer (optional for multi-instance scaling) |
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
- **Keycloak server (optional when security is enabled)**


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

👉 http://localhost:8080/swagger-ui/index.html


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
👉 http://localhost:8080/swagger-ui/index.html


## 🧠 **Future Enhancements**

⚡ Reactive WebClient Integration
Switch to asynchronous HTTP for non-blocking performance.

🧩 Redis Caching Support / Database / Persistence
Cache frequent GitHub queries for faster responses and API efficiency.
If applicable, e.g., PostgreSQL / Oracle / In-memory H2 can also be extended and integrated

📊 Extended Scoring Model
Include metrics such as issues, pull requests, and recency decay improvements.
