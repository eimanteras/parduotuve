# Parduotuvė - AI Agent Guide

A Jakarta EE web application demonstrating dual persistence approaches (JPA & MyBatis) for managing a product catalog with categories and warehouses.

## Architecture Overview

**Key Insight:** This project intentionally maintains two parallel data access patterns:
- **JPA Path** (`dao/jpa/` → Service → Web Bean → UI) for traditional entity management
- **MyBatis Path** (`dao/mybatis/` → Mapper Interface → Web Bean) for SQL-centric queries

Both paths coexist in `ParduotuveBean` to demonstrate different persistence patterns. When adding features, respect this duality.

### Component Topology

```
UI Layer (JSF/Facelets)
├── produktai.xhtml, kategorijos.xhtml (data tables via #{parduotuveBean})
└── mybatis.xhtml (separate MyBatis query display)
       ↓
Web Bean (@Named/@RequestScoped)
├── ParduotuveBean (coordinates JPA-based & MyBatis operations)
       ↓
Service Layer (@RequestScoped)
└── ParduotuveService (JPA-only business logic)
       ↓
Data Access (Two Parallel Routes)
├── JPA DAOs: ProduktasDAO, KategorijaDAO
│   └── Uses EntityManager with persistence unit "ParduotuvePU"
│   └── Configured in META-INF/persistence.xml
└── MyBatis Mappers: ProduktasMapper (interface)
    └── Backed by mybatis/ProduktasMapper.xml (SQL mappings)
    └── Configured via MyBatisResources producer & mybatis-config.xml
       ↓
Database
└── H2 in-memory (datasource: ParduotuveDataSource in parduotuve-ds.xml)
```

## Language & Version Notes

- **Java 11 target** (maven.compiler.source/target in pom.xml)
- **Jakarta EE 10** (not javax - uses jakarta.* imports)
- **Lombok 1.18.10** for getters/setters (note: entities don't use @Data, only manual getters/setters)
- Project name suggests Lithuanian domain (product names use pavadinimas, etc.)

## Critical Patterns & Conventions

### Entity-DAO Relationship
- **Entities** (`Kategorija.java`, `Produktas.java`) use `@Entity @Table` with Jakarta Persistence
- **DAOs** inject `@PersistenceContext(unitName = "ParduotuvePU")` EntityManager
- **Key Detail:** Save methods require `@Transactional` annotation for persist operations
- **Relationship Example:** `Produktas.kategorija` is `@ManyToOne`; `Kategorija.produktai` is `@OneToMany(mappedBy="kategorija", fetch=FetchType.LAZY)`

### MyBatis Model Separation
- `ProduktasModel` (separate DTO in `dao/mybatis/`) ≠ `Produktas` entity
- ProduktasModel includes denormalized fields: `kategorijaId` + `kategorijaPavadinimas` (for join result binding)
- `ProduktasMapper.xml` maps SQL result sets to ProduktasModel properties; no automatic camelCase conversion
- **Pattern:** When adding MyBatis queries, create corresponding Model class and resultMap in XML

### Dependency Injection
- All DAOs and service use `@Inject` (CDI)
- Bean scopes: `@RequestScoped` for DAOs, Service, and Web Bean
- MyBatis mapper auto-injection via `@Mapper` annotation (from mybatis-cdi)

### Transaction Management
- JPA: `@Transactional` on write operations (persist, merge, remove)
- MyBatis: No explicit @Transactional needed for reads; inserts in mapper XML are auto-transacted via CDI-MyBatis integration

## File Organization

- **`src/main/java/lt/eimantas/`**
  - `entity/` — Jakarta Persistence entities (Produktas, Kategorija, Sandelis)
  - `dao/jpa/` — EntityManager-based DAOs (CRUD per entity)
  - `dao/mybatis/` — Mapper interfaces & Model DTOs
  - `service/` — ParduotuveService orchestrates JPA DAOs
  - `web/` — JSF backing bean (ParduotuveBean) - **single point of UI coordination**
  - `persistence/` — MyBatisResources producer (CDI factory for SqlSessionFactory)
- **`src/main/resources/`**
  - `META-INF/persistence.xml` — JPA unit definition (Hibernate dialect, H2, DDL mode=update)
  - `mybatis-config.xml` — MyBatis root config; registers ProduktasMapper.xml
  - `mybatis/ProduktasMapper.xml` — SQL queries & result mappings

## Build & Deployment

**Build Command:** `mvn clean package`
- Creates WAR artifact: `target/parduotuve.war`
- Packaging type is `war` (web archive)
- **No integration tests in pom.xml** — testing is manual via deployment

**Server Requirement:** Jakarta EE 10 compatible application server (e.g., WildFly 27+, Payara 6.x)
- Must provide JTA transaction manager
- H2 datasource bound to JNDI name `ParduotuveDataSource` (see `WEB-INF/parduotuve-ds.xml`)

## Common Development Tasks

### Adding a New Entity with JPA Persistence
1. Create Entity class in `entity/` with `@Entity @Table` annotations
2. Create DAO in `dao/jpa/` with `@PersistenceContext` EntityManager and CRUD methods
3. Inject DAO in `ParduotuveService` and add service method
4. Inject service in `ParduotuveBean` and expose getter/action method for UI
5. Update JSF view to bind to bean method

### Adding a MyBatis Query
1. Create Model DTO in `dao/mybatis/` matching SQL result structure
2. Add method signature to `ProduktasMapper` interface
3. Add `<select>` or `<insert>` in `mybatis/ProduktasMapper.xml` with matching namespace & ID
4. Ensure resultMap property names match Model field names
5. Inject `ProduktasMapper` in `ParduotuveBean` and call method
6. Bind result to JSF view (e.g., `getMyBatisProduktai()`)

### Debugging Data Flow
- **SQL Logging:** `persistence.xml` has `hibernate.show_sql=true` — check server logs
- **MyBatis Logging:** Enable org.mybatis loggers at DEBUG level
- **CDI Injection Issues:** Verify `beans.xml` exists in `WEB-INF/` (enables CDI in WAR)

## Key Dependencies & Compatibility

| Dependency | Version | Purpose | Notes |
|---|---|---|---|
| jakarta.jakartaee-api | 10.0.0 | Core EE APIs | Provided scope (app server supplies) |
| h2 | 1.4.196 | In-memory database | Old version; consider upgrading for security |
| mybatis | 3.5.13 | SQL mapping framework | Not automatically transacted; handled by mybatis-cdi |
| mybatis-cdi | 2.1.0 | CDI integration for MyBatis | Enables @Mapper injection & auto-txn |
| lombok | 1.18.10 | Code generation (provided scope) | Entities don't use it; optional for future POJOs |

## Important Notes for AI Agents

- **Dual Persistence is Intentional:** Don't refactor to use only one pattern—the project teaches both approaches
- **Manual Getters/Setters in Entities:** Entities use explicit getters/setters (not @Data from Lombok) for clarity
- **JSF EL Binding:** Web views use `#{parduotuveBean.method}` syntax; ensure bean methods match exactly
- **No Tests in Source:** Testing is deployed & manual; focus on build-time compilation safety
- **Lithuanian Field Names:** Entity & DAO methods use Lithuanian names (`pavadinimas` = name, `kaina` = price); keep consistency
- **MyBatis XML-First:** Queries are defined in XML, not annotations—edit `.xml` files when changing queries, not Model classes

## Related Configuration Files

- `src/main/webapp/WEB-INF/web.xml` — Servlet mappings (JSF FacesServlet)
- `src/main/webapp/WEB-INF/parduotuve-ds.xml` — JNDI datasource definition (H2)
- `src/main/webapp/WEB-INF/beans.xml` — CDI enablement (required for @Inject)
- `src/main/webapp/index.xhtml` — Entry point

