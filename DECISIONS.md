# Architecture Decision Records

## ADR-001: Database-first with Debezium CDC
**Decision:** MariaDB is the system of record. Debezium tails the binary log and produces domain events to Kafka.
**Rationale:** Eliminates the dual write problem at the source. Events are guaranteed to reflect committed state.
**Tradeoffs:** Adds operational complexity -- MariaDB must have binary logging enabled, Debezium must be running and healthy.

## ADR-002: Stream-first considered and rejected
**Decision:** Event sourcing / stream-first architecture was considered and rejected in favor of DB-first.
**Rationale:** Music catalog submission volumes don't justify the operational complexity of Kafka as a system of record. A well-indexed MariaDB handles the load trivially.
**Tradeoffs:** Dual write problem must be solved at the application layer where it arises.

## ADR-003: Validation service doubles as Reviewer API
**Decision:** The Kafka consumer and the REST API for the reviewer UI live in the same Spring Boot application.
**Rationale:** Both concern quality control. Splitting them would add operational overhead with no scaling benefit at this volume.
**Tradeoffs:** At scale, validation pipeline and reviewer API have different scaling characteristics and may need to be split.

## ADR-004: Redis caching strategy
**Decision:** Redis is not used for caching in this implementation.
**Rationale:** The reviewer UI is an internal tool used by a small team of QC reviewers. Read load does not justify a cache at reasonable submission volumes. A well-indexed MariaDB query is sufficient.
**Assumptions:** Moderate submission volume, small reviewer team. If volume grew significantly and DB read latency became a problem, Redis-backed caching of product status queries would be the first thing to reach for -- specifically caching by product ID with write-through invalidation driven by Debezium events.
**Redis in the broader platform:** Redis has valid use cases outside this service -- session management for the reviewer UI if not delegated to an identity provider, rate limiting at the API boundary, and caching for high-volume label-facing dashboard queries owned by other teams.
**Revisit when:** Label-facing status polling creates measurable DB read pressure, or reviewer team grows significantly.

## ADR-005: Single service for validation pipeline and reviewer API
**Decision:** The Kafka consumer, validation rule engine, and reviewer REST API live in a single Spring Boot application.
**Rationale:** Both concerns belong to the QC bounded context and share the same domain data. Splitting them would add operational overhead -- separate deployments, separate monitoring, inter-service communication -- with no scaling benefit at FUGA's likely volume.
**Tradeoffs:** Violates strict Single Responsibility Principle at the technical level. The validation pipeline and reviewer API have different scaling characteristics -- under high submission volume you might want to scale the consumer independently of the API. If volume grows significantly, splitting them would be the right move.
**Revisit when:** Submission volume requires consumer scaling that would over-provision the API tier, or vice versa.

## ADR-006: Product domain model based on music industry metadata standards
**Decision:** The Product model includes UPC, ISRC, contributor credits with roles, ownership splits, content references, DSP targets, release date, genre, explicit flag, and language.
**Rationale:** Music metadata is the backbone of the distribution chain and a primary source of industry-wide errors. Modelling it correctly from the start reduces downstream validation failures and royalty attribution errors. The three-layer abstraction of musical work, sound recording, and release informed the decision to include both ISRC (sound recording) and UPC (release) identifiers.
**Reference:** "How Broken Metadata Affects the Music Industry" -- Soundcharts, Dmitry Pastukhov (December 2023). https://soundcharts.com/blog/music-metadata
**Tradeoffs:** A richer domain model increases complexity. For this submission some fields are modelled but not deeply validated -- a production system would validate against external registries like ISRC and UPC databases.

## ADR-007: JSON columns for product list fields
**Decision:** Contributors, ownership splits, and DSP targets are stored as JSON columns in the products table rather than in separate normalized tables.
**Rationale:** The QC service reads the full product for validation -- it doesn't query individual contributors or splits in isolation. JSON columns keep the schema simple and avoid joins that add no value for this use case.
**Tradeoffs:** If FUGA were to extend this service to handle artist-level royalty accounting -- tracking payments to individual contributors rather than just to the label -- normalized tables would be strongly preferable. Querying "all products where Michael Jackson is a contributor" or "total ownership percentage for a given rights holder" is significantly harder with JSON columns. At that point a migration to normalized tables would be warranted.
**Revisit when:** The service needs to query or aggregate data at the contributor or rights holder level.

## ADR-008: Sequential rule execution
**Decision:** Rules are executed sequentially rather than in parallel.
**Rationale:** For in-memory rule checks, sequential execution has lower overhead than parallel streams. Thread management and result collection costs outweigh the benefits at this scale.
**Tradeoffs:** If rules make external I/O calls -- checking ISRC against an external registry, querying a copyright database -- parallel execution would significantly reduce latency. `parallelStream()` makes this a trivial change if needed.
**Revisit when:** Rule evaluation latency becomes measurable, or rules are introduced that make external calls.

## ADR-009: Rule engine configuration via Spring Factory
**Decision:** Universal and DSP-specific rules are wired into `RuleEngineImpl` via a `RuleEngineConfig` Spring `@Configuration` class rather than direct injection by type.
**Rationale:** Follows the Open/Closed Principle -- adding a new DSP rule group requires only a new class and a new entry in the config map. `RuleEngineImpl` never needs to change. The config class acts as a Factory, centralizing the assembly of the rule engine.
**Tradeoffs:** In a production system, DSP rule sets would likely be more dynamic -- loaded from a database or external configuration rather than hardcoded in a Spring config. The config class approach is appropriate for this submission but would need to evolve for a fully configurable rule registry.
**Revisit when:** DSP rule sets need to be managed by non-engineers or updated without a deployment.

## ADR-010: Dead Letter Queue routing with DefaultErrorHandler
**Decision:** Failed messages are routed to `product-dlq` via Spring Kafka's `DefaultErrorHandler` and `DeadLetterPublishingRecoverer`. `RuntimeException` is configured as non-retryable -- permanent failures go directly to DLQ without retrying.
**Rationale:** Two categories of failure need different handling. Transient failures (network blips, temporary DB unavailability) are worth retrying. Permanent failures (malformed events, invalid UUIDs, JSON parse errors) will never succeed and should fail fast to avoid blocking the consumer. Routing to a DLQ ensures no messages are permanently lost and can be inspected and replayed when the underlying issue is fixed.
**Tradeoffs:** The current configuration treats all `RuntimeException` as non-retryable which is broad. A production system would classify exceptions more precisely -- for example retrying `DataAccessException` but not `JsonProcessingException`. This is a known simplification for this submission.
**Revisit when:** Specific transient failure modes are identified that warrant retry before DLQ routing.
**Operational considerations:** In production, a dedicated DLQ consumer would monitor `product-dlq` and alert via Slack webhook when messages arrive, enabling the team to inspect and replay failed messages promptly. This is outside the scope of this submission but would be a first priority before going to production.

## ADR-011: Product CRUD API scoped to reviewer workflow
**Decision:** The product CRUD API (`POST /products`, `GET /products/{id}`, `GET /products`, `PATCH /products/{id}/status`, `DELETE /products/{id}`) is exposed via a single `ProductController`. The review decision endpoint is modeled as a status update (`PATCH /products/{id}/status`) rather than a separate reviews resource.
**Rationale:** In FUGA's domain, the reviewer UI is the primary consumer of product management operations -- reviewers need to inspect product details, correct metadata, and make disposition decisions. Modeling these as standard CRUD operations on the product resource rather than a separate reviews API keeps the surface area small and the resource model coherent. A product is the aggregate root; its status is an attribute of that resource, not a separate entity.
**Tradeoffs:** `GET /products` without pagination defaults to filtering by status rather than returning the full catalog. Returning an unbounded result set would be irresponsible without pagination, and pagination is outside the scope of this submission. This is a known limitation.
**Revisit when:** The API needs to serve clients beyond the reviewer UI -- for example a label-facing portal where labels manage their own submissions. At that point the access control model and resource scoping would need revisiting.
**Operational considerations:** In production, the API would require authentication and authorization -- reviewers should only see products assigned to them or their queue, and labels should only see their own products. This submission omits auth entirely as it is outside the stated scope.