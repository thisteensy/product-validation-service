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