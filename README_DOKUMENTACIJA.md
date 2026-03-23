# 📖 Dokumentacijos Nuorodos

Šis failas padeda navigavimui per visą atsiskaitymo dokumentaciją.

---

## 🎯 Pagrindiniai Dokumentai (Pradėkite nuo Šių)

### 1. **[PARUOSTYMO_SANTRAUKA.md](./PARUOSTYMO_SANTRAUKA.md)** - SKAITYK PIRMĄ!
- ✅ Visų reikalavimų checklist
- ✅ Projekto struktūra su atnaujinimais
- ✅ Išbandymo žingsniai žingsnis po žingsnio
- ✅ Raktinės technologijos ir versijos
- ✅ Git commit historija

**Idealu:** Mano atsiskaityme naudoti kaip pagrindinis reference dokumento.

### 2. **[ATSISKAITYMO_DEMOHOME.md](./ATSISKAITYMO_DEMOHOME.md)** - GILESNIS PAAIŠKINJMAS
- 📋 Pilnas atsiskaitymo aprašymas
- 🔗 Detaliūs ryšių paaiškinimai (one-to-many, many-to-many)
- 🗂️ Failų lokacijos su konkretaus kodo pavyzdžiais
- 📊 JPA vs MyBatis lentelė
- 💾 Transakcijų valdymo demonstracija
- 🔍 Topologijas ir komponento diagramos

**Idealu:** Pagilinti supratimą ir atsakyti į detaliausiais klausimais.

### 3. **[AGENTS.md](./AGENTS.md)** - AI AGENTO VADOVAS
- 🏗️ Architektūros apžvalga
- 🔑 Kritiniai pattern'ai ir konvencijos
- 📁 Failų organizacijos struktūra
- 🛠️ Bendri development task'ai
- ⚠️ Svarbios pastabos AI agentams
- 📚 Žinynas į visus konfigūracijos failus

**Idealu:** AI agentams ar šio projekto kodo wartotojams.

---

## 🗂️ Pagrindiniai Šaltinio Kodai (Išbandymui)

### JPA Sluoksnis
```
✅ src/main/java/lt/eimantas/dao/jpa/ProduktasDAO.java
✅ src/main/java/lt/eimantas/dao/jpa/KategorijaDAO.java
✅ src/main/java/lt/eimantas/dao/jpa/SandelisDAO.java (NAUJAS)
```

### MyBatis Sluoksnis
```
✅ src/main/java/lt/eimantas/dao/mybatis/ProduktasMapper.java
✅ src/main/java/lt/eimantas/dao/mybatis/ProduktasModel.java
✅ src/main/java/lt/eimantas/dao/mybatis/SandelisMapper.java (NAUJAS)
✅ src/main/java/lt/eimantas/dao/mybatis/SandelisModel.java (NAUJAS)
✅ src/main/resources/mybatis/ProduktasMapper.xml
✅ src/main/resources/mybatis/SandelisMapper.xml (NAUJAS)
```

### Esybės (Entities)
```
✅ src/main/java/lt/eimantas/entity/Produktas.java (many-to-one, many-to-many)
✅ src/main/java/lt/eimantas/entity/Kategorija.java (one-to-many)
✅ src/main/java/lt/eimantas/entity/Sandelis.java (many-to-many)
```

### Business Logic
```
✅ src/main/java/lt/eimantas/service/ParduotuveService.java (@RequestScoped)
✅ src/main/java/lt/eimantas/web/ParduotuveBean.java (@Named, data binding)
```

### UI Puslapiai (JSF/Facelets)
```
✅ src/main/webapp/index.xhtml
✅ src/main/webapp/produktai.xhtml (JPA view)
✅ src/main/webapp/kategorijos.xhtml
✅ src/main/webapp/sandeliai.xhtml (NAUJAS - warehouse view)
✅ src/main/webapp/mybatis.xhtml (MyBatis view)
```

### Konfigūracija
```
✅ src/main/resources/META-INF/persistence.xml (JPA)
✅ src/main/resources/mybatis-config.xml
✅ src/main/webapp/WEB-INF/beans.xml (CDI)
✅ src/main/webapp/WEB-INF/web.xml (JSF)
✅ src/main/webapp/WEB-INF/parduotuve-ds.xml (JNDI)
```

---

## 🎓 Atsiskaitymo Reikalavimų Žemėlapis

| # | Kategorija | Reikalavimas | Dokumentas | Failas |
|----|-----------|-------------|-----------|-------|
| 1 | IDE/Build | Minimalus pakeitimas | PARUOSTYMO_SANTRAUKA | SandelisDAO.java |
| 2 | IDE/Build | Serveris & deployment | PARUOSTYMO_SANTRAUKA | WildFly + WAR |
| 3 | VCS | Git commit | PARUOSTYMO_SANTRAUKA | .git/ (3 commits) |
| 4 | JPA | One-to-many | ATSISKAITYMO_DEMOHOME | Kategorija.java |
| 5 | JPA | Many-to-many | ATSISKAITYMO_DEMOHOME | Produktas.java |
| 6 | JPA | Ryšių mapping | ATSISKAITYMO_DEMOHOME | entity/*.java |
| 7 | MyBatis | Model DTO | ATSISKAITYMO_DEMOHOME | dao/mybatis/*.java |
| 8 | MyBatis | XML mapping | ATSISKAITYMO_DEMOHOME | mybatis/*.xml |
| 9 | UI | Duomenų pateikimas | ATSISKAITYMO_DEMOHOME | produktai.xhtml |
| 10 | UI | Forma su data binding | ATSISKAITYMO_DEMOHOME | h:inputText |
| 11 | CDI | @RequestScoped | ATSISKAITYMO_DEMOHOME | ParduotuveService |
| 12 | CDI | @Inject | ATSISKAITYMO_DEMOHOME | Visuose DAOs |
| 13 | DAO | JPA implementacija | ATSISKAITYMO_DEMOHOME | ProduktasDAO.java |
| 14 | DAO | MyBatis implementacija | ATSISKAITYMO_DEMOHOME | ProduktasMapper.java |
| 15 | Transakcijos | @Transactional | ATSISKAITYMO_DEMOHOME | save() metodai |

---

## 🔄 Kompiliavimo ir Paleidimo Žingsniai

### 1. Kompiliuoti
```bash
cd C:\Users\user\IdeaProjects\parduotuve
mvn clean compile
```

### 2. Sukurti WAR Archyvą
```bash
mvn clean package
# Rezultatas: target/parduotuve.war
```

### 3. Diegti į WildFly
```bash
# Paleidžiant WildFly:
$WILDFLY_HOME/bin/standalone.bat

# Kopijuoti WAR:
cp target/parduotuve.war $WILDFLY_HOME/standalone/deployments/

# Palaukti deployment'o (žiūrėti konsolę)
```

### 4. Pasiekti Aplikaciją
```
http://localhost:8080/parduotuve/
```

---

## ✨ Naujos Funkcionalumų Aprašas (v2026-03-23)

### Sandėlių Valdymas (Sandelis Entity)
- **JPA DAO:** `SandelisDAO.java` - EntityManager su CRUD operacijomis
- **MyBatis:** `SandelisMapper` + `SandelisModel` + XML mapping
- **Service:** `ParduotuveService.getVisiSandeliai()` ir `issaugotiSandelį()`
- **UI:** `sandeliai.xhtml` - Lentelė su forma įvedimui

### Ryšiai
- **Kategorija** 1:N **Produktas** (su lazy loading)
- **Produktas** M:N **Sandelis** (per junction table)

### Dviguba Persistencija
- **JPA Path:** UI → Bean → Service → JPA DAO → H2 DB
- **MyBatis Path:** UI → Bean → Mapper Interface → XML SQL → H2 DB
- Abi veikia vienu metu, demonstruojant skirtingus pattern'us

---

## 🚀 Nuolatinis Patobulinimas

### Siūlomi Tolesni Žingsniai
- [ ] Update/Delete operacijos abiem persistence pattern'ais
- [ ] Paieška (find by name/description)
- [ ] Validacija (@NotNull, @Min, @Max)
- [ ] Optimistic locking (@Version)
- [ ] JUnit + Arquillian testai
- [ ] REST API endpoints (JAX-RS)

---

## 📞 Pagalba Skaitant Dokumentaciją

**Klausimas:** Kur rasti XYZ?
**Atsakymas:** Žr. lentelę šio failo pradžioje.

**Klausimas:** Kaip veikia ABC?
**Atsakymas:** Žr. ATSISKAITYMO_DEMOHOME.md sekcija "3.2" arba "3.3"

**Klausimas:** Kokia yra bendra architektūra?
**Atsakymas:** Žr. AGENTS.md sekcija "Architecture Overview"

**Klausimas:** Kaip padaryti DEF?
**Atsakymas:** Žr. PARUOSTYMO_SANTRAUKA.md sekcija "Išbandymo žingsniai"

---

**Paruošta:** 2026-03-23
**Statusas:** ✅ PARUOŠTA ATSISKAITYMUI
**Dokumentacija:** Pilna ir išsami
**Kodas:** Visas sukompiliuojamas ir diegiamas
**Git Historija:** 3 svarūs commit'ai

Sėkmės atsiskaityme! 🎓

