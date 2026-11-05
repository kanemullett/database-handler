# database-handler

A Spring Boot library for working with PostgreSQL through jOOQ, using
Immutables-style domain records. Add it as a dependency and it sets up the
datasource, runs your Flyway migrations, and provides
`DatabaseQueryService` and `DatabaseTableService` for building and running
queries.

## Installation

The library is published to GitHub Packages:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/kanemullett/database-handler</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.kanemullett</groupId>
  <artifactId>database-handler</artifactId>
  <version>3.0.0</version>
</dependency>
```

The PostgreSQL JDBC driver, Flyway and jOOQ come with the library, so you
don't need to add them yourself.

## What your application must provide

### 1. Connection details

The datasource is built from these environment variables. Any other Spring
property source also works, e.g. system properties.

| Variable            | Required | Default  |
|---------------------|----------|----------|
| `POSTGRES_HOST`     | yes      |          |
| `POSTGRES_PORT`     | no       | `5432`   |
| `POSTGRES_DB`       | yes      |          |
| `POSTGRES_USER`     | yes      |          |
| `POSTGRES_PASSWORD` | yes      |          |
| `POSTGRES_SSLMODE`  | no       | `prefer` |

These produce:

```properties
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT:5432}/${POSTGRES_DB}?sslmode=${POSTGRES_SSLMODE:prefer}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
```

### 2. Flyway migrations

Put your migrations in `src/main/resources/db/migration` (that is,
`classpath:db/migration`), named using Flyway's convention, e.g.
`V1__create_schema.sql`. They run automatically on startup, before jOOQ's
`DSLContext` is available.

Baseline-on-migrate is **off**. If the target schema already has tables but
no `flyway_schema_history` table, startup fails with a Flyway error. With it
on, Flyway would have silently skipped `V1`. To adopt an existing database,
baseline it on purpose (for example, set `spring.flyway.baseline-on-migrate=true`
and `spring.flyway.baseline-version=...` once) and then remove that setting.

### 3. PostgreSQL

Only PostgreSQL is supported. jOOQ's dialect is fixed to `POSTGRES`.
Collection, map and nested-object properties on records are stored as
`jsonb`, so declare those columns as `jsonb`.

## Overriding defaults

The library's defaults live in `database-handler.properties` and are loaded
with the lowest precedence. Any single setting can be overridden in your own
`application.properties` (or env vars, etc.) without losing the others. For
example:

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.flyway.locations=classpath:db/migration,classpath:db/seed
```

## Upgrading from 2.x

3.0.0 contains breaking changes:

- **Environment variables renamed:** `DB_URL`, `DB_USERNAME` and
  `DB_PASSWORD` are no longer read. Set the `POSTGRES_*` variables above
  instead.
- **Migrations now actually run on startup.** In 2.x, the custom Flyway bean
  stopped Spring Boot from running migrations.
- **Baseline-on-migrate is off.** See [Flyway migrations](#2-flyway-migrations).
- **No more `application.properties` in the jar.** The library used to ship
  one, which could clash with yours. Its settings now come from
  `database-handler.properties`.

3.0.0 also includes these fixes:

- **Enums:** string columns map onto enum properties (an exact name match is
  preferred, otherwise a case-insensitive one).
- **`jsonb`:** `List`/`Map`/nested-object properties are written as `jsonb`
  and read back using their full generic type.
- **Null columns:** a null column no longer causes an NPE. It leaves the
  property at its `@Nullable`/`@Default` value and is left out of `data`.
- **UPDATEs:** unset properties are left unchanged rather than set to `NULL`.
- **Writing dates and times:** `java.time` values are bound as proper
  temporal types, and are stored as ISO-8601 strings when nested inside
  `jsonb`.
- **Reading dates and times:** `Timestamp`, `java.sql.Date` and `Time` columns
  are converted to `LocalDateTime`/`Instant`, `LocalDate` and `LocalTime`.

## Building

```bash
mvn test
```

The auto-configuration tests use Testcontainers to start PostgreSQL, so they
need Docker running.
