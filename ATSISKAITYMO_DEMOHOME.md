# Parduotuvė - Atsiskaitymo Demonstracija

## Atsiskaitymo Aprašymas

Šis dokumentas demonstruoja, kaip "Parduotuvė" projektas atitinka universiteto atsiskaitymo reikalavimus dėl Java EE, JPA, MyBatis, JSF ir CDI technologijų.

---

## 1. IDE, Build Tool, VCS Minimalus Ciklas (0.15 balų)

### 1.1 Minimalus Projekto Pakeitimas (0.05 balo)

**Atlikti pakeitimai:**
- Sukurtas naujas `SandelisDAO` failas: `src/main/java/lt/eimantas/dao/jpa/SandelisDAO.java`
- Šis failas įgyvendina CRUD operacijas sandėliams naudojant JPA
- Failas sukompiliuotas ir integruotas į projektą

**Kodas:**
```java
@RequestScoped
public class SandelisDAO {
    @PersistenceContext(unitName = "ParduotuvePU")
    private EntityManager em;
    
    @Transactional
    public void save(Sandelis s) {
        em.persist(s);
    }
}
```

### 1.2 Dalykinio Serverio Nustatymas ir Paleidimas (0.05 balo)

**Serverio Reikalavimas:** Jakarta EE 10 suderlus serveris (WildFly 27+, Payara 6.x)

**Nustatymas:**
- JNDI datasource: `ParduotuveDataSource` (apibrėžtas `WEB-INF/parduotuve-ds.xml`)
- H2 duomenų bazė: `jdbc:h2:~/h2database/ParduotuveDB`
- Konfigūracija `persistence.xml` su Hibernate dialektu

**Serverio Kontrolė:**
```bash
# WildFly paleidimas
$WILDFLY_HOME/bin/standalone.bat

# WAR diegimas
mvn clean package
# Nukopijuoti target/parduotuve.war į serverio deployments direktoriją

# Aplikacija pasiekiama: http://localhost:8080/parduotuve/
```

### 1.3 Versijų Kontrolė (0.05 balo)

**Git Istorija:**
```bash
# Žiūrėti commit'us
git log --oneline
# a58f552 Add warehouse (Sandelis) support...
# 1b70d5e Initial commit

# Patikrinti status
git status

# Pridėti pakeitimus
git add -A

# Commit'inti
git commit -m "Descriptive message"
```

**Įvykdytas Commit:**
- Timestamp: 2026-03-23
- Message: "Add warehouse (Sandelis) support with JPA DAO and MyBatis mapper"
- Failai: 10 pakeistų/sukurtų
- Additions: 299 eilutės

---

## 2. Duomenų Bazė, ORM/JPA ir DataMapper/MyBatis (0.25 balų)

### 2.1 Duomenų Bazės Modelis (0.05 balo)

**Lentelės Ryšiai:**

```
KATEGORIJA (1) ──────────── (*) PRODUKTAS
   id                            id
   pavadinimas                    pavadinimas
                                  kaina
                                  kategorija_id (FK)

PRODUKTAS (*) ──────────── (*) SANDELIS
   (per PRODUKTAS_SANDELIS jungtinę lentelę)
```

**H2 Konfigūracija:**
- Embedded duomenų bazė: `H2` v1.4.196
- DDL Mode: `update` (automatinis lentelių kūrimas)
- JNDI Binding: `ParduotuveDataSource`

---

### 2.2 JPA Esybės Paaiškinti (0.1 balo)

#### Produktas.java
```java
@Entity
@Table(name = "PRODUKTAS")
public class Produktas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // → PRODUKTAS.id (primary key)
    
    @Column(name = "pavadinimas", length = 100, nullable = false)
    private String pavadinimas;  // → PRODUKTAS.pavadinimas
    
    @Column(name = "kaina")
    private BigDecimal kaina;  // → PRODUKTAS.kaina
    
    @ManyToOne
    @JoinColumn(name = "kategorija_id")
    private Kategorija kategorija;  // → PRODUKTAS.kategorija_id (FK)
    
    @ManyToMany
    @JoinTable(
        name = "PRODUKTAS_SANDELIS",
        joinColumns = @JoinColumn(name = "produktas_id"),
        inverseJoinColumns = @JoinColumn(name = "sandelis_id")
    )
    private List<Sandelis> sandeliai;  // → jungtinė lentelė
}
```

**Ryšiai:**
- **Many-to-One:** `Produktas` → `Kategorija` (kiekvienas produktas priklauso vienai kategorijai)
- **Many-to-Many:** `Produktas` ↔ `Sandelis` (produktas gali būti keliuose sandėliuose)

#### Kategorija.java
```java
@Entity
@Table(name = "KATEGORIJA")
public class Kategorija {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pavadinimas", length = 100, nullable = false)
    private String pavadinimas;
    
    @OneToMany(mappedBy = "kategorija", fetch = FetchType.LAZY)
    private List<Produktas> produktai;  // Atvirkštinis ryšys
}
```

**Ryšys:**
- **One-to-Many:** `Kategorija` → `Produktas` (kiekviena kategorija turi daug produktų)

#### Sandelis.java
```java
@Entity
@Table(name = "SANDELIS")
public class Sandelis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pavadinimas", length = 100, nullable = false)
    private String pavadinimas;
    
    @Column(name = "adresas", length = 200)
    private String adresas;
    
    @ManyToMany(mappedBy = "sandeliai", fetch = FetchType.LAZY)
    private List<Produktas> produktai;  // Atvirkštinis many-to-many
}
```

**Lauko Pervadinimas:**
Jei norite lauko pavadinimą Java klasėje kitu nei stulpelio pavadinimas:
```java
@Column(name = "kategorija_id")  // DB stulpele
private Kategorija kategorija;    // Java lauke
```

---

### 2.3 MyBatis Esybės Paaiškinti (0.1 balo)

#### ProduktasModel.java (DTO)
```java
public class ProduktasModel {
    private Long id;
    private String pavadinimas;
    private BigDecimal kaina;
    private Long kategorijaId;           // Denormalizuotas laukas
    private String kategorijaPavadinimas; // JOIN rezultatas
}
```

#### ProduktasMapper.java (Interface)
```java
@Mapper
public interface ProduktasMapper {
    List<ProduktasModel> findAll();
    void insert(ProduktasModel produktas);
}
```

#### ProduktasMapper.xml (SQL Mapping)
```xml
<mapper namespace="lt.eimantas.dao.mybatis.ProduktasMapper">
    <resultMap id="ProduktasResultMap" type="lt.eimantas.dao.mybatis.ProduktasModel">
        <id property="id" column="id"/>
        <result property="pavadinimas" column="pavadinimas"/>
        <result property="kaina" column="kaina"/>
        <result property="kategorijaId" column="kategorija_id"/>
        <result property="kategorijaPavadinimas" column="kategorija_pavadinimas"/>
    </resultMap>

    <select id="findAll" resultMap="ProduktasResultMap">
        SELECT p.id, p.pavadinimas, p.kaina, p.kategorija_id,
               k.pavadinimas AS kategorija_pavadinimas
        FROM PRODUKTAS p
        LEFT JOIN KATEGORIJA k ON p.kategorija_id = k.id
    </select>
</mapper>
```

**Pagrindiniai Skirtumai:**
- MyBatis Model ≠ JPA Entity (atskiri objektai)
- MyBatis žemėlis tiesiai SQL į Java objektus
- SQL `LEFT JOIN` rezultatai mapuojami į denormalizuotus laukus
- MyBatis.xml faile lauko pavadinimas turėtų sutapti su Java modelio lauku

---

## 3. Panaudos Atvejai (0.6 balų)

### 3.1 UI - Duomenų Pateikimas ir Forma (0.2 balo)

#### produktai.xhtml - JPA Duomenys
```html
<h1>Produktų sąrašas (JPA)</h1>

<h:dataTable value="#{parduotuveBean.visiProduktai}" var="p" border="1">
    <h:column>
        <f:facet name="header">Kategorija</f:facet>
        #{p.kategorija.pavadinimas}  <!-- Naviguacija per many-to-one ryšį -->
    </h:column>
</h:dataTable>

<h2>Pridėti naują produktą</h2>
<h:form>
    <h:inputText value="#{parduotuveBean.naujasProduktas.pavadinimas}"/>
    <h:selectOneMenu value="#{parduotuveBean.naujasProduktas.kategorija}">
        <f:selectItems value="#{parduotuveBean.visiKategorijos}"
                       var="k" itemValue="#{k}" itemLabel="#{k.pavadinimas}"/>
    </h:selectOneMenu>
    <h:commandButton value="Išsaugoti" action="#{parduotuveBean.issaugotiProdukta}"/>
</h:form>
```

**Realizuotos Savybės:**
- ✅ Duomenų pateikimas iš DB (JPA)
- ✅ Kelių susijusių esybių duomenys (Produktas + Kategorija)
- ✅ Naviguacija per ryšius (p.kategorija.pavadinimas)
- ✅ Duomenų įvedimo forma
- ✅ Data binding (#{parduotuveBean.naujasProduktas})

#### kategorijos.xhtml - Kategorijų Valdymas
```html
<h2>Pridėti naują kategoriją</h2>
<h:form>
    <h:inputText value="#{parduotuveBean.naujaKategorija.pavadinimas}"/>
    <h:commandButton value="Išsaugoti" action="#{parduotuveBean.issaugotiKategorija}"/>
</h:form>
```

#### sandeliai.xhtml - Sandėlių Valdymas (NAUJAS)
```html
<h:dataTable value="#{parduotuveBean.visiSandeliai}" var="s" border="1">
    <h:column><f:facet name="header">Pavadinimas</f:facet>#{s.pavadinimas}</h:column>
    <h:column><f:facet name="header">Adresas</f:facet>#{s.adresas}</h:column>
</h:dataTable>

<h2>Pridėti naują sandėlį</h2>
<h:form>
    <h:inputText value="#{parduotuveBean.naujasSandelis.pavadinimas}"/>
    <h:inputText value="#{parduotuveBean.naujasSandelis.adresas}"/>
    <h:commandButton value="Išsaugoti" action="#{parduotuveBean.issaugotiSandelį}"/>
</h:form>
```

#### mybatis.xhtml - MyBatis Duomenys
```html
<h1>Produktų sąrašas (MyBatis)</h1>
<h:dataTable value="#{parduotuveBean.myBatisProduktai}" var="p" border="1">
    <h:column><f:facet name="header">Kategorija</f:facet>#{p.kategorijaPavadinimas}</h:column>
</h:dataTable>
```

---

### 3.2 Dalykinio Funkcionalumo Komponentas (0.05 balo)

#### ParduotuveService.java
```java
@RequestScoped  // Valdomas CDI, vienas instancas per HTTP request
public class ParduotuveService {
    @Inject
    private ProduktasDAO produktasDAO;
    
    @Inject
    private SandelisDAO sandelisDAO;
    
    public List<Produktas> getVisiProduktai() {
        return produktasDAO.findAll();
    }
    
    public void issaugotiProdukta(Produktas p) {
        produktasDAO.save(p);
    }
    
    public List<Sandelis> getVisiSandeliai() {
        return sandelisDAO.findAll();
    }
    
    public void issaugotiSandelį(Sandelis s) {
        sandelisDAO.save(s);
    }
}
```

**CDI Anotacijos Paaiškinti:**
- `@RequestScoped` - Klasė virsta CDI komponentu, egzistuoja vieno HTTP request'o metu
- `@Inject` - CDI priklausomybės injection (automatinis konstruktavimas)
- `@SessionScoped` - Komponentas gyvuoja viso HTTP sesijos metu
- `@ApplicationScoped` - Komponentas gyvuoja visos aplikacijos metu

---

### 3.3 Duomenų Prieigos Komponentas (0.2 balo)

#### JPA DAO Implementacija (0.1 balo)
```java
@RequestScoped
public class ProduktasDAO {
    @PersistenceContext(unitName = "ParduotuvePU")
    private EntityManager em;
    
    public List<Produktas> findAll() {
        return em.createQuery("SELECT p FROM Produktas p", Produktas.class).getResultList();
    }
    
    @Transactional
    public void save(Produktas p) {
        em.persist(p);  // Deklaratyvus transakcijų valdymas
    }
}
```

**JPA Privalumai:**
- Objekto-reliacinis žemėlis (ORM)
- Automatinės ryšių valdymas (lazy loading, cascade)
- Type-safe queries (JPQL)
- Transakcijų automatizacija

#### MyBatis Mapper Implementacija (0.1 balo)
```java
@Mapper
public interface ProduktasMapper {
    List<ProduktasModel> findAll();
    void insert(ProduktasModel produktas);
}

<!-- ProduktasMapper.xml -->
<select id="findAll" resultMap="ProduktasResultMap">
    SELECT p.id, p.pavadinimas, p.kaina, p.kategorija_id,
           k.pavadinimas AS kategorija_pavadinimas
    FROM PRODUKTAS p
    LEFT JOIN KATEGORIJA k ON p.kategorija_id = k.id
</select>
```

**MyBatis Privalumai:**
- SQL-centric (pilna SQL kontrolė)
- Žemi runtime overhead'ai
- Denormalizuotus duomenys (JOIN optimizacija)
- Granuliar performance control

**ORM vs DataMapper Skirtumai:**

| Aspektas | ORM (JPA) | DataMapper (MyBatis) |
|----------|-----------|----------------------|
| Abstraktymas | Aukštas - objektai | Žemas - SQL |
| Ryšiai | Automatiniai | Rankinis mapping |
| Performance | Geras, lazy loading | Optimizuotas SQL |
| Learning Curve | Sudėtingiau | Paprastiau SQL | 
| Kiekis Kodo | Mažiau | Daugiau XML |
| Kada Naudoti | CRUD operacijos | Sudėtingos queries |

---

### 3.4 Deklaratyvios Transakcijos (0.05 balo)

#### Transakcijų Kontrolė
```java
@Transactional  // Deklaratyvus - automatinės begin/commit
public void save(Produktas p) {
    em.persist(p);
    // Jei error - rollback; jei OK - commit
}
```

**Transakcijų Savybės:**
- No manual `begin()` / `commit()` - automatinis valdymas
- Exception handling - automatiški rollback'ai
- JTA integration - tarpserverio transakcijos (jei reikia)

---

### 3.5 Web Bean su Injekcijomis (0.05 balo)

#### ParduotuveBean.java
```java
@Named  // Pasiekiamas JSF views kaip #{parduotuveBean}
@RequestScoped
public class ParduotuveBean {
    @Inject
    private ParduotuveService service;  // Automatinė injekcija
    
    @Inject
    private ProduktasMapper produktasMapper;  // MyBatis mapper
    
    private Produktas naujasProduktas = new Produktas();  // Data binding
    
    public String issaugotiProdukta() {
        service.issaugotiProdukta(naujasProduktas);
        return "produktai?faces-redirect=true";
    }
}
```

**JSF Binding:**
```html
<!-- Data binding: dvikryptis ryšys (bidirectional) -->
<h:inputText value="#{parduotuveBean.naujasProduktas.pavadinimas}"/>

<!-- Aktyvinimas: Submit forma ir iškviesti metodą -->
<h:commandButton value="Išsaugoti" action="#{parduotuveBean.issaugotiProdukta}"/>
```

---

## 4. Techninio Turinio Santrauka

### Komponento Topologija
```
UI (JSF/Facelets)
    ↓ #{parduotuveBean.method}
Web Bean (@Named @RequestScoped)
    ↓ @Inject ParduotuveService
Service (@RequestScoped)
    ↓ @Inject DAO
Data Access Layer
    ├─ JPA DAO (@PersistenceContext EntityManager)
    │  └─ persistence.xml → H2 DB
    └─ MyBatis Mapper (@Mapper)
       └─ mybatis-config.xml → H2 DB
```

### Failų Struktūra
```
parduotuve/
├── AGENTS.md (← AI Agent Guide)
├── pom.xml
├── src/main/
│   ├── java/lt/eimantas/
│   │   ├── entity/ (Produktas, Kategorija, Sandelis)
│   │   ├── dao/
│   │   │   ├── jpa/ (ProduktasDAO, KategorijaDAO, SandelisDAO)
│   │   │   └── mybatis/ (ProduktasMapper, SandelisMapper models)
│   │   ├── service/ (ParduotuveService)
│   │   ├── web/ (ParduotuveBean)
│   │   └── persistence/ (MyBatisResources CDI producer)
│   ├── resources/
│   │   ├── META-INF/persistence.xml (JPA config)
│   │   ├── mybatis-config.xml
│   │   └── mybatis/ (ProduktasMapper.xml, SandelisMapper.xml)
│   └── webapp/
│       ├── index.xhtml
│       ├── produktai.xhtml (JPA view)
│       ├── kategorijos.xhtml
│       ├── sandeliai.xhtml (NEW - warehouse view)
│       ├── mybatis.xhtml (MyBatis view)
│       └── WEB-INF/
│           ├── beans.xml (CDI enablement)
│           ├── web.xml (JSF servlet config)
│           └── parduotuve-ds.xml (JNDI datasource)
```

---

## 5. Testuojamos Iteracijos

Norėdami išbandyti aplikaciją:

1. **Kompiliuoti:** `mvn clean package`
2. **Diegti:** WAR į WildFly `/deployments` direktoriją
3. **Pasiekti:** `http://localhost:8080/parduotuve/`
4. **Bandyti:**
   - Navigacija: Index → Produktai → Kategorijos → Sandėliai → MyBatis
   - Duomenų įvedimas: Pridėti produktą (forma)
   - Duomenų rodžiauslas: Lentelės su DB duomenimis
   - Ryšiai: Produktų lentelėje rodymas kategorijos pavadinimo

---

## 6. Reikalavimų Checklist

| Reikalavimas | Įvykdyta | Failas/Lokacija |
|--------------|---------|-----------------|
| Minimalus IDE pakeitimas | ✅ | SandelisDAO.java |
| Serveris & deployment | ✅ | WildFly + WAR |
| VCS commit | ✅ | git commit a58f552 |
| One-to-many ryšys | ✅ | Kategorija.produktai |
| Many-to-many ryšys | ✅ | Produktas.sandeliai |
| JPA esybės paaiškinti | ✅ | entity/*.java |
| MyBatis paaiškinti | ✅ | dao/mybatis/* |
| Duomenų pateikimas (UI) | ✅ | produktai.xhtml, sandeliai.xhtml |
| Forma su data binding | ✅ | h:inputText + h:commandButton |
| CDI komponentas (@RequestScoped) | ✅ | ParduotuveService |
| @Inject dependency injection | ✅ | Visuose DAOs ir Service |
| JPA DAO su @Transactional | ✅ | ProduktasDAO.save() |
| MyBatis Mapper | ✅ | ProduktasMapper, SandelisMapper |
| Deklaratyvios transakcijos | ✅ | @Transactional on save() |
| Dual persistence pattern | ✅ | JPA + MyBatis side-by-side |

---

**Atsiskaitymas Sukomplektas:** ✅ Visos sritys apdengtios ir demonstruojamos.

