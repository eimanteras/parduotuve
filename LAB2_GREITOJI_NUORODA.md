# LAB2 - GREITOJI NUORODA (Quick Reference)

Šitas failas skirtas **greitam žvilgsniui** prieš atsiskaitymą. Jeigu neturi laiko skaityti visa, skaitytkite šitą! ⚡

---

## 1. OPTIMISTINIS RAKINIMAS 🔒

### Kas tai yra?
`@Version` ant lauko → JPA automatiškai patikrina: "Ar niekas nepakeitė tarp perskaitymo ir UPDATE?"

### Kaip veikia?
```
1. Perskaitytas: version = 0
2. UPDATE SQL: WHERE id = 5 AND opt_lock_version = 0
3. Jeigu OK → version = 1
4. Jeigu FAILED (nes version ≠ 0) → OptimisticLockException
```

### OptimisticLockException?
- **Transakcija automatiškai ROLLBACK'ina** (nereik daryti)
- **EntityManager laikomas nepatikimu** → reik `clear()`
- **Klientas gauna HTTP 409** + "retry with new version"

### Sprendimas klientui:
```
1. GET /produktai/{id} → gauti naujausią version
2. PUT /produktai/{id} su naujausia version
3. Tada pavyks
```

---

## 2. ASINCHRONINIS KOMUNIKAVIMAS 🔄

### Kas tai?
Long-running task nesiūbina HTTP response. Serveris grąžina `taskId` ir klientas polling'ina statusą.

### Timeline:
```
client POST → 202 Accepted { taskId } → return iš karto
                                      ↓
                            async lambda vykdytu 8 sekundes
                                      ↓
                            client GET statusą kas 2 sekundes
                                      ↓
                            { status: "DONE" }
```

### Svarbu:
- `managedExecutorService.submit(lambda)` = background thread
- Async **NE** gali naudoti kvietėjo transakcija (kito thread'e)
- Async **NE** gali naudoti `@RequestScoped` EntityManager (request pabaigtas)

### Sprendimas:
```java
managedExecutorService.submit(() -> {
    // Reikia SAVO transakcijos
    txMgr.begin();
    try {
        dariDarbą();
        txMgr.commit();
    } catch (Exception ex) {
        txMgr.rollback();
    }
});
```

---

## 3. CDI - 4 Variantai 🎭

### 3.1 @Alternative - Visiškai kiton implementacija
```java
@Alternative @ApplicationScoped
public class GreitasService implements Service { }

// beans.xml:
<alternatives>
    <class>GreitasService</class>
</alternatives>
```
**Naudojimas:** Development (mock) vs Production (real)

### 3.2 @Specializes - Pavelditas su pakeitimu
```java
@Specializes @ApplicationScoped
public class OptimizuotasService extends BaseService { }
```
**Naudojimas:** "Geresne" versija pagrindinio

### 3.3 Interceptor - Aplink metodą (timing, logging)
```java
@Audited @Interceptor
public class AuditInterceptor {
    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        long start = ...;
        return ctx.proceed();  // Originalus metodas
        // finally log timing
    }
}

// Naudojimas:
@Audited
public String suformuoti() { ... }

// beans.xml:
<interceptors>
    <class>AuditInterceptor</class>
</interceptors>
```

### 3.4 Decorator - Aplink rezultata (papildyti)
```java
@Decorator
public class MyDecorator implements Service {
    @Inject @Delegate private Service delegate;
    
    public String get() {
        return delegate.get() + " (dekoruota)";
    }
}

// beans.xml:
<decorators>
    <class>MyDecorator</class>
</decorators>
```

**Skirtumas:**
- Interceptor: [log start] → [method] → [log end]
- Decorator: [method] → [modify result]

---

## 4. REST - DTOs ir Version 📡

### Kodėl DTO?
- Entity turi `@Version`, `@ManyToOne` - client'ai nežino
- DTO tik duomenys: `id`, `pavadinimas`, `kategorijaId`, `version`

### HTTP Codes:
- **GET** → 200 OK
- **POST** → 201 Created + Location header
- **PUT** → 200 OK (su naujaisiais duomenimis)
- **PUT failure** → 409 Conflict (optimistic lock)

### Kodėl PUT reikalinga version?
```
WHERE id = 5 AND opt_lock_version = 0
```
Jeigu `version` nėra, negali patikrint konflikto!

---

## 5. DĖSTYTOJO KLAUSIMŲ CHEATSHEET 🎯

| Klausimas | Atsakymas |
|-----------|-----------|
| **@Version esmė?** | JPA patikrina version prieš UPDATE |
| **OptimisticLockException su transakcija?** | Automatiškai rollback, EntityManager clear() |
| **Async nesiūbina?** | managedExecutorService.submit() = background thread |
| **Async + transaction?** | NE, nes kito thread'e. Reikia savo tx |
| **Async + @RequestScoped?** | NE, request jau pabaigtas. Naudoti @ApplicationScoped |
| **@Alternative vs @Specializes?** | Alternative = atvira (beans.xml), Specializes = pavelditas (auto) |
| **Interceptor vs Decorator?** | Interceptor aplink metodą, Decorator aplink rezultata |
| **DTO vietoj Entity?** | Saugumas + performance, client'ai nežino apie JPA |
| **PUT version reikalinga?** | Dėl optimistic lock WHERE sąlygos |

---

## 6. POWERSHELL KOMANDOS - Copy & Paste 📋

```powershell
# GET produktus
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai"

# POST (create)
$body = @{ pavadinimas = "Test"; kaina = 10; kategorijaId = 1 } | ConvertTo-Json
$r = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/produktai" -ContentType "application/json" -Body $body
$r.id

# PUT (update) - su version
$body2 = @{ pavadinimas = "Updated"; kaina = 15; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/1" -ContentType "application/json" -Body $body2

# Async start
$body3 = @{ sleepSeconds = 8 } | ConvertTo-Json
$t = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/async/tasks" -ContentType "application/json" -Body $body3
$t.taskId

# Async status
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/async/tasks/{taskId}"

# CDI demo
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/cdi"
```

---

## 7. FAILAI SKAITYMUI (prioritetu tvarka)

1. **LAB2_ISAMUS_PAAISKINIMAS.md** - Bendras supratimas
2. **LAB2_DETALI_KODU_ANALIZE.md** - Kodo eilutė-po-eilutės
3. **LAB2_KLAUSIMU_ATSAKYMAI.md** - Išsamūs atsakymai
4. **LAB2_GREITOJI_NUORODA.md** - ŠIS FAILAS (quick ref)

---

## 8. PRIEŠ ATSISKAITYMĄ - CHECKLIST ✅

- [ ] Supranti `@Version` ir OptimisticLockException
- [ ] Žinai, kaip async nesiūbina request
- [ ] Atskiri @Alternative, @Specializes, Interceptor, Decorator
- [ ] Žinai, kodėl DTO ir version reikalinga PUT'e
- [ ] Galim paleisti REST komandas iš PowerShell
- [ ] Galim paaiškintu konkretaus kodo eilutę iš 3 failų

---

**Tau sekasi! 🚀**

Jeigu dėstytojas pradės klausimu, **šitas failas yra tavo žinynas**. Atsakyk drąsiai! 💪


