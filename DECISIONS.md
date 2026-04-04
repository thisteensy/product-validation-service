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
**Decision:** The Kafka consumer, validation rule engine, Product API, and Review API live in a single Spring Boot application.
**Rationale:** Consolidating these components into one service was a pragmatic choice for this assessment. It reduces operational overhead for the reviewer -- a single deployment, a single `docker compose up`, a single application to run and understand.
**In production:** These are distinct concerns that would likely live in separate services. The validation pipeline (consumer + rule engine) is a backend processing concern with different scaling characteristics than the label-facing Product API or the internal reviewer-facing Review API. In a production FUGA platform, these would be strong candidates for separate services with their own deployment boundaries, monitoring, and scaling policies.
**Tradeoffs:** The single service boundary makes independent scaling impossible without a refactor. The controllers are already separated by concern, so extracting them into separate services would be a natural next step.
**Revisit when:** This assessment becomes a production system.

## ADR-004: Redis caching strategy
**Decision:** Redis is not used for caching in this implementation.
**Rationale:** The reviewer UI is an internal tool used by a small team of QC reviewers. Read load does not justify a cache at reasonable submission volumes. A well-indexed MariaDB query is sufficient.
**Assumptions:** Moderate submission volume, small reviewer team. If volume grew significantly and DB read latency became a problem, Redis-backed caching of product status queries would be the first thing to reach for -- specifically caching by product ID with write-through invalidation driven by Debezium events.
**Redis in the broader platform:** Redis has valid use cases outside this service -- session management for the reviewer UI if not delegated to an identity provider, rate limiting at the API boundary, and caching for high-volume label-facing dashboard queries owned by other teams.
**Revisit when:** Label-facing status polling creates measurable DB read pressure, or reviewer team grows significantly.

## ADR-005: Product domain model based on music industry metadata standards
**Decision:** The Product model includes UPC, ISRC, contributor credits with roles, ownership splits, content references, DSP targets, release date, genre, explicit flag, and language.
**Rationale:** Music metadata is the backbone of the distribution chain and a primary source of industry-wide errors. Modelling it correctly from the start reduces downstream validation failures and royalty attribution errors. The three-layer abstraction of musical work, sound recording, and release informed the decision to include both ISRC (sound recording) and UPC (release) identifiers.
**Reference:** "How Broken Metadata Affects the Music Industry" -- Soundcharts, Dmitry Pastukhov (December 2023). https://soundcharts.com/blog/music-metadata
**Tradeoffs:** A richer domain model increases complexity. For this submission some fields are modelled but not deeply validated -- a production system would validate against external registries like ISRC and UPC databases.

## ADR-006: JSON columns for product list fields
**Decision:** Contributors, ownership splits, and DSP targets are stored as JSON columns in the products table rather than in separate normalized tables.
**Rationale:** The QC service reads the full product for validation -- it doesn't query individual contributors or splits in isolation. JSON columns keep the schema simple and avoid joins that add no value for this use case.
**Tradeoffs:** If FUGA were to extend this service to handle artist-level royalty accounting -- tracking payments to individual contributors rather than just to the label -- normalized tables would be strongly preferable. Querying "all products where Michael Jackson is a contributor" or "total ownership percentage for a given rights holder" is significantly harder with JSON columns. At that point a migration to normalized tables would be warranted.
**Revisit when:** The service needs to query or aggregate data at the contributor or rights holder level.

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

## ADR-010: Separate controllers for label and reviewer workflows
**Decision:** The product API (`POST /products`, `GET /products/{id}`, `GET /products`, `PUT /products/{id}`, `POST /products/{id}/resubmit`, `DELETE /products/{id}`) is served by `ProductController`. The reviewer API (`GET /reviews/pending`, `PATCH /reviews/{id}/decision`) is served by a separate `ReviewController`.
**Rationale:** The Label UI and Reviewer UI are distinct clients with different workflows, different access patterns, and different URL namespaces. Serving them through a single controller conflates two separate concerns -- product lifecycle management and human review disposition -- that happen to share the same domain data. Separating them makes each controller's responsibility legible, makes the URL structure self-documenting, and makes each controller independently testable.
**Tradeoffs:** Two controllers instead of one adds a small amount of structural overhead. Both controllers depend on the same `ProductRepository`, which means they share a data access layer -- this is intentional and appropriate since they operate on the same aggregate root.
**Revisit when:** The label and reviewer workflows diverge significantly enough to warrant separate services, or access control requirements differ enough between the two clients to warrant separate deployment boundaries.
**Operational considerations:** In production, both APIs would require authentication. The reviewer API would be restricted to internal FUGA staff. The label API would be restricted to authenticated label accounts with access scoped to their own catalog.

## ADR-011: Domain-layer structural validation with boundary sanitization
**Decision:** Structural validation (ISRC format, title presence, ownership split integrity) lives in the domain service's pre-flight checks rather than exclusively at the API boundary. Sanitization (normalization of whitespace, casing, formatting) happens at both boundaries -- the REST controller and the Kafka consumer mapper -- before the domain receives input.
**Rationale:** The domain cannot trust that all entry points have validated correctly. With multiple boundaries (REST API, Kafka consumer, and potentially future entry points), pushing all validation exclusively to the boundary risks invalid state reaching the domain if a new entry point is added without the correct checks. This is a belt-and-suspenders approach informed by financial systems engineering, where the cost of invalid domain state is high relative to the cost of redundant checks.
**Tradeoffs:** Duplicates some checking between the boundary and the domain. The "parse, don't validate" school of thought would argue sanitization and structural checks belong exclusively at the boundary, with the type system carrying proof of validity downstream. That approach is cleaner in languages with expressive type systems (Haskell, Rust) but harder to enforce in Java without significant ceremony.
**Revisit when:** A dedicated DTO layer with Bean Validation is introduced at the REST boundary, at which point the domain pre-flight checks could be reconsidered.

## ADR-012: Explicit resubmission as a separate action
**Decision:** Label resubmission is a two-step process. The label first corrects their product data via `PUT /products/{id}`, then explicitly triggers resubmission via `POST /products/{id}/resubmit`. The resubmit endpoint is a no-op if the product is not in `VALIDATION_FAILED` status.
**Rationale:** Implicit resubmission -- where saving updated data automatically triggers the validation pipeline -- creates subtle failure modes. A label may save partial corrections multiple times before they are satisfied. Firing validation on incomplete data wastes pipeline capacity and produces noise in the reviewer queue. Making resubmission an explicit conscious action gives the label control over when they are ready, and gives the system a clean, unambiguous trigger for the `RESUBMITTED` status transition.
**Tradeoffs:** Requires the label UI to surface two distinct actions -- save and resubmit -- which adds a small amount of UI complexity. This is preferable to the alternative of silent, unintended validation triggers.
**Status guard:** The resubmit endpoint rejects requests with a `400` if the product is not in `VALIDATION_FAILED` status. This guard exists at both the controller and repository layers -- the controller returns a clean error to the client, the repository guard is a safety net against resubmission being triggered from other entry points in the future.
**Revisit when:** The resubmission workflow needs to support partial corrections across multiple sessions, at which point a draft status and explicit submission flow would be worth considering.