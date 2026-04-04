# Architecture Decision Records

## ADR-001: Database-first with Debezium CDC
**Decision:** MariaDB is the system of record. Debezium tails the binary log and produces domain events to Kafka.
**Rationale:** Eliminates the dual write problem at the source. Events are guaranteed to reflect committed state.
**Tradeoffs:** Adds operational complexity -- MariaDB must have binary logging enabled, Debezium must be running and healthy.

## ADR-002: Stream-first considered and rejected
**Decision:** Event sourcing / stream-first architecture was considered and rejected in favor of DB-first.
**Rationale:** Music catalog submission volumes don't justify the operational complexity of Kafka as a system of record. A well-indexed MariaDB handles the load trivially.
**Tradeoffs:** Dual write problem must be solved at the application layer where it arises.

## ADR-003: Single service for assessment purposes
**Decision:** The catalog API and all validation consumers live in a single Spring Boot application. This includes the Product Validation Consumer, Track Validation Consumer, and Product Status Consumer, as well as downstream stubs for notification, DSP delivery, and reviewer routing.
**Rationale:** Consolidating these components into one service was a pragmatic choice for this assessment. It reduces operational overhead for the reviewer -- a single deployment, a single `docker compose up`, a single application to run and understand.
**In production:** These are distinct concerns that would live in separate services. The product catalog API, the validation pipeline, and the DSP rule engine are three different bounded contexts with different scaling characteristics, different deployment cadences, and different teams. In a production FUGA platform, each would have its own service, its own deployment boundary, and its own monitoring.
**Tradeoffs:** The single service boundary makes independent scaling impossible without a refactor. The consumers are already separated by concern, so extracting them into separate services would be a natural next step.
**Revisit when:** This assessment becomes a production system.

## ADR-004: Redis caching strategy
**Decision:** Redis is not used for caching in this implementation.
**Rationale:** Read load on the catalog API does not justify a cache at reasonable submission volumes. A well-indexed MariaDB query is sufficient.
**Assumptions:** Moderate submission volume. If volume grew significantly and DB read latency became a problem, Redis-backed caching of product status queries would be the first thing to reach for -- specifically caching by product ID with write-through invalidation driven by Debezium events.
**Redis in the broader platform:** Redis has valid use cases outside this service -- rate limiting at the API boundary, and caching for high-volume label-facing dashboard queries owned by other teams.
**Revisit when:** Label-facing status polling creates measurable DB read pressure.

## ADR-005: Product domain model based on music industry metadata standards
**Decision:** The domain is modeled around two distinct concepts -- `Product` (the release) and `Track` (the recording). A `Product` carries release-level metadata: UPC, title, release date, genre, language, artwork URI, DSP targets, and ownership splits. A `Track` carries recording-level metadata: ISRC, title, track number, audio file URI, duration, explicit flag, contributor credits, and ownership splits. The `explicit` flag on `Product` is derived -- a release is explicit if any of its tracks are explicit.
**Rationale:** Music metadata is the backbone of the distribution chain. The three-layer abstraction of musical work, sound recording, and release informed the separation of product and track concerns. ISRC identifies a specific sound recording -- it belongs on the track. UPC identifies the release -- it belongs on the product. Contributor credits and ownership splits exist at both levels in the industry, but for this submission they live at the track level where they are most operationally significant for royalty attribution.
**Reference:** "How Broken Metadata Affects the Music Industry" -- Soundcharts, Dmitry Pastukhov (December 2023). https://soundcharts.com/blog/music-metadata
**Tradeoffs:** Ownership splits at the release level are common in simpler distribution models. Moving them entirely to the track level is more accurate but requires labels to specify splits per track, which adds friction for simple single-track releases. A production system might support both release-level and track-level splits with track-level taking precedence.
**Revisit when:** The service needs to support release-level ownership splits as a shorthand for uniform track-level splits.

## ADR-006: JSON columns for list fields
**Decision:** Contributors, ownership splits, and DSP targets are stored as JSON columns rather than in separate normalized tables. Contributors and ownership splits live on the `tracks` table. DSP targets live on the `products` table.
**Rationale:** The validation consumer reads the full product and its tracks for validation -- it doesn't query individual contributors or splits in isolation. JSON columns keep the schema simple and avoid joins that add no value for this use case. The move of contributors and ownership splits to the track level reflects the industry model -- royalty attribution is track-level, not release-level.
**Tradeoffs:** If FUGA were to extend this service to handle artist-level royalty accounting -- tracking payments to individual contributors across their full catalog -- normalized tables would be strongly preferable. Querying "all tracks where Michael Jackson is a contributor" or "total ownership percentage for a given rights holder across all releases" is significantly harder with JSON columns. At that point a migration to normalized tables would be warranted.
**Revisit when:** The service needs to query or aggregate data at the contributor or rights holder level, or when royalty accounting moves into this bounded context.

## ADR-007: Sequential rule execution
**Decision:** Rules are executed sequentially rather than in parallel.
**Rationale:** For in-memory rule checks, sequential execution has lower overhead than parallel streams. Thread management and result collection costs outweigh the benefits at this scale.
**Tradeoffs:** If rules make external I/O calls -- checking ISRC against an external registry, querying a copyright database -- parallel execution would significantly reduce latency. `parallelStream()` makes this a trivial change if needed.
**Revisit when:** Rule evaluation latency becomes measurable, or rules are introduced that make external calls.

## ADR-008: Rule engine configuration via Spring Factory
**Decision:** Universal and DSP-specific rules are wired into `RuleEngineImpl` via a `RuleEngineConfig` Spring `@Configuration` class rather than direct injection by type.
**Rationale:** Follows the Open/Closed Principle -- adding a new DSP rule group requires only a new class and a new entry in the config map. `RuleEngineImpl` never needs to change. The config class acts as a Factory, centralizing the assembly of the rule engine.
**Tradeoffs:** In a production system, DSP rule sets would likely be more dynamic -- loaded from a database or external configuration rather than hardcoded in a Spring config. The config class approach is appropriate for this submission but would need to evolve for a fully configurable rule registry.
**Revisit when:** DSP rule sets need to be managed by non-engineers or updated without a deployment.

## ADR-009: Dead Letter Queue routing with DefaultErrorHandler
**Decision:** Failed messages are routed to `product-dlq` via Spring Kafka's `DefaultErrorHandler` and `DeadLetterPublishingRecoverer`. `RuntimeException` is configured as non-retryable -- permanent failures go directly to DLQ without retrying.
**Rationale:** Two categories of failure need different handling. Transient failures (network blips, temporary DB unavailability) are worth retrying. Permanent failures (malformed events, invalid UUIDs, JSON parse errors) will never succeed and should fail fast to avoid blocking the consumer. Routing to a DLQ ensures no messages are permanently lost and can be inspected and replayed when the underlying issue is fixed.
**Tradeoffs:** The current configuration treats all `RuntimeException` as non-retryable which is broad. A production system would classify exceptions more precisely -- for example retrying `DataAccessException` but not `JsonProcessingException`. This is a known simplification for this submission.
**Revisit when:** Specific transient failure modes are identified that warrant retry before DLQ routing.
**Operational considerations:** In production, a dedicated DLQ consumer would monitor `product-dlq` and alert via Slack webhook when messages arrive, enabling the team to inspect and replay failed messages promptly. This is outside the scope of this submission but would be a first priority before going to production.

## ADR-010: Single Product API controller
**Decision:** All catalog operations (`POST /products`, `GET /products/{id}`, `GET /products`, `PUT /products/{id}`, `POST /products/{id}/resubmit`, `DELETE /products/{id}`) are served by a single `ProductController`. The human review workflow is represented by a `ReviewerStub` downstream consumer rather than a REST API.
**Rationale:** The catalog API serves one client -- labels. A reviewer workflow in production would be a separate internal service with its own API, authentication, and deployment boundary. Modeling it as a stub consumer accurately reflects that separation of concerns without building a second service within this submission.
**Tradeoffs:** The reviewer stub is minimal -- it logs and does nothing. A production reviewer workflow would require a full service with its own persistence, assignment logic, and notification system.
**Revisit when:** The reviewer workflow needs to be built out as a real service.
**Operational considerations:** In production, the Product API would require authentication via an identity provider (Okta), with label accounts scoped to their own catalog entries.

## ADR-011: Boundary sanitization and structural validation
**Decision:** Sanitization (normalization of whitespace, casing, formatting) happens at both entry boundaries before the domain receives input -- in `ProductEventMapper` for Kafka events and in `ProductMapper` for REST requests. Structural validation of REST input is handled by Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Pattern`) on `ProductParams` at the controller boundary.
**Rationale:** Sanitization is part of parsing raw input into a domain object -- it happens inline during mapping, not as a separate pass. Bean Validation handles malformed REST requests early and returns clean 400 responses before the domain is involved. This is informed by the "parse, don't validate" principle described by Alexis King (https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/), applied pragmatically within Java's type system constraints.
**Tradeoffs:** Java's type system cannot enforce at compile time that a `Product` has been sanitized before reaching the domain, unlike a language with more expressive types (Haskell, Rust). The separation between boundary sanitization and domain validation is enforced by convention rather than the compiler. Bean Validation cannot express business-rule constraints such as valid status transitions -- those are enforced by the domain model via `Product.transitionTo()`, with a global exception handler translating `IllegalStateException` to HTTP 400 responses.
**Revisit when:** A dedicated value object or smart constructor approach is introduced so that invalid or unsanitized `Product` instances cannot be constructed -- at that point the boundary validation layer could be reconsidered.

## ADR-012: Explicit resubmission as a separate action
**Decision:** Label resubmission is a two-step process. The label first corrects their product data via `PUT /products/{id}`, then explicitly triggers resubmission via `POST /products/{id}/resubmit`. The resubmit endpoint is a no-op if the product is not in `VALIDATION_FAILED` status.
**Rationale:** Implicit resubmission -- where saving updated data automatically triggers the validation pipeline -- creates subtle failure modes. A label may save partial corrections multiple times before they are satisfied. Firing validation on incomplete data wastes pipeline capacity and produces noise in the reviewer queue. Making resubmission an explicit conscious action gives the label control over when they are ready, and gives the system a clean, unambiguous trigger for the `RESUBMITTED` status transition.
**Tradeoffs:** Requires the label UI to surface two distinct actions -- save and resubmit -- which adds a small amount of UI complexity. This is preferable to the alternative of silent, unintended validation triggers.
**Status guard:** The resubmit endpoint enforces the transition via `Product.transitionTo()` in the repository layer. Invalid transitions throw `IllegalStateException`, which is caught by the global exception handler and returned as a 400. This is consistent with how all status transitions are enforced across the system.
**Revisit when:** The resubmission workflow needs to support partial corrections across multiple sessions, at which point a draft status and explicit submission flow would be worth considering.

## ADR-013: Status transition enforcement on the domain model
**Decision:** Valid status transitions are enforced by `Product.transitionTo(ProductStatus)`. The method consults an allowlist of valid next states for the current status and throws `IllegalStateException` if the transition is invalid.
**Rationale:** Status transition rules are domain invariants -- they belong on the aggregate root, not in controllers or repository implementations. Centralizing transition logic on `Product` means the rules are enforced regardless of which entry point triggers the transition. A global `@RestControllerAdvice` translates `IllegalStateException` to HTTP 400 responses at the API boundary.
**Valid transitions:**
- `SUBMITTED`, `RESUBMITTED` → `VALIDATED`, `VALIDATION_FAILED`, `NEEDS_REVIEW`
- `VALIDATION_FAILED` → `RESUBMITTED`
- `NEEDS_REVIEW` → `VALIDATED`, `VALIDATION_FAILED`
- `VALIDATED` → `PUBLISHED`
- `PUBLISHED` → `TAKEN_DOWN`
- `TAKEN_DOWN` → `PUBLISHED`, `RETIRED`
- `RETIRED` → terminal, no further transitions
  **Tradeoffs:** Enforcing transitions in the domain requires loading the product from the database before every status update, adding one read per write. This is acceptable at catalog scale.
  **Revisit when:** Transition rules become more complex or context-dependent, at which point a formal state machine library (Spring Statemachine) would be worth considering.

## ADR-014: QC validation logic in infrastructure layer
**Decision:** Rule engine, rule results, validation results, and rule severity live in `infrastructure/rules`, not in `domain/`. The Kafka consumer depends on the rule engine directly.
**Rationale:** QC validation is a consumer concern, not a catalog concern. The catalog domain knows only that a product has a status -- it has no knowledge of how that status was determined. Placing validation logic in the infrastructure layer reflects this boundary accurately. The domain remains pure: `Product`, `ProductStatus`, contributors, ownership splits.
**Tradeoffs:** The rule engine is an infrastructure concern that happens to write back to the catalog via the repository. This creates a dependency from infrastructure to domain, which is the correct direction in hexagonal architecture.
**Revisit when:** The validation consumer is extracted into its own service, at which point the rule engine and its supporting types would move with it entirely.

## ADR-015: Status transition history tracking
**Decision:** Every call to `Product.transitionTo()` results in a row written to `product_status_history` via `ProductStatusHistoryRepository`. Each row captures the product ID, previous status, new status, actor type (`SYSTEM`, `REVIEWER`, `LABEL`), optional actor identity, and timestamp.
**Rationale:** Status history is critical for the resubmission workflow -- ops teams need to see why a product failed validation and what changed between submissions. Without history, the only visible state is the current status. History also provides a natural audit trail for compliance and debugging.
**Actor identity:** The `changed_by_type` distinguishes between system-driven transitions (validation consumer) and human-driven transitions (label resubmission, reviewer decisions). The `changed_by_id` field is nullable -- currently null for all transitions since authentication is not implemented. When an identity provider is introduced, this field will carry the authenticated user or label account ID.
**Tradeoffs:** Every status update now requires two writes -- the status update and the history row. These are not wrapped in an explicit transaction in this submission, which means a failure between the two writes could leave the history incomplete. In production, both writes should be wrapped in a single transaction.
**Revisit when:** Authentication is introduced, at which point `changed_by_id` should be populated from the security context. Also revisit to wrap both writes in a transaction.

## ADR-016: Track as a first-class domain concept
**Decision:** `Track` is a separate domain entity with its own identity, status lifecycle, and validation pipeline. A `Product` contains one or more `Track` instances. Track-level fields -- ISRC, audio file URI, duration, explicit flag, contributor credits, and ownership splits -- live on `Track`, not on `Product`.
**Rationale:** A music release is a container for recordings. ISRC identifies a specific recording, not a release -- attaching it to the product was a simplification that broke down as soon as album support was considered. Separating track and product concerns allows each to be validated independently, failed independently, and resubmitted independently. A label can fix one failing track without resubmitting the entire release.
**Tradeoffs:** Track-level modeling adds complexity -- a separate `tracks` table, a separate validation consumer, a separate status lifecycle, and a product status derivation step. For single-track releases this is more ceremony than necessary. A production system might optimize the single-track case while still supporting the full track model.
**Revisit when:** The service needs to support track-level publishing, takedowns, or retirements independently of the parent release -- which is a natural next step given that DSPs manage tracks individually.

## ADR-017: Track-level validation pipeline with intermediate product state
**Decision:** Validation is split into two independent pipelines -- product-level and track-level. The `ProductValidationConsumer` validates release-level fields and writes `AWAITING_TRACK_VALIDATION` on pass or `VALIDATION_FAILED` on failure. The `TrackValidationConsumer` validates each track independently and writes the track's status. The `ProductStatusConsumer` listens for track status changes and recomputes the product status from all track statuses once no tracks remain in `PENDING`.
**Rationale:** Validating tracks independently means a single failing track does not block the entire release from being inspectable. A label can see exactly which tracks failed and why, fix only those tracks, and resubmit them individually. The `AWAITING_TRACK_VALIDATION` intermediate state makes the product lifecycle legible -- a product in this state has passed release-level checks and is waiting for its tracks to complete validation.
**Tradeoffs:** Three consumers instead of one adds operational complexity. The product status derivation step requires loading all tracks from the database on every track status change, which adds read overhead at scale. The current implementation is DB-backed rather than stream-backed -- see ADR-019 for the production direction.
**Status guard:** The `ProductStatusConsumer` skips recomputation if any track is still `PENDING`, if the product is in a terminal state (`PUBLISHED`, `TAKEN_DOWN`, `RETIRED`), or if the recomputed status matches the current status. This prevents unnecessary writes and avoids premature status transitions.
**Revisit when:** Track validation volume requires the product status derivation to be moved out of the DB and into a stream-backed state store -- see ADR-019.

## ADR-018: DSP rule orchestration
**Decision:** DSP-specific validation rules are encapsulated in classes implementing `DspRuleSet`, which exposes both `evaluateProduct` and `evaluateTrack` methods. A `DspOrchestrator` takes a list of `DspRuleSet` implementations, filters by the product's DSP targets, and calls the appropriate method. `ProductRules` and `TrackRules` delegate to the orchestrator after running their universal checks. Adding a new DSP requires only a new class implementing `DspRuleSet`.
**Rationale:** DSP requirements differ significantly -- Spotify has different metadata requirements than Apple Music, Amazon Music, or Tidal. Keeping DSP-specific rules isolated means a change to Spotify requirements doesn't touch Apple Music logic, and adding a new DSP doesn't require modifying existing rule classes. The orchestrator pattern keeps the consumers and universal rules ignorant of which DSPs exist.
**Honest assessment:** DSP rule management is its own bounded context. In a production FUGA platform it would be a separate service with its own data store for rule configuration, its own API for rule management, and its own team. The current implementation collapses it into the catalog service as an assessment constraint. The `DspRuleSet` interface and `DspOrchestrator` are designed to make extraction straightforward when that time comes.
**Tradeoffs:** DSP rules that require external I/O -- checking against a rights registry, validating against a DSP's API -- are not supported in the current sequential execution model. Adding external calls would require moving to parallel execution and handling partial failures per DSP.
**Revisit when:** DSP rule sets need to be managed by non-engineers, updated without a deployment, or loaded from an external configuration store -- at which point extraction into a dedicated service is warranted.

## ADR-019: DB-backed product status derivation (compacted topic deferred)
**Decision:** Product status is derived from track statuses by loading all tracks from the database after each track status change. This is implemented in `ProductStatusConsumer` which reads current state from MariaDB rather than maintaining a stream-backed state store.
**Rationale:** The DB-backed approach is simpler to implement and reason about for an assessment context. It requires no additional infrastructure and works correctly given the existing Debezium CDC setup.
**Production direction:** The correct architecture for this problem is a compacted Kafka topic keyed by `product_id`. Each message represents the current aggregated validation state of a product -- product-level result plus a map of track statuses. When all tracks pass and the product is valid, a tombstone record cleans up the entry. This is a standard Kafka Streams pattern for exactly this kind of aggregation problem. It eliminates the DB read overhead on every track status change and makes the validation state visible as a first-class stream rather than a derived DB query.
**Tradeoffs:** The current DB-backed approach adds one `findByProductId` read per track status change. At catalog scale this is negligible. At streaming scale with millions of tracks it would become a bottleneck. The compacted topic approach scales horizontally and avoids DB reads entirely for status derivation.
**Revisit when:** Track validation volume makes the per-event DB read measurable, or when the team has the operational maturity to run Kafka Streams in production.