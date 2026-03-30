# 2 Laboratorinis - ATSISKAITYMĄ SUDĖTINGI KLAUSIMAI IR ATSAKYMAI

## Tikslas
Šis failas turi **TIKSLIUS, IŠSAMIUS** atsakymus į klausimus, kurie 99% tikimybę dėstytojas paklaus atsiskaityme. Skaityk šitą failą per naktį prieš atsiskaitymą! 😎

---

# OPTIMISTINIS RAKINIMAS - 0.3 balai

## K1: "Ką iš tikro daro `@Version` anotacija?"

### Atsakymas (rimtas ir detalus):

`@Version` - tai JPA mekanizmas, kuris automatiškai **didina versijos numerį kiekvieno UPDATE metu**.

**Kaip tai veikia?**

1. **Kai perskaitytas įrašas iš DB:**
   ```
   SELECT id, pavadinimas, kaina, opt_lock_version FROM produktas WHERE id = 5;
   → produkto.version = 0 (grąžinta iš DB)
   ```

2. **Kai atliekami pakeitimai:**
   ```
   produkto.setPavadinimas("Naujas pavadinimas");
   // produkto.version vis dar = 0 (nepakeitėm)
   ```

3. **Kai bandai išsaugoti (UPDATE):**
   ```
   UPDATE PRODUKTAS 
   SET pavadinimas = 'Naujas pavadinimas', opt_lock_version = 1
   WHERE id = 5 AND opt_lock_version = 0;  // ← SVARBI sąlyga!
   ```

4. **Kas gali atsitikti?**
   - **A) Jeigu versija sutampa (0 = 0):**
     ```
     UPDATE pavyks, version bus updated į 1
     Grąžinam sėkmę klientui
     ```
   
   - **B) Jeigu versija NESUTAMPA (0 ≠ 1):**
     ```
     UPDATE atmes (0 eilučių paveikta)
     JPA meta OptimisticLockException
     ```

**Dėl ko tai reikalinga?**

Imagine šitas scenarijus:
1. Klientas A perskaito: `version = 0`
2. Klientas B perskaito: `version = 0`
3. Klientas B padaro UPDATE → `version = 1`
4. Klientas A bandė UPDATE su `version = 0` → **KLAIDA!** (Nuo tada jau yra 1)

**Tai vadinama optimistiniu rakinimu:**
- Pasitikėtam, kad konfliktai RETAI atsitinka
- Nesiūbinam DB-level blokatorių
- Tik prieš COMMIT, patikrinama: "Ar dar galioja version?"

---

## K2: "Kas nutinka su transakcija, kai gaunamas OptimisticLockException?"

### Atsakymas:

Kai `OptimisticLockException` išmetama, **transakcija automatiškai pažymima kaip rolled-back**.

**Detales:**

1. **Transakcija laikoma FAILED:**
   ```
   @Transactional
   public Produktas atnaujintiProdukta(...) {
       try {
           produktasDAO.update(p);  // ← Iš čia meta OptimisticLockException
           produktasDAO.flush();     // ← Negausim iki čia!
       } catch (OptimisticLockException ex) {
           // Transakcija jau pažymėta rollback-only!
       }
   }
   ```

2. **Kai baigiasi metoda:**
   - JPA automatiškai rodo ROLLBACK (ne COMMIT)
   - Visos operacijos tos transakcijos yra "anuliuotos"
   - DB grįžta į buvusią būseną

3. **Praktiniam pavyzdyje:**
   ```sql
   BEGIN TRANSACTION
   UPDATE produktas SET ... -- (jeigu čia OptimisticLockException)
   ROLLBACK;  -- Sugrįžtam į pradinę būseną
   ```

**Svarbų:** Negalim "spasint" transakcija:
- Negali daryti `commit()` po `OptimisticLockException`
- Transakcija XIAOEDNA

---

## K3: "Kas nutinka su EntityManager, kai gaunamas OptimisticLockException? Kaip daryti update po to?"

### Atsakymas:

Po `OptimisticLockException`, **EntityManager persistence context laikomas ŽALISMU ir NEPATIKIMES**.

**Dėl ko?**

Kai JPA bando UPDATE su WHERE sąlyga:
```sql
UPDATE PRODUKTAS 
SET ... 
WHERE id = 5 AND opt_lock_version = 0
```

Jeigu šita sąlyga NEPAVYKSTA (nes version jau 1), JPA persistence context **negali žinoti, kurie objektai šiame kontekste yra tikri**.

**Todėl praktika:**

```java
@Transactional
public Produktas atnaujintiProdukta(Produktas p) {
    try {
        Produktas atnaujintas = produktasDAO.update(p);
        produktasDAO.flush();
        return atnaujintas;
    } catch (OptimisticLockException ex) {
        produktasDAO.clear();  // ← SVARBU! Išvalom persistence context
        throw new OptimisticConflictException(...);
    }
}
```

**`produktasDAO.clear();` daro:**
- Nuimam **VISUS objektus** iš persistence context atminties
- Naujas request = naujas EntityManager = naujas persistence context
- Jeigu klientas bandys retry -> nauja operacija su nauja session

**Kaip klientas turėtu bandyti iš naujo?**

1. Gauna HTTP 409 + "version reikalinga"
2. Perskaito produktą: `GET /api/produktai/5`
3. Gauna **naujausią version** (pvz, 1)
4. Kartoja PUT su naujausia versija:
   ```json
   {
       "pavadinimas": "...",
       "kaina": 50.0,
       "kategorijaId": 1,
       "version": 1  // ← Naujausias!
   }
   ```
5. Dabar UPDATE pavyks!

---

# ASINCHRONINIS KOMUNIKAVIMAS - 0.2 balai

## K4: "Kaip asinchroninis komponentas nesiūbina HTTP request'o?"

### Atsakymas:

**Atsakyme grąžinam iš karto, o darbas tęsiasi fone.**

```java
@POST
@Path("/tasks")
public Response startTask(Map<String, Integer> payload) {
    int sleepSeconds = payload.get("sleepSeconds");  // Gauna kliento 8 sekundes
    
    String taskId = asyncTaskService.startLongTask(sleepSeconds);
    
    // IŠKART grąžinam response (nelauksime 8 sekundžių!)
    return Response.accepted(Map.of("taskId", taskId, "statusUrl", "/api/async/tasks/" + taskId)).build();
}
```

**Detales AsyncTaskService:**

```java
public String startLongTask(int sleepSeconds) {
    String taskId = UUID.randomUUID().toString();
    TaskState state = new TaskState("RUNNING", "Pradeta", ...);
    tasks.put(taskId, state);  // Nuimam į mapą: "Task A yra RUNNING"

    // ← KRITINĖ linija: submit lambda "background thread'e"
    managedExecutorService.submit(() -> {
        try {
            Thread.sleep(sleepSeconds * 1000L);  // ← Šita vyksta KITAME thread'e!
            tasks.put(taskId, new TaskState("DONE", ...));  // Atnaujina mapą
        } catch (Exception ex) {
            tasks.put(taskId, new TaskState("FAILED", ...));
        }
    });

    return taskId;  // ← Grąžiname IŠKART, nelauksime 8 sekundžių!
}
```

**Laiko linija:**

```
Sekundės:  0        1        2        3        4        5        6        7        8
Main:      [POST]──────────────────────────────────────────────────────────────────
           |accepts  returns 202 Accepted + taskId|
           
Async:     [Thread.sleep(8s) vykdytas atskirame thread'e]────────────────[DONE]
```

**Praktiniam kliento perspektyvoje:**

```powershell
# 1. Start (grąžinam iš karto per 50ms)
POST /api/async/tasks?sleepSeconds=8
← 202 Accepted { taskId: "a1b2c3d4" }

# 2. Klientas polling (kas 2 sekundes)
GET /api/async/tasks/a1b2c3d4 
← { status: "RUNNING" }

GET /api/async/tasks/a1b2c3d4 
← { status: "RUNNING" }

GET /api/async/tasks/a1b2c3d4 
← { status: "DONE" }  ← Po 8 sekundžių
```

---

## K5: "Ar asinchroninis komponentas gali įsijungti į kvietėjo pradėtą transakciją?"

### Atsakymas: NE

**Dėl ko?**

Transakcija egzistuoja **konkrečiame thread'e, per konkretų HTTP request**.

```
HTTP Request Thread        | Async Background Thread
─────────────────────────  | ─────────────────────────
BEGIN TRANSACTION          |
SELECT produktas           |
managedExecutorService     |
.submit(() -> { ... })     | ← Lambda pradės naujame thread'e
  ↓                        |
COMMIT TRANSACTION         | (Transakcija grįžo iš HTTP request)
  ↓                        | Lambda dar tęsiasi...
                           | (Jeigu jis bandys UPDATE?)
                           | KLAIDA: nei transakcijos, nei EntityManager
```

**Praktiniame pavyzdyje:**

```java
@Transactional
public void kuriProduktą() {
    Produktas p = new Produktas();
    p.setPavadinimas("Jablka");
    
    managedExecutorService.submit(() -> {
        // ← Šitam lambda NĖRA transakcijos!
        produktasDAO.save(p);  // Klaida: nėra EntityManager konteksto
    });
    
    // HTTP Request transakcija COMMIT'ina, lambda dar darbuojasi
}
```

**Jeigu async reikalinga DB operacija:**

```java
managedExecutorService.submit(() -> {
    // ← Reikia **savo** transakcijos
    txMgr.begin();
    try {
        produktasDAO.save(p);
        txMgr.commit();
    } catch (Exception ex) {
        txMgr.rollback();
    }
});
```

**Arba naudoti `@RequestScoped` su `@Transactional`:**

```java
@ApplicationScoped  // Async komponentas
public class AsyncService {
    
    @Inject
    private ProduktoService service;  // Šis yra @Transactional
    
    public void doDarkWork() {
        managedExecutorService.submit(() -> {
            service.dariDarbą();  // ← Service vidiniai @Transactional!
        });
    }
}

@RequestScoped  // ← Bet vis dar needs own request context
@Transactional
public class ProduktoService {
    public void dariDarbą() {
        // Šitam metodui JPA duoda naujas EntityManager
    }
}
```

---

## K6: "Ar asinchroninis komponentas gali naudoti `@RequestScoped EntityManager`?"

### Atsakymas: NE

**Dėl ko?**

`@RequestScoped` = "šitas komponentas gyvuoja HTTP request gyvavimo ciklo metu"

```
HTTP Request:      [START]──────────────────────────────────[END]
@RequestScoped:    |exists|───────────────────────────────|destroyed|
                   
Async Thread:                              [VYKDYTAS ČIAAA, BET REQUEST JJAu PABAIGTAS]
```

Kai async lambda pradedi su `Thread.sleep(8)`:

```java
// HTTP Request thread
managedExecutorService.submit(() -> {  // ← Submitina background thread'e
    Thread.sleep(8000);  // ← 8 sekundės...
    // ...bet HTTP Request jau pabaigtas!
    // @RequestScoped EntityManager jau UŽDARYTAS / NUSTATYTAS į null
});

// HTTP Response grąžinta klientui, @RequestScoped EntityManager DESTROYED
```

**Todėl, jeigu async lambda bandytų:**

```java
managedExecutorService.submit(() -> {
    Produktas p = entityManager.find(Produktas.class, 1);  // ← NullPointerException!
    // entityManager nėra (jis buvo @RequestScoped, request pabaigtas)
});
```

**Sprendimas: naudoti `@ApplicationScoped` EntityManager:**

```java
@ApplicationScoped  // Gyvuoja visą aplikacijos laiką
public class AsyncRepository {
    
    @PersistenceContext(unitName = "ParduotuvePU")
    private EntityManager em;  // ← Šitas gyvuoja, o @RequestScoped - ne
    
    public void doWork() {
        managedExecutorService.submit(() -> {
            Produktas p = em.find(...);  // ← OK, EntityManager vis dar egzistuoja
        });
    }
}
```

**Arba use ConnectionPool iš JNDI:**

```java
@Resource(lookup = "java:jboss/datasources/ParduotuveDataSource")
private DataSource ds;

managedExecutorService.submit(() -> {
    Connection conn = ds.getConnection();  // ← Tiesiog connection, ne EntityManager
    // Darius SQL iš čia...
    conn.close();
});
```

---

# GLASS-BOX EXTENSIBILITY (CDI) - 0.4 balai

## K7: "Skirtumas tarp `@Alternative` ir `@Specializes`?"

### Atsakymas:

| Aspektas | `@Alternative` | `@Specializes` |
|----------|---|---|
| **Esmė** | Visiškai KITON implementacija | PAVELDITAS su pakeitimu |
| **Valdymas** | Reikia `beans.xml` | Automatiš (nereik beans.xml) |
| **Bazinė klase** | Reikia implementuoti interface'us, nereik pavelding | **REIKIA** paveldinti bazinę |
| **Injection** | `@Inject SomeService` gaus ALTERNATIVE (jeigu įjungta) | `@Inject SomeService` gaus SPECIALIZED |
| **Naudojimo atvejis** | Development mock vs Production | "Galingesne" versija bazinės |

**Praktinis pavyzdys 1: @Alternative (Development vs Production)**

```java
// Production versija (default)
@ApplicationScoped
public class KategorijaDAO {
    public List<Kategorija> gaviAll() {
        // Real DB query
        return entityManager.createQuery("SELECT k FROM Kategorija k", Kategorija.class)
            .getResultList();
    }
}

// Development versija (mock)
@Alternative
@ApplicationScoped
public class MockKategorijaDAO implements KategorijaDAO {
    public List<Kategorija> getAll() {
        // Hardkoded mock duomenys (greitas testing)
        return List.of(
            new Kategorija(1L, "Vasiai"),
            new Kategorija(2L, "Primabalai")
        );
    }
}
```

**beans.xml (Production):**
```xml
<!-- Nenurodome - gausim KategorijaDAO (default) -->
```

**beans.xml (Development):**
```xml
<alternatives>
    <class>MockKategorijaDAO</class>
</alternatives>
<!-- Gausim MockKategorijaDAO vietoj KategorijaDAO -->
```

**Praktinis pavyzdys 2: @Specializes (Bazinė + Specialized)**

```java
// Bazinė versija
@ApplicationScoped
public class SkaiciavimoService {
    public String versija() {
        return "BAZINIS - Simple math";
    }
}

// Specialistuota versija (greičiau, geresnė, etc.)
@Specializes
@ApplicationScoped
public class OptimizuotasSkaiciavimoService extends SkaiciavimoService {
    @Override
    public String versija() {
        return "SPECIALIZED - Optimized with caching";
    }
}
```

**Injection:**
```java
@Inject
private SkaiciavimoService service;  // ← Gaus OptimizuotasSkaiciavimoService!
// (@Specializes automatiškai "replace'ina" bazinę)
```

**Nereik beans.xml!** (@Specializes veikia automatiš)

---

## K8: "Koks yra CDI Interceptor ir kokiam jis skirtas?"

### Atsakymas:

**CDI Interceptor** - tai mechanizmas, kuris leidžia **aplink metodą** įterpti papildomą logiką (timing, logging, validation) **be šito metodo pakeitimo**.

### Struktura:

```java
// 1. Anotacija (@InterceptorBinding)
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
}

// 2. Interceptor klasė
@Audited
@Interceptor
public class AuditInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        long started = System.currentTimeMillis();
        System.out.println("PRIEŠ: " + context.getMethod().getName());
        
        try {
            Object result = context.proceed();  // ← Kviečiam originalią metodą
            System.out.println("SĖKMĖ!");
            return result;
        } catch (Exception ex) {
            System.out.println("KLAIDA: " + ex.getMessage());
            throw ex;
        } finally {
            long tookMs = System.currentTimeMillis() - started;
            System.out.println("TRUKMĖ: " + tookMs + " ms");
        }
    }
}

// 3. Naudojimas - markuojam metodą @Audited
@ApplicationScoped
public class NumatytasSveikinimoService implements SveikinimoService {
    
    @Override
    @Audited  // ← Šis metodas bus "interceptuotas"
    public String suformuoti(String vardas) {
        return "Labas, " + vardas;
    }
}

// 4. beans.xml - įjungimas/išjungimas
<interceptors>
    <class>lt.eimantas.cdi.AuditInterceptor</class>
</interceptors>
```

**Kai klientas kviečia `suformuoti("Eimantas")`:**

```
1. AuditInterceptor.around() PRADEDA
   └─ System.out.println("PRIEŠ: suformuoti")
   
2. context.proceed() - Kviečiam originalią
   └─ return "Labas, Eimantas"
   
3. AuditInterceptor.around() BAIGIA
   └─ System.out.println("SĖKMĖ!")
   └─ System.out.println("TRUKMĖ: 2 ms")
```

### K9: Ar interceptor gali nustatyti metodą (pvz., grąžinti custom rezultatą)?

**Atsakymas: SVARBU ŽINOTI**

Interceptor **CAN** pakeisti rezultatą:

```java
@AroundInvoke
public Object around(InvocationContext context) throws Exception {
    
    // Vienas iš variantų: neskaityk originalios metodos
    if (shouldSkip()) {
        return "Custom rezultas (skipintas originalas)";
    }
    
    // Arba: change rezultatą
    Object result = context.proceed();
    return "Modified: " + result;
}

private boolean shouldSkip() {
    // Logic...
    return true;
}
```

**Tačiau:** Tai nėra "puiki" praktika - todėl existuoja **Decorator** (žiūr K10)

---

## K10: "Ar yra skirtumas tarp Interceptor ir Decorator?"

### Atsakymas: SKIRTUMPAI YRA DIDELI

| Aspektas | **Interceptor** | **Decorator** |
|----------|---|---|
| **Tikslas** | Aplink metodą (timing, logging, validation) | Aplink REZULTATA (papildyti, transformuoti) |
| **@AroundInvoke** | `context.proceed()` | - (yra delegate) |
| **Panaudojimas** | Kaip šalinis effect'as | Kaip wrapper |
| **Grąžinama** | Originalaus metodo rezultatas | Papildytas rezultatas |
| **Anotacija** | `@Audited`, `@Loggable` | `@Decorator` |

### Praktinis pavyzdys - Interceptor:

```java
@Audited
@Interceptor
public class AuditInterceptor {
    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        long started = System.currentTimeMillis();
        
        try {
            return context.proceed();  // ← Originalus metodas vykdytas
        } finally {
            System.out.println("Took " + (System.currentTimeMillis() - started) + " ms");
        }
    }
}
```

**Naudojimas:**
```java
public class NumatytasSveikinimoService {
    @Audited
    public String suformuoti(String vardas) {
        return "Labas, " + vardas;
    }
}
```

**Rezultatas:**
```
Logger output: "Took 2 ms"
Metodui grąžintas: "Labas, Eimantas"
```

### Praktinis pavyzdys - Decorator:

```java
@Decorator
public abstract class SveikinimoDecorator implements SveikinimoService {

    @Inject
    @Delegate
    private SveikinimoService delegate;  // ← Original bean

    @Override
    public String suformuoti(String vardas) {
        String originalResult = delegate.suformuoti(vardas);  // "Labas, Eimantas"
        return originalResult + " (dekoruota)";  // ← Papildau
    }
}
```

**Rezultatas:**
```
Metodui grąžintas: "Labas, Eimantas (dekoruota)"
```

### Vaizdinis palyginimas:

```
Interceptor: [Timing/Log] → [Original Method] → [Timing/Log]
             └─ Skersinis функчионалумас aplink metodą

Decorator:   [Original Method] → [Transform Result] → [Enhanced Result]
             └─ Wrapper aplink rezultatą
```

---

# RESTFUL PASLAUGOS - 0.1 balai

## K11: "Kodėl REST naudojam DTO (ProduktasDto) o ne Entity (Produktas)?"

### Atsakymas:

**DTO = Data Transfer Object** - tai skirtas **TIKTAI duomenų transportui** (ne business logic).

### Šaltinis: Entity vs. DTO

```
Entity (Produktas)              | DTO (ProduktasDto)
────────────────────────────────────────────────
@Entity                         | Plain POJO
@Table(name="PRODUKTAS")        | Nėra DB anotacijų
@ManyToOne Kategorija           | kategorijaId (tik Long)
@Version                        | version (viskas)
@ManyToMany List<Sandelis>      | (negražina, dėl serialization)
```

### Kodėl DTO?

1. **Bezpečnost** - entity turi `@Version`, client'ai nežino apie optimistic locking
2. **Performance** - nedudžinams Entity su `@OneToMany` relacijomis (N+1 klaidos)
3. **API kontraktas** - DTO apsaugo Entity changes nuo client'ų

### Praktinis pavyzdys - BLOGAI (naudoti Entity):

```java
@GET
public Produktas getById(@PathParam("id") Long id) {
    return produktasDAO.findById(id);  // ← Grąžinam Entity
}
// Rezultatas:
{
  "id": 1,
  "pavadinimas": "Jablka",
  "kategorija": {  // ← Lazy-loading klaida!
    "id": 1,
    "pavadinimas": "Vasiai",
    "produktai": [...]  // ← Circular reference!
  }
}
```

**Problemos:**
- Kategorija jos produktai -> kategorija -> ... (circular)
- Lazy-loading exception (EntityManager closed)
- Client'as matai @Version ir kitus JPA detalyse

### Praktinis pavyzdys - GERAI (naudoti DTO):

```java
@GET
public ProduktasDto getById(@PathParam("id") Long id) {
    Produktas p = produktasDAO.findById(id);
    return toDto(p);  // ← Konvertuojam į DTO
}

private ProduktasDto toDto(Produktas p) {
    ProduktasDto dto = new ProduktasDto();
    dto.setId(p.getId());
    dto.setPavadinimas(p.getPavadinimas());
    dto.setKategorijaId(p.getKategorija() != null ? p.getKategorija().getId() : null);
    dto.setVersion(p.getVersion());
    return dto;
}

// Rezultatas:
{
  "id": 1,
  "pavadinimas": "Jablka",
  "kategorijaId": 1,  // ← Tik ID, ne object
  "version": 0
}
```

---

## K12: "Kodėl PUT reikalinga `version` laukas?"

### Atsakymas:

Dėl **optimistic locking** - JPA turi patikrint: "Ar šis produktas vis tiek turi version=0?"

```
Clientas:
1. GET /api/produktai/5
   ← { id: 5, pavadinimas: "Jablka", version: 0 }

2. Naudotojas keičia: "Gruša"

3. PUT /api/produktai/5
   {
     "pavadinimas": "Gruša",
     "version": 0  // ← SVARBU!
   }
```

**Serveris:**

```java
@PUT
@Path("/{id}")
public ProduktasDto update(@PathParam("id") Long id, ProduktasDto dto) {
    if (dto.getVersion() == null) {
        throw new BadRequestException("version reikalingas!");
    }
    
    Produktas toUpdate = new Produktas();
    toUpdate.setId(id);
    toUpdate.setVersion(dto.getVersion());  // ← dto.version = 0
    toUpdate.setPavadinimas(dto.getPavadinimas());
    
    // JPA tada dares:
    // UPDATE PRODUKTAS SET ... WHERE id = 5 AND opt_lock_version = 0
    Produktas updated = service.atnaujintiProdukta(toUpdate);
    return toDto(updated);
}
```

**Jeigu kas nors jau pakeitė:**

```
Klientas A:
GET /api/produktai/5 → version: 0
(laukia 5 min)

Klientas B:
PUT /api/produktai/5 su version: 0 → SUCCESS, now version: 1

Klientas A:
PUT /api/produktai/5 su version: 0 (STALE!) → OptimisticLockException → HTTP 409
```

**Todėl version SVARBU:**
- Sudaro Optimistic Locking pagrindą
- Kaip CAS (Compare-And-Swap) DB'eje
- Neimanomas "lost update" problem'ų

---

## K13: "Kaip REST response grąžinti (GET, POST, PUT)?"

### Atsakymas:

**GET:**
```java
@GET
public List<ProduktasDto> getAll() {
    return service.getVisiProduktai().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
}
// HTTP 200 OK
// Body: [ { id: 1, ... }, { id: 2, ... } ]
```

**POST:**
```java
@POST
public Response create(ProduktasDto dto) {
    Produktas p = new Produktas();
    // ...fill fields...
    Produktas created = service.sukurtiProdukta(p);
    
    return Response.created(URI.create("/api/produktai/" + created.getId()))
            .entity(toDto(created))
            .build();
}
// HTTP 201 Created
// Header: Location: /api/produktai/5
// Body: { id: 5, ... }
```

**PUT:**
```java
@PUT
@Path("/{id}")
public ProduktasDto update(@PathParam("id") Long id, ProduktasDto dto) {
    // ... validation, create toUpdate ...
    Produktas updated = service.atnaujintiProdukta(toUpdate);
    return toDto(updated);
}
// HTTP 200 OK (arba 204 No Content)
// Body: { id: 5, version: 1, ... } (naujausiais duomenimis)
```

**PUT su OptimisticLockException:**
```
HTTP 409 Conflict
Body: {
  "error": "OPTIMISTIC_LOCK_CONFLICT",
  "message": "Irasas buvo pakeistas kito naudotojo. Atnaujinkite duomenis ir bandykite dar karta."
}
```

---

# IŠBANDŽIAU POWERSHELL KOMANDOS

## REST Testing Commands (copy-paste ready)

```powershell
# ============ PRODUKTAI ============

# 1. Gauti visus produktus
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai"

# 2. Gauti vieną produktą
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai/1"

# 3. Kurti naują produktą
$newBody = @{ pavadinimas = "Naujas produktas"; kaina = 15.99; kategorijaId = 1 } | ConvertTo-Json
$created = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/produktai" `
    -ContentType "application/json" -Body $newBody
$createdId = $created.id
$createdId

# 4. Atnaujinti produktą (su version)
$updBody = @{ pavadinimas = "Pakeistas"; kaina = 17.99; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/$createdId" `
    -ContentType "application/json" -Body $updBody

# ============ OPTIMISTIC LOCK (Conflict) ============

# 5. Bandyti atnaujinti su stale version (gausi 409 Conflict)
$staleBody = @{ pavadinimas = "Bandymas"; kaina = 99.00; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/$createdId" `
    -ContentType "application/json" -Body $staleBody
# Rezultatas: 409 Conflict

# ============ ASYNC TASKS ============

# 6. Startuoti async task (8 sekundes)
$asyncBody = @{ sleepSeconds = 8 } | ConvertTo-Json
$taskResp = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/async/tasks" `
    -ContentType "application/json" -Body $asyncBody
$taskId = $taskResp.taskId
Write-Host "Task ID: $taskId"

# 7. Klaust task status (polling)
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/async/tasks/$taskId" | ConvertTo-Json

# 8. Polling kol pabaigta
$done = $false
while (-not $done) {
    $status = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/async/tasks/$taskId"
    Write-Host "[$($status.status)] $($status.message)"
    if ($status.status -eq "DONE") { $done = $true }
    Start-Sleep -Seconds 2
}

# ============ CDI ============

# 9. Patikrinti CDI extensions
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/cdi" | ConvertTo-Json
# Rezultatas:
# {
#   "alternative": "GREITAS_ALTERNATIVE",
#   "specialization": "SPECIALIZED",
#   "interceptorDecorator": "Labas, Eimantas (dekoruota)"
# }
```

---

# REZIUMĖ - ATSAKYTI ŠIŲ KLAUSIMŲ ATSISKAITYME

### 1. Optimistinis rakinimas (0.3):
- [ ] Ką daro @Version?
- [ ] Kas nutinka su transakcija kai OptimisticLockException?
- [ ] Kas nutinka su EntityManager ir kaip fix'inti?

### 2. Asinchroninis komunikavimas (0.2):
- [ ] Kaip nesiūbinamas request?
- [ ] Ar async gali naudoti kvietėjo transakcija?
- [ ] Ar async gali naudoti @RequestScoped EntityManager?

### 3. CDI (0.4):
- [ ] @Alternative vs @Specializes?
- [ ] Interceptor tikslas?
- [ ] Interceptor vs Decorator?

### 4. REST (0.1):
- [ ] Kodėl DTO a ne Entity?
- [ ] Kodėl PUT reikalinga version?
- [ ] HTTP status codes (201, 200, 409)?

---

# FINAL TIP: Atsakyti kaip PRO 🎓

Jeigu dėstytojas paklaus **konkretaus kodo eilutės**:

1. **Raskite** failę (IDE `Ctrl+F`)
2. **Perskaitykite** kontekstą (5 eilutės prieš, 5 po)
3. **Nusaikite** iš šio `.md` failo (tikslus atsakymas)
4. **Paaiškinkite**, dėl ko tai reikalinga

**Pavyzdys:**
> Dėstytojas: "Kas šita linija: `produktasDAO.clear();`?"

Jūs:
> "Tai JPA `clear()` metodas. Po `OptimisticLockException`, persistence context yra nepatikimas (galėtų turėti stale duomenis). Todėl nusvalome visą kontekstą. Jeigu reikia iš naujo operuoti, nauja transakcija / nauja EntityManager bus skirtas."

**SĖKMĖS!** 🚀


