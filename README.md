# database-handler

A Spring Boot library for working with PostgreSQL through jOOQ, using
Immutables-style domain records. Add it as a dependency and it sets up the
datasource, runs your Flyway migrations, creates any tables you describe in
JSON, and provides `DatabaseQueryService`, `DatabaseTableService` and
`DatabaseSchemaService` for building and running queries. These beans are
auto-configured, so you don't need to component-scan `com.kanemullett`, and
you can replace any of them by defining your own bean of the same type.

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

Flyway keeps its `flyway_schema_history` table in the `public` schema
(`spring.flyway.default-schema=public`). This matters because Postgres puts
`"$user"` first in its default `search_path`. Without this setting, if a
migration created a schema with the same name as the database user, Flyway
would look for its history in that schema on the next start and fail.

### 3. PostgreSQL

Only PostgreSQL is supported. jOOQ's dialect is fixed to `POSTGRES`.
Collection, map and nested-object properties on records are stored as
`jsonb`, so declare those columns as `jsonb`.

## Creating tables from JSON definitions

This is optional. Put one JSON file per table in
`src/main/resources/definitions/tables/` (subfolders are fine). On startup,
each table is created if it doesn't already exist:

```json
{
  "schema": "app",
  "table": "users",
  "columns": [
    { "name": "id", "dataType": "VARCHAR", "primaryKey": true },
    { "name": "email", "dataType": "VARCHAR" },
    { "name": "created", "dataType": "TIMESTAMP_WITHOUT_TIME_ZONE" },
    { "name": "settings", "dataType": "JSONB", "isNullable": true }
  ]
}
```

- `dataType` is one of `VARCHAR`, `INTEGER`, `BOOLEAN`,
  `TIMESTAMP_WITHOUT_TIME_ZONE` or `JSONB`.
- `primaryKey` defaults to `false`. Mark more than one column to get a
  composite key.
- `isNullable` defaults to `false`, so columns are `NOT NULL` unless you set
  `"isNullable": true`. Primary key columns are always `NOT NULL`.
- Your application works within a single schema, so every file should use
  the same `schema`. It is created if it doesn't exist.
- Names are quoted, so they are case-sensitive. Stick to lowercase.

How it behaves:

- **Runs after Flyway.** Migrations run first, so a migration can't rely on a
  table that's defined in JSON.
- **Only creates, never alters.** Tables are created with
  `CREATE TABLE IF NOT EXISTS`, so editing the JSON for a table that already
  exists does nothing. Change existing tables with a Flyway migration.
- **Fails fast.** Every file is read before anything is created. A file that
  isn't valid JSON, isn't an object, or has an unknown field value such as a
  misspelled `dataType` stops startup with an error naming the file, and no
  tables are created.

To switch it off, for example in tests or where the database user isn't
allowed to run DDL:

```properties
database-handler.tables.enabled=false
```

or `DATABASE_HANDLER_TABLES_ENABLED=false` as an environment variable.

## Overriding defaults

The library's defaults live in `database-handler.properties` and are loaded
with the lowest precedence. Any single setting can be overridden in your own
`application.properties` (or env vars, etc.) without losing the others. For
example:

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.flyway.locations=classpath:db/migration,classpath:db/seed
```

## What's new in 3.1.0

- **Tables from JSON definitions.** See
  [Creating tables from JSON definitions](#creating-tables-from-json-definitions).
- **Column nullability.** `ColumnDefinition` has an `isNullable` flag, which
  defaults to `false`. **This changes `DatabaseTableService.createTable`:**
  columns used to be created nullable, and are now `NOT NULL` unless you
  build them with `.isNullable(true)`. Tables that already exist are not
  affected.
- **`DatabaseSchemaService`** with `createSchema` and `deleteSchema`.
  `deleteSchema(schema, false)` refuses to drop a schema that isn't empty;
  `deleteSchema(schema, true)` drops it and everything in it.
- **Beans are auto-configured.** The services, builder functions and record
  mapper used to be plain `@Service`/`@Component` classes, so you only got
  them if your application happened to component-scan `com.kanemullett`. They
  are now registered by auto-configuration, and each one backs off if you
  define your own.

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
