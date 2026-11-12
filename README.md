# Simple Chat Application using Redis (Spring Boot)

REST-only chat backend (no UI) where **Redis is the primary data store**: chat rooms, participants, message history and real-time delivery all live in Redis.

**Stack:** Java 17, Spring Boot 3.3, Spring Data Redis (Lettuce), springdoc-openapi (Swagger), JUnit 5 + Mockito.

## Redis data model

| Purpose | Redis type | Key |
|---|---|---|
| Room metadata (name, createdAt) | **Hash** | `chat:room:{roomId}:meta` |
| Participants of a room | **Set** | `chat:room:{roomId}:participants` |
| Messages, chronological | **List** (JSON strings, `RPUSH`) | `chat:room:{roomId}:messages` |
| Real-time updates | **Pub/Sub** channel | `chat:room:{roomId}:channel` |
| Index of all rooms (atomic uniqueness via `SADD`) | Set | `chat:rooms` |

Each list entry: `{"participant":"guest_user","message":"Hello, everyone!","timestamp":"2024-01-01T10:00:00.000Z"}`

Why it scales: `RPUSH` is O(1); "last N messages" is `LRANGE key -N -1` (O(N), independent of history size); `LTRIM` caps history per room (`chat.max-messages-per-room`); room creation uses atomic `SADD` so concurrent duplicate creates are safe; Pub/Sub fan-out works across multiple app instances.

## Run it

**Option A - Docker (app + Redis with AOF persistence):**
```bash
docker compose up --build
```
**Option B - local:** start Redis (`docker run -p 6379:6379 redis:7-alpine redis-server --appendonly yes`), then
```bash
mvn spring-boot:run
```
App: http://localhost:8080 | **Swagger UI:** http://localhost:8080/swagger-ui.html | OpenAPI JSON: `/v3/api-docs` | Health: `/actuator/health`

Run tests: `mvn test`

Config (env vars): `REDIS_HOST`, `REDIS_PORT`, `PORT`, or for cloud Redis `SPRING_DATA_REDIS_URL=rediss://default:<pass>@<host>:<port>`.
Settings in `application.properties`: `chat.max-messages-per-room`, `chat.default-history-limit`, `chat.max-history-limit`, `chat.enforce-membership`.

## APIs (base: `/api/chatapp/chatrooms`)

| # | Endpoint | Success | Errors |
|---|---|---|---|
| 1 | `POST /` `{"roomName":"general"}` | 201 `{"message":"Chatroom 'general' created successfully.","roomId":"general","status":"success"}` | 409 duplicate, 400 invalid name |
| 2 | `POST /{roomId}/join` `{"participant":"guest_user"}` | 200 `{"message":"User 'guest_user' joined chatroom 'general'.","status":"success"}` | 404 room missing, 400 |
| 3 | `POST /{roomId}/messages` `{"participant":"guest_user","message":"Hello, everyone!"}` | 201 `{"message":"Message sent successfully.","status":"success"}` | 404 room missing, 403 not joined, 400 |
| 4 | `GET /{roomId}/messages?limit=10` | 200 `{"messages":[{participant,message,timestamp}, ...]}` (oldest -> newest) | 404, 400 bad limit |
| extra | `GET /` list rooms, `GET /{roomId}` details | 200 | 404 |
| extra | `DELETE /{roomId}` (Test Case 5) removes meta/participants/messages keys | 200 | 404 |
| extra | `GET /{roomId}/stream` live **SSE** stream of Pub/Sub messages | `text/event-stream` | 404 |

All errors: `{"message":"...","status":"error"}`.

## Test cases walkthrough (curl)
```bash
B=http://localhost:8080/api/chatapp/chatrooms
# TC1 create + join
curl -X POST $B -H 'Content-Type: application/json' -d '{"roomName":"general"}'
curl -X POST $B/general/join -H 'Content-Type: application/json' -d '{"participant":"guest_user"}'
curl -X POST $B/general/join -H 'Content-Type: application/json' -d '{"participant":"another_user"}'
# TC3 real-time: terminal 1 (keeps open)
curl -N $B/general/stream
# TC2 send two messages (terminal 2) -> terminal 1 receives both instantly via Redis Pub/Sub
curl -X POST $B/general/messages -H 'Content-Type: application/json' -d '{"participant":"guest_user","message":"Hello, everyone!"}'
curl -X POST $B/general/messages -H 'Content-Type: application/json' -d '{"participant":"another_user","message":"Hi, guest_user!"}'
curl "$B/general/messages?limit=1"      # last message
curl "$B/general/messages?limit=10"     # both, in order
# TC4 error handling
curl -X POST $B/ghost/messages -H 'Content-Type: application/json' -d '{"participant":"guest_user","message":"hi"}'   # 404
# TC5 delete
curl -X DELETE $B/general
```
A ready Postman collection is in `postman/Redis-Chat-App.postman_collection.json` (set `baseUrl` variable).

## Persistence
`docker-compose.yml` starts Redis with `--appendonly yes --appendfsync everysec` (AOF) plus an RDB snapshot rule, and mounts a volume, so data survives restarts.

## Project structure
```
controller/  REST endpoints (+ SSE stream)      service/     business rules & validation
repository/  Redis access behind interfaces     realtime/    Pub/Sub publisher, subscriber, SSE registry
dto/ model/  request/response & stored objects  exception/   custom exceptions + global handler
config/      Redis listener container, OpenAPI  util/        key naming, room-id normalisation
```
Design: layered architecture, repositories/broadcaster behind interfaces (easy to mock -> unit tests without a Redis server), constructor injection, single `@RestControllerAdvice`.

## Deploy (needed for submission)
Render / Railway / Fly.io: deploy this repo with the included `Dockerfile` and attach a managed Redis (e.g. Upstash, Redis Cloud, Railway Redis). Set `SPRING_DATA_REDIS_URL` (use `rediss://` for TLS). Live Swagger will be at `<your-url>/swagger-ui.html`.

## Live Demo 
<img width="1366" height="768" alt="Screenshot (508)" src="https://github.com/user-attachments/assets/708fe58e-25f8-4378-8d1a-bee8cfe09f42" />

<img width="1366" height="768" alt="Screenshot (509)" src="https://github.com/user-attachments/assets/643d79b6-b3e9-4151-bc61-7056b3118f83" />

<img width="1366" height="768" alt="Screenshot (510)" src="https://github.com/user-attachments/assets/1a9ed964-96e2-4cf0-aa45-44b7b72b710a" />



