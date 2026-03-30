# 2 laboratorinis darbas - Detalus kodo paaiškinimas (eilutė-po-eilutės)

## Tikslas
Šis failas suteikia giluminį supratimą apie KIEKVIENĄ klasę ir metodą, jog galėtum ramiai apginti darbo kodą atsiskaityme. Jei dėstytojas paklaus "Kodėl čia yra `@Version`?" arba "Ką daro `OptimisticLockException`?" - žinosi tikslų atsakymą iš šio failo.

---

# 1. OPTIMISTINIS RAKINIMAS

## 1.1 Produktas.java - Entitetas su `@Version`

### Eilutės 1-8: Paketai ir importai
```java
package lt.eimantas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "PRODUKTAS")
```

**Paaiškinimas:**
- `@Entity` - JPA žino, kad ši klase atitinka DB lentelę
- `@Table(name = "PRODUKTAS")` - sako, kurios DB lentelės šitas entitetas atstovai

### Eilutės 10-14: Pirminės raktas
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**Paaiškinimas:**
- `@Id` - nurodo, kad tai pirminės raktas
- `GenerationType.IDENTITY` - reikšmė sugeneruojama DB (AUTO_INCREMENT)

### Eilutės 16-17: Paprastas laukas
```java
@Column(name = "pavadinimas", length = 100, nullable = false)
private String pavadinimas;
```

**Paaiškinimas:**
- `@Column(name = "pavadinimas")` - susiejimas su DB stulpeliu PAVADINIMAS
- `length = 100` - maksimum 100 simbolių
- `nullable = false` - šis laukas negali būti NULL

### Eilutės 19-20: Kaina
```java
@Column(name = "kaina")
private BigDecimal kaina;
```

**Paaiškinimas:**
- Naudojam `BigDecimal` (o ne `double`) dėl finansų tikslumo (ne slankiojo taško klaidos)

### Eilutės 22-24: Many-to-One ryšis
```java
@ManyToOne
@JoinColumn(name = "kategorija_id")
private Kategorija kategorija;
```

**Paaiškinimas:**
- Vienas `Produktas` priklauso vienai `Kategorijai` (many = produktai, one = kategorija)
- `@JoinColumn(name = "kategorija_id")` - sako, kuris FK stulpelis naudoti DB
- Pavyzdžiui: produktas "Jablka" priklauso kategorijai "Vaisiai"

### Eilutės 26-32: Many-to-Many ryšis
```java
@ManyToMany
@JoinTable(
    name = "PRODUKTAS_SANDELIS",
    joinColumns = @JoinColumn(name = "produktas_id"),
    inverseJoinColumns = @JoinColumn(name = "sandelis_id")
)
private List<Sandelis> sandeliai;
```

**Paaiškinimas:**
- Vienas produktas gali būti keliuose sandeliuose, vienas sandėlis gali turėti keliu produktu
- `@JoinTable` sutvarka lentele `PRODUKTAS_SANDELIS` (jungtinė lentelė)
- `joinColumns` - sayas (produkto) dalies FK
- `inverseJoinColumns` - kitos puses (sandelio) dalies FK

### Eilutės 34-36: `@Version` - OPTIMISTINIS RAKINIMAS
```java
@Version
@Column(name = "opt_lock_version", nullable = false)
private Long version;
```

**KRITINIS KLAUSIMAI IR ATSAKYMAI:**

> **Q: Ką daro `@Version`?**

`@Version` - tai optimistinio rakinimo mechanizmas. Kai kuomet jūs:
1. Perskaitate produktą (version = 0)
2. Išsaugo pakeitimus (UPDATE)

JPA **automatiškai patikrina**: "Ar šis produktas vis dar turi version = 0?"
- Jeigu taip -> UPDATE pavyksta, version = 1
- Jeigu ne (kažkas jau pakeitė) -> **OptimisticLockException**

**Q: Kas yra optimistinis rakinimas?**

Tai strategi, kuomet daroma prielaida, kad konfliktai su ryšiais **retai nutinka**:
- Nesiūbinant "pessimistic" blokirų (DB-level LOCK)
- Vietoje to, tik prieš commit, patikrinant: "Ar dar galioja?"

Likusios getters/setters (eilutės 38-66) ir equals/hashCode yra standartiniai.

---

## 1.2 OptimisticConflictException.java

```java
package lt.eimantas.rest;

public class OptimisticConflictException extends RuntimeException {
    public OptimisticConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Paaiškinimas:**
- Tai custom exception, sukurta specialiai optimistic lock konfliktams
- `extends RuntimeException` - tai unchecked exception (netikslus check), dėl to galim neprivalai ją chatchyt
- `message` - žmogiški tekstas (pvz: "Irasas buvo pakeistas kito naudotojo")
- `cause` - šakninis `OptimisticLockException` iš JPA (žinoti, kodėl ji iš tikro įvyko)

---

## 1.3 ParduotuveService.java - atnaujinti su konflikt valdymu

### Eilutės 52-61: `atnaujintiProdukta` metode
```java
@Transactional
public Produktas atnaujintiProdukta(Produktas p) {
    try {
        Produktas atnaujintas = produktasDAO.update(p);
        produktasDAO.flush();
        return atnaujintas;
    } catch (OptimisticLockException ex) {
        // Po OptimisticLockException esamas persistence context laikomas nepatikimu.
        produktasDAO.clear();
        throw new OptimisticConflictException("Irasas buvo pakeistas kito naudotojo. Atnaujinkite duomenis ir bandykite dar karta.", ex);
    }
}
```

**Eilutė po eilutės:**

1. `@Transactional` - JPA automatiškai užvaldys transakcijas

2. `try { ... Produktas atnaujintas = produktasDAO.update(p);`
   - Bandome atnaujinti produktą
   - JPA prieš `commit` patikrina: "Ar `version` vis tiek galioja?"
   - Jeigu ne -> `OptimisticLockException` iš JPA

3. `produktasDAO.flush();`
   - Sugraudžiam operacijas į DB
   - Jeigu konfliktas -> išduodat exception čia

4. `catch (OptimisticLockException ex) { produktasDAO.clear(); }`
   - Kaip gaunamos `OptimisticLockException`, persistence context jau yra **žalingas**
   - Negalim jame tęsti, nes gali turėti stary, nesulygintas duomenis
   - `clear()` - "išvalom visą iš EntityManager atminties"
   - Toliau: nauja transakcija, nauja EntityManager

5. `throw new OptimisticConflictException(...)`
   - Grąžinam custom exception su žmogiškam žinimu
   - Klientui grąžinsim HTTP 409 (žiūr OptimisticConflictExceptionMapper)

### KLAUSIMAS ATSISKAITYMUI: Kas nutinka su EntityManager po OptimisticLockException?

**Atsakymas:** 
- Po OptimisticLockException, EntityManager jau nepatikimas
- Negali tęsti operacijų toje pačioje transakcijoje / tame pačiame kontekste
- Todėl: `produktasDAO.clear()` - **nusvalyme visą persistence context** (visus objektus iš atminties)
- Jeigu klientas nori kartoti: **jis turi perskaityti naujausią duomenu iš DB** (gauti naujausią version)
- Tik tada gali bandyti atnaujinti iš naujo

---

## 1.4 OptimisticConflictExceptionMapper.java

```java
@Provider
public class OptimisticConflictExceptionMapper implements ExceptionMapper<OptimisticConflictException> {

    @Override
    public Response toResponse(OptimisticConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "OPTIMISTIC_LOCK_CONFLICT",
                        "message", exception.getMessage()
                ))
                .build();
    }
}
```

**Paaiškinimas:**
- `@Provider` - JAX-RS žinai, kad ši klase veido exceptions
- `ExceptionMapper<OptimisticConflictException>` - "jeigu O.O.E. metama, use mano converter"
- `Response.Status.CONFLICT` - HTTP 409 (tai yra standartinis HTTP kodas konfliktams)
- `Map.of(...)` - grąžinam JSON su detaliem: "Kas nutiko? OPTIMISTIC_LOCK_CONFLICT"

**Žmogiškam žodžiam:** Kai klientas gauna 409, žino: "Kas nors jau pakeistė tą pačią eilutę, perskaityti ir kartoti"

---

## 1.5 ProduktasResource.java - PUT operacija su version

### Eilutės 54-84: PUT metode
```java
@PUT
@Path("/{id}")
public ProduktasDto update(@PathParam("id") Long id, ProduktasDto dto) {
    if (dto.getVersion() == null) {
        throw new BadRequestException("PUT uzklausai privalomas version laukas");
    }

    Produktas existing = service.getProduktasById(id);
    if (existing == null) {
        throw new NotFoundException("Produktas nerastas");
    }

    Produktas toUpdate = new Produktas();
    toUpdate.setId(id);
    toUpdate.setVersion(dto.getVersion());  // ← SVARBU
    toUpdate.setPavadinimas(dto.getPavadinimas());
    toUpdate.setKaina(dto.getKaina());
    ...
    
    Produktas updated = service.atnaujintiProdukta(toUpdate);
    return toDto(updated);
}
```

**Eilutė po eilutės:**

1. `if (dto.getVersion() == null) { throw new BadRequestException(...) }`
   - Jeigu klientas nesiųsia `version` - grąžinam error
   - Dėl ko? Nes be `version` JPA negali patikrint, ar konflikto nėra!

2. `toUpdate.setVersion(dto.getVersion());`
   - **KRITINĖ eilutė** - nustatom tą version, kurią klientas gavo ankščiau
   - JPA bus: "OK, jūs tvirtinate, kad version buvo X. Dabar prieš commit patikrinčiau"

3. `service.atnaujintiProdukta(toUpdate);`
   - Iškviečiame service (žiūr ParduotuveService.atnaujintiProdukta)
   - Jeigu konfliktas -> OptimisticConflictException -> HTTP 409

**SVARBI PRAKSA:** PUT iš REST Endpoint vžno grąžina **naujausią version** (po UPDATE)

---

# 2. ASINCHRONINIS KOMUNIKAVIMAS

## 2.1 AsyncTaskService.java - Ilgo darbo valdymas

### Eilutės 1-20: Klasės pradžia
```java
@ApplicationScoped
public class AsyncTaskService {

    @Resource
    private ManagedExecutorService managedExecutorService;

    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();

    public String startLongTask(int sleepSeconds) {
        String taskId = UUID.randomUUID().toString();
        TaskState state = new TaskState("RUNNING", "Pradeta", Instant.now().toString(), null);
        tasks.put(taskId, state);

        managedExecutorService.submit(() -> {
            try {
                Thread.sleep(Math.max(1, sleepSeconds) * 1000L);
                tasks.put(taskId, new TaskState("DONE", "Pabaigta", state.getStartedAt(), Instant.now().toString()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                tasks.put(taskId, new TaskState("FAILED", "Nutraukta", state.getStartedAt(), Instant.now().toString()));
            } catch (Exception ex) {
                tasks.put(taskId, new TaskState("FAILED", ex.getMessage(), state.getStartedAt(), Instant.now().toString()));
            }
        });

        return taskId;
    }
}
```

**Paaiškinimas:**

1. `@ApplicationScoped`
   - Ši bean gyvuoja **visą aplikacijos gyvavimo laiką** (pvz., 24h serveryje)
   - Visoms HTTP request'ams dalina **tą patį AsyncTaskService** (singleton)
   - Todėl `tasks` mapa yra **bendri visiem request'am**

2. `@Resource private ManagedExecutorService managedExecutorService;`
   - Paprotingo Java thread'o vieton, naudojam **application server's managed thread pool**
   - Dėl ko? Jakarta EE žino, kaip teisingai valdyti kontekstą async'e

3. `private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();`
   - `ConcurrentHashMap` - thread-safe mapa (jeigu 2 thread'ai bandy rašyti tuo pat metu - OK)
   - `String` = taskId (identifikavimui), `TaskState` = dabartina statusą

4. `String taskId = UUID.randomUUID().toString();`
   - Sugeneruojam unikaluatid'ą (pvz: `a1b2c3d4-e5f6-...`)
   - Grąžiname klientui, kad jis galėtu klaust "ar pabaigta?"

5. `TaskState state = new TaskState("RUNNING", "Pradeta", ...)`
   - Nustatom pradinę statusą: `RUNNING`, žinutė `Pradeta`, laikas pradėtas

6. `managedExecutorService.submit(() -> { ... })`
   - **SVARBU:** Submit lambda, kuri darbuosis **atskirame thread'e**
   - Grąžinam iš HTTP Request **IŠKART** (nelauksime 8 sekundžių)
   - Tuo metu, lambda darbuojasi nuostatyje

7. `Thread.sleep(Math.max(1, sleepSeconds) * 1000L);`
   - Simuluojam ilgą darbą (pvz, 8 sekundes)
   - Real-world: čia būtu sudėtingas skaiciavimas, DB query'ai, API calls

8. `tasks.put(taskId, new TaskState("DONE", ...));`
   - Po darbo, atnaujinams statusą: `DONE`, grąžinams finishedAt (kada baigta)

### KLAUSIMAS ATSISKAITYMUI: Ar asinchroninis komponentas gali naudoti `@RequestScoped EntityManager`?

**Atsakymas: NE**

Dėl ko?
- `@RequestScoped` yra susietas su HTTP request gyvavimo ciklu
- Jeigu async lambda išduodama, HTTP request jau **grąžintas klientui ir uždartas**
- Todėl `@RequestScoped` EntityManager jau **uždary** arba **invalidus**
- **Praktika:** async'e reikia naudoti:
  - Naują transakcija (`@Transactional`)
  - Arba `@PersistenceContext` su kitokiu scope
  - Arba tiesiog lookup iš JNDI EntityManager factory

### KLAUSIMAS ATSISKAITYMUI: Ar asinchroninis komponentas gali įsijungti į kvietėjo pradėtą transakciją?

**Atsakymas: NE**

Dėl ko?
- Transakcija egzistuoja tame thread'e, kuomet klientas iškviečia
- Async lambda vykdymu **kitame thread'e** - kitam transakcijos kontekste
- Jeigu kvietojas daro COMMIT, o async vis dar durti -> chaos (vienas commitina, kitas rašo)
- **Todėl:** async turi **savo transakcija** (arba naudoti atskirą)

---

## 2.2 AsyncResource.java - REST endpointai

```java
@Path("/async")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AsyncResource {

    @Inject
    private AsyncTaskService asyncTaskService;

    @POST
    @Path("/tasks")
    public Response startTask(Map<String, Integer> payload) {
        int sleepSeconds = payload != null && payload.get("sleepSeconds") != null ? payload.get("sleepSeconds") : 5;
        String taskId = asyncTaskService.startLongTask(sleepSeconds);
        return Response.accepted(Map.of("taskId", taskId, "statusUrl", "/api/async/tasks/" + taskId)).build();
    }

    @GET
    @Path("/tasks/{taskId}")
    public AsyncTaskService.TaskState getTaskStatus(@PathParam("taskId") String taskId) {
        AsyncTaskService.TaskState state = asyncTaskService.getTaskState(taskId);
        if (state == null) {
            throw new NotFoundException("Uzduotis nerasta");
        }
        return state;
    }
}
```

**Paaiškinimas:**

1. `@POST @Path("/tasks")`
   - Klientas siųsial: `{ "sleepSeconds": 8 }`
   - Serveris grąžinaIZ `202 Accepted` (ne 200 OK, nes dar nepabaigta!)
   - Grąžinam `taskId` ir `statusUrl` (kaip klaust: "ar pabaigta?")

2. `Response.accepted(...)`
   - HTTP 202 = "Priėmiau jūsų užduotį, bet jos dar aiškai neatlikau"
   - Su `taskId` ir `statusUrl`, klientas žino, kur klaust

3. `@GET @Path("/tasks/{taskId}")`
   - Klientas periodiškai (polling) klaust: `/api/async/tasks/{taskId}`
   - Serveris grąžinaIZ dabartinį TaskState: `{ status: "RUNNING" }`, `{ status: "DONE" }` ir.t.t.

**Demonstravimas:**
```powershell
# 1. Startuojam
$start = @{ sleepSeconds = 8 } | ConvertTo-Json
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/async/tasks" -ContentType "application/json" -Body $start
$taskId = $response.taskId

# 2. Klausom kol baigta (polling)
do {
    $status = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/async/tasks/$taskId"
    Write-Host "Status: $($status.status) - $($status.message)"
    Start-Sleep -Seconds 2
} while ($status.status -eq "RUNNING")
```

---

# 3. CDI - GLASS-BOX EXTENSIBILITY

## 3.1 @Alternative - Alternatyvios realizacijos

### Idėja
Turėti **dvi realizacijas** bendro intereiso, ir **runtime** pasirinkti kurią naudoti.

### PristatymoService.java - Interfaceas
```java
package lt.eimantas.cdi;

public interface PristatymoService {
    String tipas();
}
```

### StandartinisPristatymoService.java - Pagrindinė realizacija
```java
@ApplicationScoped
public class StandartinisPristatymoService implements PristatymoService {
    @Override
    public String tipas() {
        return "STANDARTINIS";
    }
}
```

**Paaiškinimas:**
- Tai **default** implementacija (jeigu nenurodai alternatyvus)

### GreitasPristatymoService.java - Alternatyvus
```java
@Alternative  // ← SVARBU
@ApplicationScoped
public class GreitasPristatymoService implements PristatymoService {
    @Override
    public String tipas() {
        return "GREITAS_ALTERNATIVE";
    }
}
```

**Paaiškinimas:**
- `@Alternative` sako: "Aš esu alternatyva, ne default"
- Jeigu nenurodai `beans.xml` -> naudosian **StandartinisPristatymoService**
- Jeigu nurodysi `beans.xml` -> naudosian **GreitasPristatymoService**

### beans.xml - Pasirinkimas
```xml
<alternatives>
    <class>lt.eimantas.cdi.GreitasPristatymoService</class>
</alternatives>
```

**Paaiškinimas:**
- "Kartais use GreitasPristatymoService vietoj StandartinisPristatymoService"
- **Praktiniam pavyzdyje:** Development metu naudoti greitas mock'ą, Production metu naudoti real'ią

**Privalumas:** 
- Negereik keist kodo (`@Inject private PristatymoService service;` tas pats)
- Tik `beans.xml` konfigūracija

---

## 3.2 @Specializes - Paveldicimas su pakeitimu

### SkaiciavimoService.java - Bazinė klase
```java
@ApplicationScoped
public class SkaiciavimoService {
    public String versija() {
        return "BAZINIS";
    }
}
```

### SpecializuotasSkaiciavimoService.java - Išplėta versija
```java
@Specializes
@ApplicationScoped
public class SpecializuotasSkaiciavimoService extends SkaiciavimoService {
    @Override
    public String versija() {
        return "SPECIALIZED";
    }
}
```

**Paaiškinimas:**
- `@Specializes` sako: "Aš esu **SPECIJALIZUOTA** versija bazinės klasės"
- **Skirtumam nuo @Alternative:**
  - `@Alternative` = "visiškai kiton implementacija"
  - `@Specializes` = "**pavedinau** ir pakeitiau viski"
- Jeigu injection `@Inject SkaiciavimoService` -> gaus **SpecializuotasSkaiciavimoService**
- Nereik `beans.xml` (veikia automatiš)

**Praktiniam pavyzdyje:**
- Bazine: paprastis skaiciavimas
- Specialized: optimizuotam skaciavimas (arba su papildomis validacijom)

---

## 3.3 CDI Interceptor - Skersinis funkcionalumas

### Audited.java - Annotation'ai
```java
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
}
```

**Paaiškinimas:**
- `@InterceptorBinding` - tai annotation, kurią naudosian markuoti metodai/klasės
- `@Target({ElementType.TYPE, ElementType.METHOD})` - gali dėti ant klases arba metodo
- Neturi parametru - tik markuojam

### AuditInterceptor.java - Interceptor logika
```java
@Audited
@Interceptor
public class AuditInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        long started = System.currentTimeMillis();
        try {
            return context.proceed();  // Kviečiam originalus metodas
        } finally {
            long tookMs = System.currentTimeMillis() - started;
            System.out.println("[AuditInterceptor] " + context.getMethod().getName() + " took " + tookMs + " ms");
        }
    }
}
```

**Paaiškinimas:**

1. `@Audited` - susiejam su Audited annotation'u
2. `@Interceptor` - CDI sako: "Ši klase interceptoriuj"
3. `@AroundInvoke public Object around(InvocationContext context)`
   - Ši metodė iškviečiam **aplink** bet kurį metodą, kurie pažymeti `@Audited`
   - `context.proceed()` = "Eik ir iškviesk originalią metodą"

4. `long started = System.currentTimeMillis();` ... `finally { ... System.out.println(...) }`
   - Aplink metodo vykdymą, atsiskaitom laiką
   - **Svarbų:** `finally` blokas vykdytas **VISADA** (net jei exception)

### Kur naudotas: NumatytasSveikinimoService
```java
@ApplicationScoped
@Audited  // ← Šis metodas bus "interceptuotas"
public class NumatytasSveikinimoService implements SveikinimoService {
    @Override
    public String suformuoti(String vardas) {
        return "Labas, " + vardas;
    }
}
```

**Kai klientas kviečia `suformuoti("Eimantas")`:**
1. Interceptor nusiūbo: `System.out.println("[AuditInterceptor] suformuoti took X ms")`
2. Originalus metodas vykdytas
3. Laiką atsiskaitom

### beans.xml - Interceptor įjungimas
```xml
<interceptors>
    <class>lt.eimantas.cdi.AuditInterceptor</class>
</interceptors>
```

**Privalumas:**
- **Izoliuotas** skersinis funkcionalumas (be verslo logikos terščiam)
- Galim **on/off** perjungti su beans.xml
- Neprivalau keistis originali koda

---

## 3.4 CDI Decorator - Rezultato papildymas

### SveikinimoService.java - Interfaceas
```java
public interface SveikinimoService {
    String suformuoti(String vardas);
}
```

### NumatytasSveikinimoService.java - Bazinė implementacija
```java
@ApplicationScoped
@Audited
public class NumatytasSveikinimoService implements SveikinimoService {
    @Override
    public String suformuoti(String vardas) {
        return "Labas, " + vardas;
    }
}
```

### SveikinimoDecorator.java - Dekorator
```java
@Decorator
public abstract class SveikinimoDecorator implements SveikinimoService {

    @Inject
    @Delegate
    private SveikinimoService delegate;

    @Override
    public String suformuoti(String vardas) {
        return delegate.suformuoti(vardas) + " (dekoruota)";  // ← Papildai rezultata
    }
}
```

**Paaiškinimas:**

1. `@Decorator` - sako: "Aš dekoruoju kitą bean'ą"
2. `@Inject @Delegate private SveikinimoService delegate;`
   - `@Delegate` = "Injekt man **original** SveikinimoService (t.y., NumatytasSveikinimoService)"
   - `delegate.suformuoti(...)` - kviečiam originalią

3. `return delegate.suformuoti(vardas) + " (dekoruota)";`
   - Gaunam originalią rezultata
   - **Papildai** jį ("dekoruota")
   - Grąžinam papildytą

### beans.xml - Decorator įjungimas
```xml
<decorators>
    <class>lt.eimantas.cdi.SveikinimoDecorator</class>
</decorators>
```

**Kai klientas kviečia `suformuoti("Eimantas")`:**
```
Decorator.suformuoti("Eimantas")
  ↓
  Interceptor [AuditInterceptor kviečiam]
    ↓
    NumatytasSveikinimoService.suformuoti("Eimantas") -> "Labas, Eimantas"
    ↑
  Interceptor pabaiga + timing log
  ↓
Decorator grąžina "Labas, Eimantas (dekoruota)"
```

**Skirtumam nuo Interceptor'iaus:**
- **Interceptor** = aplink metodą (timing, logging, validation)
- **Decorator** = **aplink rezultata** (papildyti, transform'inti)

---

## 3.5 CdiDemoResource.java - Visuotiniam endpoint

```java
@Path("/cdi")
@Produces(MediaType.APPLICATION_JSON)
public class CdiDemoResource {

    @Inject
    private PristatymoService pristatymoService;

    @Inject
    private SkaiciavimoService skaiciavimoService;

    @Inject
    private SveikinimoService sveikinimoService;

    @GET
    public Map<String, String> demo() {
        return Map.of(
                "alternative", pristatymoService.tipas(),
                "specialization", skaiciavimoService.versija(),
                "interceptorDecorator", sveikinimoService.suformuoti("Eimantas")
        );
    }
}
```

**Ką jis grąžina:**
```json
{
  "alternative": "GREITAS_ALTERNATIVE",  // ← Jeigu beans.xml turi GreitasPristatymoService
  "specialization": "SPECIALIZED",       // ← Specializes automatiškai use SpecializuotasSkaiciavimoService
  "interceptorDecorator": "Labas, Eimantas (dekoruota)"  // ← Interceptor + Decorator
}
```

**Demonstravimas:**
```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/cdi" | ConvertTo-Json
```

---

# 4. RESTFUL PASLAUGOS

## 4.1 ProduktasDto.java - Duomenų transfer objektas

```java
public class ProduktasDto {
    private Long id;
    private String pavadinimas;
    private BigDecimal kaina;
    private Long kategorijaId;  // ← Tik ID, ne čia Kategorija entitas!
    private Long version;
    
    // getters/setters...
}
```

**Dėl ko DTO atskiras nuo Entity?**

1. **REST klientui negereik žinos apie Entity struktura**
   - Entity turi `@Version`, `@ManyToOne` - tai JPA detalės
   - DTO - tik duomenys, kurie klientui svarbi

2. **version lauke**
   - Entitete: automatiškai validuojam
   - DTO'je: pasiūlai klientui grąžinti version, kad jis mokėtų daryti PUT su correcta versija

3. **kategorijaId vietoj kategorijos objekto**
   - Entitete: `@ManyToOne private Kategorija kategorija;`
   - DTO'je: `private Long kategorijaId;`
   - Dėl ko? Nes REST žino tik IDs (ne pilnus objektus)

---

## 4.2 ProduktasResource.java - Pilnos CRUD operacijos

### GET /api/produktai - Sąrašas
```java
@GET
public List<ProduktasDto> getAll() {
    return service.getVisiProduktai().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
}
```

**Paaiškinimas:**
- Gauna visus produktus iš DB (per ParduotuveService)
- `stream().map()` - konvertojam kiekvienas Entity į DTO
- Grąžinam JSON array: `[ { id: 1, pavadinimas: "Jablka", ... }, ... ]`

### GET /api/produktai/{id} - Vieno produkto
```java
@GET
@Path("/{id}")
public ProduktasDto getById(@PathParam("id") Long id) {
    Produktas p = service.getProduktasById(id);
    if (p == null) {
        throw new NotFoundException("Produktas nerastas");
    }
    return toDto(p);
}
```

**Paaiškinimas:**
- `@PathParam("id")` - gauna ID iš URL (/api/produktai/5 -> id=5)
- Jeigu nepradėti -> HTTP 404

### POST /api/produktai - Kurime naujas
```java
@POST
public Response create(ProduktasDto dto) {
    Produktas p = new Produktas();
    p.setPavadinimas(dto.getPavadinimas());
    p.setKaina(dto.getKaina());

    if (dto.getKategorijaId() != null) {
        Kategorija kategorija = service.getKategorijaById(dto.getKategorijaId());
        if (kategorija == null) {
            throw new BadRequestException("Nurodyta kategorija neegzistuoja");
        }
        p.setKategorija(kategorija);
    }

    Produktas created = service.sukurtiProdukta(p);
    return Response.created(URI.create("/api/produktai/" + created.getId()))
            .entity(toDto(created))
            .build();
}
```

**Paaiškinimas:**

1. `Produktas p = new Produktas();` - naujas entitetas (nėra dar DB)
2. Užpildai laukus iš DTO
3. Jeigu kategorija nurodyta -> check: ar ji egzistuoja
4. `service.sukurtiProdukta(p)` -> @Transactional INSERT į DB
5. `Response.created(URI.create(...))` -> HTTP 201 + Location header su naujo produkto URL
6. `entity(toDto(created))` -> grąžinam naujo produkto duomenis (su id ir version)

**Demonstravimas:**
```powershell
$body = @{ pavadinimas = "Naujas produktas"; kaina = 19.99; kategorijaId = 1 } | ConvertTo-Json
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/produktai" -ContentType "application/json" -Body $body
$response | ConvertTo-Json  # Gauname { id: 5, pavadinimas: ..., version: 0 }
```

### PUT /api/produktai/{id} - Atnaujinimas (su optimistic lock)
```java
@PUT
@Path("/{id}")
public ProduktasDto update(@PathParam("id") Long id, ProduktasDto dto) {
    if (dto.getVersion() == null) {
        throw new BadRequestException("PUT uzklausai privalomas version laukas");
    }

    Produktas toUpdate = new Produktas();
    toUpdate.setId(id);
    toUpdate.setVersion(dto.getVersion());  // ← SVARBU
    toUpdate.setPavadinimas(dto.getPavadinimas());
    toUpdate.setKaina(dto.getKaina());
    
    // ...setKategorija...
    
    Produktas updated = service.atnaujintiProdukta(toUpdate);
    return toDto(updated);
}
```

**Paaiškinimas:**
- Jeigu nėr `version` -> HTTP 400 (Bad Request)
- Nustatom version -> JPA žinos patikrint konfliktą
- Jeigu konfliktas -> OptimisticConflictException -> HTTP 409

### toDto() helper - Entity to DTO
```java
private ProduktasDto toDto(Produktas p) {
    ProduktasDto dto = new ProduktasDto();
    dto.setId(p.getId());
    dto.setPavadinimas(p.getPavadinimas());
    dto.setKaina(p.getKaina());
    dto.setVersion(p.getVersion());
    dto.setKategorijaId(p.getKategorija() != null ? p.getKategorija().getId() : null);
    return dto;
}
```

**Paaiškinimas:**
- Konvertuojam Entity į DTO
- Jeigu nėra kategorijos -> null (o ne klaida)

---

## 4.3 Demonstravimas REST komandu

```powershell
# 1. Gauti visus produktus
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai"

# 2. Gauti konkretų produktą
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai/1"

# 3. Kurti nauja produktą
$newBody = @{ pavadinimas = "Naujas"; kaina = 20.00; kategorijaId = 1 } | ConvertTo-Json
$created = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/produktai" -ContentType "application/json" -Body $newBody
$createdId = $created.id

# 4. Atnaujinti (su version)
$updBody = @{ pavadinimas = "Pakeistas"; kaina = 22.00; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/$createdId" -ContentType "application/json" -Body $updBody

# 5. Jeigu bandyti dar kartą su tuo pačiu version -> gausi 409 Conflict
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/$createdId" -ContentType "application/json" -Body $updBody
# Klaida: OPTIMISTIC_LOCK_CONFLICT
```

---

# 5. ATSAKYMAI Į DAŽNIUS KLAUSIMUS

## Q1: Ką daro @Version?
Automatiškai didina version kiekvieno UPDA'TE prieš COMMIT. Jeigu du naudotojai bando UPDATE tuo pačiu metu:
- Pirmas: UPDATE su version=0 -> pavyks, version=1
- Antras: UPDATE su version=0 -> klaida (OptimisticLockException)

## Q2: Ar PersistenceContext yra async metodeje?
NE. Async vykdytas kitame thread'e, o PersistenceContext susietas su originalaus requeststhread'u.

## Q3: Kaip atskirti Interceptor nuo Decorator?
- **Interceptor** - aplink **metodą** (timing, logging, validation)
- **Decorator** - aplink **rezultata** (papildyti, transform'inti)

## Q4: Kada naudoti @Alternative, kada @Specializes?
- `@Alternative` - visiškai kiton implementacija, reik beans.xml
- `@Specializes` - paveldimas su pakeitimu, automatiš (nereik beans.xml)

## Q5: Jeigu Optional Lock klaida, kas reikia daryti klientui?
1. Gauti HTTP 409 + error messasge
2. Perskaityti naugausią produktą (GET /api/produktai/{id})
3. Panaudoti naujausią version
4. Kartoti PUT

---

# 6. PRAKTINIAM TESTAS - Atsiskaitymam

Būk pasirengęs atsakyti:

1. "Kaip optimistinis rakinimas aptinka konfliktą?"
   - Paaiškink: @Version, JPA patikra prieš COMMIT

2. "Ką gauna klientas, kai atsitinka OptimisticLockException?"
   - Paaiškink: HTTP 409, "version nebesutampa"

3. "Kaip asinchroninis darbas nesiūbina HTTP request'o?"
   - Paaiškink: managedExecutorService.submit(), grąžinam iš karto, lambda darbuojasis nuostatyje

4. "Skirtumas tarp @Alternative ir @Specializes?"
   - Paaiškink: Alternative = atvira alternatyva, Specializes = paveldimas su pakeitimu

5. "Kaip interceptor ir decorator skiriasi?"
   - Paaiškink: Interceptor aplink metodą, Decorator aplink rezultata

6. "Kodėl PUT reikalinga version?"
   - Paaiškink: dėl optimistic lock patikros (jeigu nėr version -> negali patikrint konflikto)

---

# 7. TRUMPA LOGIKA KADA KLAUSTA

**Jeigu dėstytojas paklaus apie konkretią eilutę:**

Nepanikinės! Naudokis šiom žingsniam:

1. Rask eilutę faile (su `Ctrl+F` IDE)
2. Skaityk konteksta aplink (5 eilutės prieš, 5 po)
3. Nusaki, ko toji eilutė daro (iš šio `.md` failo)
4. Paaiškink, *dėl ko* tai reikalinga (business logic)

**Pavyzdžiui:**
- Eilutė: `@Version`
- Ką ji daro: "Automatiškai didina versija kiekvieno UPDATE"
- Dėl ko: "Nes naudojam optimistic locking strategija konfliktams aptikti"

Tada jūs atrodysi **ŽINĄS** koda! 🎓


