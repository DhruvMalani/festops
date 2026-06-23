# FestOps

FestOps is an incident management and dispatch platform for a large cultural festival. It accepts emergency SOS reports, automatically triages each one into a category and severity, dispatches the best-suited responder using a proximity/skill/load scoring model, tracks every incident through a strict lifecycle state machine, and persists a full audit trail. It also includes an attendee-facing "concierge" that surfaces nearby events starting soon, a Swing operations dashboard, and a pluggable LLM-backed triage strategy.
Developed by Dhruv Malani and Yash Jain — CS F213, BITS Pilani, Summer 2026


## The problem it solves

At a festival with tens of thousands of attendees spread across many venues, incidents (medical emergencies, fires, security situations, logistics failures) arrive faster than a human dispatcher can manually classify, prioritize, and assign. FestOps automates the hot path: an SOS lands on a queue, is classified and assigned to the nearest qualified, least-loaded responder within milliseconds, and is escalated automatically if it breaches its service-level agreement (SLA). The result is faster response times, consistent prioritization, and a complete, queryable record of who did what and when.

## Tech stack

- **Java 17** — records, sealed-style hierarchies, switch expressions, text blocks
- **Spring Boot 3.3** — REST API (`spring-boot-starter-web`), dependency injection, lifecycle management
- **Java Swing** — desktop operations dashboard (`OperationsDashboard`)
- **H2 (embedded) + plain JDBC** — audit log persistence via `DriverManager`/`PreparedStatement`/`ResultSet`
- **Anthropic Messages API** — optional LLM-backed triage (`java.net.http.HttpClient` + Gson), key read from `ANTHROPIC_API_KEY`
- **Gson 2.11** — JSON (de)serialization for the LLM client and CSV-free data interchange
- **JUnit 5 (Jupiter)** — unit tests for the state machine, dispatcher, triage, and geo utilities

## Project structure

```
com.festops
├── FestOpsApplication            # Spring Boot entry point
│
├── model/                        # Domain entities
│   ├── Incident (abstract)       #   base of the incident inheritance hierarchy
│   ├── MedicalIncident           #   CRITICAL · skill "medical"   · SLA 120s
│   ├── SecurityIncident          #   HIGH     · skill "security"  · SLA 180s
│   ├── FireIncident              #   CRITICAL · skill "fire"      · SLA 90s
│   ├── LogisticsIncident         #   LOW      · skill "maintenance"· SLA 600s
│   ├── Severity, IncidentType    #   enums
│   ├── Responder                 #   field responder (thread-safe mutable state)
│   ├── Event, UserLocation       #   concierge domain
│   ├── SosReport                 #   raw intake record
│   └── IncidentObserver          #   observer interface for lifecycle changes
│
├── state/                        # State pattern — incident lifecycle
│   ├── IncidentState (interface)
│   ├── ReportedState, AcknowledgedState, RespondingState,
│   │   ResolvedState, EscalatedState
│
├── factory/                      # Factory pattern
│   └── IncidentFactory           #   builds the right Incident subclass from IncidentType
│
├── triage/                       # Strategy pattern — SOS text -> IncidentType
│   ├── TriageStrategy (interface)
│   ├── RuleBasedTriageStrategy   #   regex Pattern/Matcher classification
│   └── AgenticTriageStrategy     #   Anthropic Messages API + 5s timeout + rule-based fallback
│
├── dispatch/                     # Responder assignment
│   └── Dispatcher                #   skill×0.4 + proximity×0.3 + load×0.3 scoring (ConcurrentHashMap)
│
├── dao/                          # Data access (plain JDBC + CSV)
│   ├── AuditDAO                  #   audit_log table in H2 via JDBC, try-with-resources
│   └── ResponderCsvLoader        #   loads responders from CSV via BufferedReader
│
├── service/                      # Application/business logic
│   ├── FestOpsService            #   producer-consumer intake, dispatch, SLA monitor, transitions
│   ├── ConciergeService          #   loads events.csv, ranks nearby upcoming events
│   ├── LocationService           #   user location pings (ConcurrentHashMap)
│   ├── AuditLogObserver          #   IncidentObserver -> AuditDAO
│   └── RankedEvent               #   scored event result
│
├── controller/                   # REST layer
│   ├── SosController             #   POST /api/v1/sos
│   ├── IncidentController        #   GET/PATCH /api/v1/incidents
│   ├── ResponderController       #   GET/PATCH /api/v1/responders
│   ├── LocationController        #   POST /api/v1/location, GET /api/v1/events/nearby
│   ├── GlobalExceptionHandler    #   maps domain exceptions to HTTP status codes
│   └── dto/                      #   request/response records
│
├── exception/                    # Domain exceptions
│   ├── InvalidStateTransitionException   (-> 409)
│   ├── IncidentNotFoundException         (-> 404)
│   ├── ResponderNotFoundException        (-> 404)
│   └── NoResponderAvailableException
│
├── util/
│   └── HaversineUtil             #   great-circle distance
│
└── ui/                           # Standalone clients
    ├── DemoRunner                #   no-Spring pipeline demo (BlockingQueue -> triage -> dispatch -> lifecycle)
    ├── OperationsDashboard       #   Swing JTable dashboard (HttpClient + SwingWorker + Timer)
    └── DashboardApp              #   Swing launcher

data/
├── responders.csv               # 10 responders (BITS Pilani coords)
└── events.csv                   # 17 fest events
```

## Running locally

### Prerequisites
- **JDK 17+** (the project targets Java 17 bytecode; it compiles and runs on newer JDKs too)
- **Maven 3.9+**
- *(optional)* an **Anthropic API key** for LLM triage — without it, triage falls back to the rule-based strategy

### Build
```bash
mvn clean compile
```

### Run the REST server
```bash
mvn spring-boot:run
```
The API comes up on `http://localhost:8080`. The H2 console is at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:festops`, user `sa`, no password).

To enable LLM-backed triage instead of the regex rules:
```bash
export ANTHROPIC_API_KEY=sk-ant-...      # never commit this
mvn spring-boot:run -Dspring-boot.run.arguments=--festops.triage.mode=agentic
```

### Run the standalone pipeline demo (`DemoRunner`, no server)
```bash
mvn clean compile
mvn dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/classes:$(cat target/cp.txt)" com.festops.ui.DemoRunner
```

### Run the Swing dashboard (server must be running)
```bash
mvn dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/classes:$(cat target/cp.txt)" com.festops.ui.DashboardApp
```

### Run the tests
```bash
mvn test
```

## REST API

All paths are prefixed with `/api/v1`.

### Submit an SOS report (async intake)
```bash
curl -X POST http://localhost:8080/api/v1/sos \
  -H 'Content-Type: application/json' \
  -d '{"reporterId":"u1","description":"Someone collapsed and is bleeding","latitude":28.3635,"longitude":75.5870}'
# -> 202 Accepted  {"incidentId":"INC-1","status":"QUEUED","message":"SOS accepted for processing"}
```

### List all incidents
```bash
curl http://localhost:8080/api/v1/incidents
```

### Get one incident (with audit trail)
```bash
curl http://localhost:8080/api/v1/incidents/INC-1
```

### Transition an incident's lifecycle
`status` is the target state ∈ `ACKNOWLEDGED | RESPONDING | RESOLVED | ESCALATED`.
```bash
curl -X PATCH http://localhost:8080/api/v1/incidents/INC-1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"ACKNOWLEDGED"}'
# invalid transition -> 409, unknown status -> 400, unknown id -> 404
```

### List responders
```bash
curl http://localhost:8080/api/v1/responders
```

### Nearby responders (ranked by proximity, optional skill filter)
```bash
curl "http://localhost:8080/api/v1/responders/nearby?lat=28.3630&lng=75.5870&skill=medical"
```

### Update responder availability
```bash
curl -X PATCH http://localhost:8080/api/v1/responders/R4/status \
  -H 'Content-Type: application/json' \
  -d '{"available":false}'
```

### Report a user location ping
```bash
curl -X POST http://localhost:8080/api/v1/location \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1","latitude":28.3635,"longitude":75.5870}'
```

### Nearby upcoming events (concierge)
```bash
curl "http://localhost:8080/api/v1/events/nearby?lat=28.3635&lng=75.5870&tags=tech,coding"
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **State** | `state/*` driven by `Incident` | Each lifecycle state is its own class that knows its legal transitions; illegal moves throw `InvalidStateTransitionException`. Adding a state doesn't touch the others. |
| **Strategy** | `triage/TriageStrategy` with `RuleBasedTriageStrategy` / `AgenticTriageStrategy` | Triage algorithm is swappable at runtime via the `festops.triage.mode` flag without changing callers. |
| **Factory** | `factory/IncidentFactory` | Centralizes construction of the correct `Incident` subclass from an `IncidentType`. |
| **Observer** | `model/IncidentObserver` + `service/AuditLogObserver` | Incidents notify observers on every state change; the audit observer persists transitions to H2 — decoupled from the lifecycle logic. |
| **Producer–Consumer** | `service/FestOpsService` (`BlockingQueue` + `ExecutorService`) | Decouples API response time from processing: the controller enqueues and returns `202` immediately; a consumer thread triages/dispatches/stores. |
| **DAO** | `dao/AuditDAO`, `dao/ResponderCsvLoader` | Isolates persistence (JDBC, CSV) from business logic. |
| **DTO** | `controller/dto/*` | Decouples the wire format from domain entities, so internal state (lifecycle objects, observers) is never serialized. |

## Advanced Java features used

- **`java.util.concurrent`** — `LinkedBlockingQueue` for SOS intake, `ExecutorService` (single consumer thread), `ScheduledExecutorService` for the 30s SLA monitor, and `ConcurrentHashMap` for incident/responder/location registries. Chosen so the API thread, consumer thread, and scheduler can share state safely without blocking the request path.
- **`CompletableFuture` with `orTimeout`** — wraps the Anthropic API call in `AgenticTriageStrategy` with a hard 5-second timeout and graceful fallback, so an unreachable/slow LLM never stalls triage.
- **`java.net.http.HttpClient`** — non-blocking-capable HTTP client used both by the LLM triage strategy and the Swing dashboard (no third-party HTTP dependency).
- **`SwingWorker` + `javax.swing.Timer`** — the dashboard fetches off the Event Dispatch Thread and auto-refreshes every 5s, so the UI never freezes.
- **Records** — DTOs, `RankedEvent`, `UserLocation`, `SosReport` use records for concise, immutable carriers.
- **Switch expressions & text blocks** — used in the factory, severity colouring, and the LLM system prompt.
- **`volatile` + `synchronized`** — `Responder.available` is `volatile` and load mutations are synchronized so dispatch (consumer thread) and availability updates (HTTP threads) are memory-safe.
- **Regex `Pattern`/`Matcher`** — compiled, case-insensitive, priority-ordered keyword classification in `RuleBasedTriageStrategy`.
- **Try-with-resources** — JDBC connections/statements/result sets in `AuditDAO` and `BufferedReader` in the CSV loaders.

## OOP design — the incident inheritance hierarchy

The core of the domain is an **abstract `Incident` base class** with four concrete subclasses:

```
                       Incident (abstract)
                       ├─ id, description, lat/lng, severity, reportedAt
                       ├─ state machine (acknowledge/respond/resolve/escalate)
                       ├─ observer registration + notification
                       └─ abstract: getType(), defaultSeverity(),
                                    requiredSkill(), slaSeconds()
        ┌───────────────────┬───────────────┴───────┬───────────────────┐
 MedicalIncident     SecurityIncident          FireIncident       LogisticsIncident
 CRITICAL/medical    HIGH/security             CRITICAL/fire      LOW/maintenance
 SLA 120s            SLA 180s                  SLA 90s            SLA 600s
```

`Incident` holds everything common to all incidents — identity, location, the lifecycle state object, and the observer list — and implements the transition logic **once**. What differs per category is expressed through four **abstract methods** the subclasses must override:

- `getType()` — the `IncidentType` enum value
- `defaultSeverity()` — the `Severity` assigned at creation
- `requiredSkill()` — the responder skill the dispatcher matches against
- `slaSeconds()` — the SLA window the monitor enforces

This is **polymorphism in action**: the `Dispatcher`, `FestOpsService` SLA monitor, and `IncidentFactory` all operate on the `Incident` abstraction and call these methods without knowing the concrete type. Adding a new incident category (say, `WeatherIncident`) means writing one subclass and one factory case — no existing code changes. Severity is intentionally derived from the subclass rather than stored as free data, so a `FireIncident` is *always* CRITICAL by construction.

The lifecycle itself is **not** modeled with inheritance but with the **State pattern** (composition): an `Incident` *has-a* `IncidentState`, and delegates transitions to it. This keeps the "what kind of incident" axis (inheritance) cleanly separate from the "what stage is it in" axis (state objects).

## Testing

`mvn test` runs 28 JUnit 5 tests covering: every valid/invalid state transition, dispatcher scoring (skill, proximity, load) and the no-responder failure, rule-based triage classification + severity, and Haversine distances against known reference values.
Developed by Dhruv Malani and Yash Jain — CS F213, BITS Pilani, Summer 2026
Developed by Dhruv Malani and Yash Jain — CS F213, BITS Pilani, Summer 2026
