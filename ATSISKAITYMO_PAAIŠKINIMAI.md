# Atsiskaitymo Paaiškinimai — Dažnos Dėstytojo Klausai

## 1. Ar Naudoti Lombok Atsiskaitymui? (Ne/Taip — Abi Gerai)

### Atsakymas:
**Atsiskaitymui Lombok naudojimas NĖRA reikalingas ir NĖRA privalus.** Projektas paruoštas su rankiniu `getter`/`setter` dėl šio tikslo:

```java
// Dabartinis paruošymas (TEISINGAI):
@Entity
@Table(name = "KATEGORIJA")
public class Kategorija {
    private Long id;
    
    public Long getId() { return id; }              // Rankinis getter
    public void setId(Long id) { this.id = id; }   // Rankinis setter
}

// Alternatyva su Lombok (IRGI TEISINGAI, bet nereikalinga):
@Entity
@Table(name = "KATEGORIJA")
@Getter
@Setter
public class Kategorija {
    private Long id;  // Lombok sugeneruoja getter/setter compilTime
}
```

### Kodėl Rankinis Paruošymas?
- **Edukacinis tikslas:** matote, kas tikrai vyksta, nėra "black box"
- **Kontrolė:** žinote, kada getter/setter iškviečiami
- **JPA ryšiai:** lengviau suprasti `@ManyToOne`, `@OneToMany`, `@ManyToMany` nustatymus
- **Atsiskaitymui:** nėra jokio skirtumo — dėstytojas testos **logika**, ne kodo generavimą

### Dėstytojui Atsakyti:
> "Naudojau rankinius getter'ius ir setter'ius, nes norėjau suprasti, kaip JPA entity laukai surišti su DB stulpeliais. Lombok gali sugeneruoti juos automatiškai, bet projekto tikslas buvo matyti šiuos ryšius tiesiogiai."

---

## 2. `issaugotiSandelis` — Kodėl ASCII, ne Lietuviškas?

### Atsakymas:
**Kodas naudoja ASCII raidės (be lietuviškų: ė, š, č, ž, ą).**

```java
// TEISINGAI KODE (ASCII tik):
public void issaugotiSandelis(Sandelis s) {
    sandelisDAO.save(s);
}
// Metodas naudoja ASCII — `issaugotiSandelis` (ne `issaugotiSandelį`)
```

### Kodėl ASCII Kode?
- **Suderlumas:** visose sistemose ir serverių koduavuose ASCII raidės veikia jeigu
- **IDE/Git/Deploy:** lietuviškos raidės gali sukelti koduavimo problemas
- **Best practice:** Java jie apibrėžtos visuotinai — ASCII metodų vardai

### Dokumentacija vs Kodas:
- **Dokumentacija** — gali naudoti lietuviškas raides (ė, š, č, etc.) 
- **Kodas** — tik ASCII (a-z, A-Z, 0-9, _, $)

### Dėstytojui Atsakyti:
> "Kodas naudoja ASCII raidės (`issaugotiSandelis`), bet dokumentacija ir aprašymai gali turėti lietuviškas raides. Tai standartinis šiuolaikinis Java kodo praktika — metodų vardai ir kintamieji ASCII, tačiau komentarai ir dokumentacija gali būti šalies kalba."

---

### Kitų Metodų Pavyzdžiai (ASCII):
- `issaugotiProdukta` ✅ (ne `issaugotiProduktą`)
- `issaugotiKategorija` ✅ (ne `issaugotiKategorijĄ`)
- `issaugotiSandelis` ✅ (ne `issaugotiSandelį`)
- `naujasSandelis` ✅ (ne `naujasSandelis`)
- `visiSandeliai` ✅ (ne `visiSandėliai`)

---

## 3. IDE Warning: "Unsatisfied Dependency" — MyBatis Mapper

### Problema:
```
Warning:(25, 29) Unsatisfied dependency: no bean matches the injection point
private ProduktasMapper produktasMapper;  // <-- IDE raudona linija
```

### Atsakymas:

**IDE klaidą rodo, bet runtime viskas veikia.** Priežastis:

```java
// ParduotuveBean.java
@Inject
private ProduktasMapper produktasMapper;  // IDE nežino, kad yra CDI bean!
```

```java
// ProduktasMapper.java
@Mapper  // ← MyBatis CDI integration anotacija
public interface ProduktasMapper {
    List<ProduktasModel> findAll();
}
```

### Kaip veikia:

1. **CompilTime:** IDE žiūri `@Inject` + `ProduktasMapper` — IDE nežino, kas tai sukūrės
2. **Runtime:** MyBatis CDI extension (`mybatis-cdi-2.1.0.jar`) **automatiškai** sukuria `ProduktasMapper` bean per `@Mapper`
3. **CDI konteineris:** randa bean, susierga injection

IDE negali statinio analizatoriaus per sutikti CDI extension'u, nes tai dinamiškas runtime mechanizmas.

### Konfigūracija:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-cdi</artifactId>
    <version>2.1.0</version>
</dependency>
```

```xml
<!-- src/main/resources/mybatis-config.xml -->
<configuration>
    <mappers>
        <mapper resource="mybatis/ProduktasMapper.xml"/>
    </mappers>
</configuration>
```

```xml
<!-- src/main/webapp/WEB-INF/beans.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://xmlns.jcp.org/xml/ns/javaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee 
       http://xmlns.jcp.org/xml/ns/javaee/beans_2_0.xsd"
       bean-discovery-mode="all">
    <!-- CDI enabled — MyBatis extension'ai matyti -->
</beans>
```

### Dėstytojui Atsakyti:

> "IDE rodo 'Unsatisfied dependency', nes nežino apie MyBatis CDI integration. Realybėje, `@Mapper` anotacija MyBatis'ą patalpina CDI konteineryje runtime'e. IDE statinis analizatorius negali patirti CDI extension'u, todėl rodo warning'ą, bet aplikacija veikia. Tai yra pažįstamas IDE limitation su CDI integracijom."

#### Arba techniškiau:

> "CDI managed bean'ai gali būti kuriami:
> 1. **Standartiniai** (@Named, @RequestScoped, etc.) — IDE mato
> 2. **Per extension'us** (@Mapper, @Producer) — IDE negali garantuoti
> 
> MyBatis Mapper'iai kuriami per CDI extension, todėl IDE rodo warning'ą. Application Server CDI konteineris to negali — runtime viskas veikia."

---

## 4. Lombok Anotacijos Warnings — Projekto Sąmoningas Pasirinkimas

### Problema:
```
Warning:(8, 14) Class 'Kategorija' may use Lombok @Getter
Warning:(8, 14) Class 'Kategorija' may use Lombok @Setter
```

### Atsakymas:

**IDE siūlo Lombok, bet projektas sąmoningai nenaudoja.** Priežastis:

```java
// Projekto konvencija — rankinis kodas:
public class Kategorija {
    private Long id;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // Aiški, perskaityma, nėra "magic"
}
```

vs

```java
// Lombok — kompaktiška, bet "black box":
@Getter
@Setter
public class Kategorija {
    private Long id;
    // Lombok compilTime sugeneruoja getter'ius/setter'ius
}
```

### Kada Lombok Gera:
- ✅ Didelius projetus su 100+ entity — sutaupo kod
- ✅ POJO DTO klasės (ne entity) — sudėtinga logiką nėra

### Kada Rankinis Paruošymas Geriau:
- ✅ Edukacinis projektas — matote tikslią struktūrą
- ✅ JPA entity — getter'iai gali turėti logiką (lentos lazy loading, etc.)

### Dėstytojui Atsakyti:

> "Naudojau rankinius getter'ius/setter'ius, nors IDE siūlo Lombok. Tai sąmoningas pasirinkimas — kai kuriate entity su JPA ryšiais, geriau matyti getter'ių ir setter'ių kodą tiesiogiai, negu priklausyti nuo Lombok magic. Tai padeda suprasti, kaip JPA kontroliuoja laukus ir jų inicijalizavimą."

---

## Apibendrinimas Atsiskaitymui

| Klausimas | Atsakymas | Argumentas |
|-----------|-----------|-----------|
| Lombok naudojimas | **Ne, tačiau galima** | Rankinis paruošymas aiškiau demonstruoja entity struktūrą |
| `issaugotiSandelis` | **TEISINGAI** | ASCII tik — koduose lietuviškos raidės neleidžiamos |
| Unsatisfied dependency | **IDE false positive** | CDI extension'ai (runtime) IDE (statinis) negali patikrinti |
| Lombok warnings | **Projekto konvencija** | Sąmoningas pasirinkimas matyti kodą tiesiogiai |

---

## Jei Vis Tiek Nori Naudoti Lombok

Jei tu ar dėstytojas nori pabandyti su Lombok:

```java
@Entity
@Table(name = "KATEGORIJA")
@Getter
@Setter
public class Kategorija {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pavadinimas", length = 100, nullable = false)
    private String pavadinimas;

    @OneToMany(mappedBy = "kategorija", fetch = FetchType.LAZY)
    private List<Produktas> produktai;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kategorija that = (Kategorija) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
```

**Bet:** nėra privalumo. Jei dėstytojas nepageidauja Lombok, palikite rankini.




