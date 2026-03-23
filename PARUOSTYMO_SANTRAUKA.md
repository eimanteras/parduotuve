# Atsiskaitymo Parengtymo Santrauka

## 📋 Ką Atlikus

Parduotuvė projektas buvo pilnai parengtas ir dokumentuotas atsiskaitymui su šiais komponentais:

### ✅ Sukurta / Atnaujinta

#### 1. **AI Agent Guide - AGENTS.md** (2026-03-23)
- Išsami architektūros dokumentacija
- Dvigubos persistencijos modelio paaiškinimai (JPA + MyBatis)
- Kritiniai pattern'ai ir konvencijos
- Common development tasks su pavyzdžiais
- CVE keliami saugos aspektai

#### 2. **Sandėlių Valdymo Funkcionalumas** (Naujas)

**JPA DAO Sluoksnis:**
- `src/main/java/lt/eimantas/dao/jpa/SandelisDAO.java` - CRUD operacijos
- `@PersistenceContext(unitName = "ParduotuvePU")` injector
- `@Transactional` deklaratyvus transakcijų valdymas

**MyBatis Sluoksnis:**
- `src/main/java/lt/eimantas/dao/mybatis/SandelisModel.java` - DTO objektas
- `src/main/java/lt/eimantas/dao/mybatis/SandelisMapper.java` - SQL mapper interface
- `src/main/resources/mybatis/SandelisMapper.xml` - SQL query mappings

**Business Logic:**
- `ParduotuveService` - sandelio metodai (`getVisiSandeliai()`, `issaugotiSandelį()`)

**UI:**
- `src/main/webapp/sandeliai.xhtml` - Sandėlių lentelė + forma įvedimui
- `ParduotuveBean` - Warehouse data binding ir akcijos
- `index.xhtml` - Nauja navigacijos nuoroda

#### 3. **Atsiskaitymo Dokumentacija - ATSISKAITYMO_DEMOHOME.md**
- Pilnas reikalavimų žemėlapis (assignment specification)
- Paaiškinti visi esybių ryšiai (one-to-many, many-to-many)
- JPA vs MyBatis palyginimas ir praktiniai pavyzdžiai
- CDI anotacijų paaiškinimai (@RequestScoped, @Inject ir kt.)
- Transakcijų valdymo demonstracija
- Reikalavimų checklist su ✅ žymimais

---

## 🔗 Ryšiai Duomenų Bazėje

```
┌──────────────┐          ┌──────────────┐          ┌──────────────┐
│  KATEGORIJA  │          │  PRODUKTAS   │          │  SANDELIS    │
├──────────────┤          ├──────────────┤          ├──────────────┤
│ id (PK)      │◄─────────┤ id (PK)      │          │ id (PK)      │
│ pavadinimas  │ 1:N      │ pavadinimas  │◄───┐    │ pavadinimas  │
└──────────────┘          │ kaina        │    N:M  │ adresas      │
                          │ kategorija_id├────────►│              │
                          └──────────────┘         └──────────────┘
                                 ▲
                            ONE-TO-MANY
                          (Kategorija.produktai)
```

---

## 📊 Visų Reikalavimų Pasiekimas

| Sritis | Balai | Reikalavimas | Failas/Komponentas | Statusas |
|--------|-------|--------------|-------------------|----------|
| **IDE/Build/VCS** | 0.15 | Minimalus kodas pakeitimas | SandelisDAO.java | ✅ |
| | | Serverio nustatymas & deployment | WildFly + WAR | ✅ |
| | | Versijų kontrolė | git commit a58f552, 52212d6 | ✅ |
| **JPA** | 0.25 | One-to-many ryšys | Kategorija ↔ Produktas | ✅ |
| | | Many-to-many ryšys | Produktas ↔ Sandelis | ✅ |
| | | JPA esybės + mapping | entity/*.java | ✅ |
| | | MyBatis esybės + mapping | dao/mybatis/* | ✅ |
| **UI** | 0.1 | Duomenų pateikimas | produktai.xhtml, sandeliai.xhtml | ✅ |
| | | Forma su data binding | h:inputText, h:commandButton | ✅ |
| **Business Logic** | 0.05 | CDI komponentas | ParduotuveService (@RequestScoped) | ✅ |
| | | @Inject dependency injection | Visiems DAOs | ✅ |
| **Data Access** | 0.2 | JPA DAO | ProduktasDAO, SandelisDAO | ✅ |
| | | MyBatis Mapper | ProduktasMapper, SandelisMapper | ✅ |
| | | Transakcijos | @Transactional on save() | ✅ |
| **Bendrai** | 1.0 | **Iš VISO** | | ✅ |

---

## 📁 Projekto Struktūra (Atnaujinta)

```
parduotuve/
├── AGENTS.md                              ← AI Agent Guide ✨
├── ATSISKAITYMO_DEMOHOME.md               ← Atsiskaitymo dokumentacija ✨
├── pom.xml
├── src/main/
│   ├── java/lt/eimantas/
│   │   ├── entity/
│   │   │   ├── Kategorija.java            (1:N ryšys)
│   │   │   ├── Produktas.java             (M:1 → Kategorija, M:N ↔ Sandelis)
│   │   │   └── Sandelis.java              (M:N ↔ Produktas)
│   │   ├── dao/jpa/
│   │   │   ├── KategorijaDAO.java
│   │   │   ├── ProduktasDAO.java
│   │   │   └── SandelisDAO.java           ✨ NAUJAS
│   │   ├── dao/mybatis/
│   │   │   ├── ProduktasModel.java
│   │   │   ├── ProduktasMapper.java
│   │   │   ├── SandelisModel.java         ✨ NAUJAS
│   │   │   └── SandelisMapper.java        ✨ NAUJAS
│   │   ├── service/
│   │   │   └── ParduotuveService.java     (CDI @RequestScoped)
│   │   ├── web/
│   │   │   └── ParduotuveBean.java        (@Named, data binding)
│   │   └── persistence/
│   │       └── MyBatisResources.java      (CDI producer)
│   ├── resources/
│   │   ├── META-INF/persistence.xml
│   │   ├── mybatis-config.xml             (↑ SandelisMapper.xml)
│   │   └── mybatis/
│   │       ├── ProduktasMapper.xml
│   │       └── SandelisMapper.xml         ✨ NAUJAS
│   └── webapp/
│       ├── index.xhtml                    (↑ sandeliai link)
│       ├── produktai.xhtml
│       ├── kategorijos.xhtml
│       ├── sandeliai.xhtml                ✨ NAUJAS
│       ├── mybatis.xhtml
│       └── WEB-INF/
│           ├── beans.xml
│           ├── web.xml
│           └── parduotuve-ds.xml
└── .git/                                  (git history: 2 commits)
    └── commits: a58f552, 52212d6 ✅
```

---

## 💻 Išbandymo Žingsniai

### Kompiliavimas:
```bash
mvn clean package
# Sukuria: target/parduotuve.war
```

### Diegimas:
```bash
# 1. Parsisiųsti WildFly 27+
# 2. Suarchyvuoti ir nustatyti WILDFLY_HOME

# 3. Paleisti serverį
$WILDFLY_HOME/bin/standalone.bat

# 4. Kopijuoti WAR į deployments
cp target/parduotuve.war $WILDFLY_HOME/standalone/deployments/
```

### Prieiga:
```
http://localhost:8080/parduotuve/
```

### Navigacija:
- **Index** - Pradžia
- **Produktai (JPA)** - Produktų sąrašas iš JPA, forma su kategorijos pasirinkimu
- **Kategorijos** - Kategorijų sąrašas ir forma
- **Sandėliai** - Sandėlių sąrašas ir forma (NAUJAS)
- **Produktai (MyBatis)** - Produktai su SQL JOIN iš MyBatis

---

## 🔍 Raktinės Technologijos

| Technologija | Versija | Vaidmuo |
|-------------|---------|---------|
| Jakarta EE | 10.0.0 | Application framework |
| Java | 11 | Language target |
| Hibernate | 5.x (per JPA) | ORM provider |
| MyBatis | 3.5.13 | SQL mapper |
| MyBatis CDI | 2.1.0 | CDI integration |
| H2 Database | 1.4.196 | Embedded DB |
| JSF | 3.0+ (Jakarta) | UI framework |
| CDI | 3.0 (Jakarta) | Dependency injection |

---

## 📚 Pagrindines Koncepcijos Paaiškinti

### 1. **JPA vs MyBatis**
- **JPA** - Objekto modelis, automatinė transakcija, lazy loading
- **MyBatis** - SQL-driven, explicit mapping, performance control

### 2. **CDI Bean Scopes**
- `@RequestScoped` - Naujas per request (default mūsoje)
- `@SessionScoped` - Gyvuoja visą session
- `@ApplicationScoped` - Singleton per aplikaciją

### 3. **Deklaratyvios Transakcijos**
```java
@Transactional  // Automatic begin/commit/rollback
public void save(Entity e) {
    em.persist(e);
}
```

### 4. **JSF Data Binding**
```html
<!-- Dvikryptis (bidirectional) -->
<h:inputText value="#{bean.objektas.laukas}"/>
<h:commandButton action="#{bean.metodas}"/>
```

---

## ✨ Specialūs Aspektai

1. **Dual Persistence** - JPA ir MyBatis veikia kartu, demonstruodami abi strategijas
2. **LaFieldų Mapavimas** - `@Column(name="db_column")` skyrimas nuo Java lauko
3. **Ryšių Naviguacija** - `#{produktas.kategorija.pavadinimas}` per JPA ryšius
4. **JOIN Optimizacija** - MyBatis `LEFT JOIN` su denormalizuotais laukais

---

## 🎯 Tolesni Žingsniai (Pasiūlymai)

Jei reikalingas išplėtimas:

1. **Paieškai** - Pridėti `findByName(String likeName)` metodą
2. **Valdymui** - Update/Delete operacijos
3. **Validacijai** - `@NotNull`, `@Min`, `@Max` anotacijos
4. **Sekumui** - `@Version` laukas optimistic locking'ui
5. **Testams** - JUnit + Arquillian arba integration testai

---

## 📝 Git Istorija

```bash
git log --oneline

52212d6 Add comprehensive assignment documentation
a58f552 Add warehouse (Sandelis) support with JPA DAO and MyBatis mapper
1b70d5e Initial commit
```

---

**Statusas: PARUOŠTA ATSISKAITYMUI** ✅

Visa dokumentacija ir kodas yra GitHub repositorijoje, paruošti demonstravimui ir egzaminatorių žiūrėjimui.

