# 2 laboratorinio darbo demonstracija

## Kas įgyvendinta

- Optimistinis rakinimas su `@Version` lauku esybėje `src/main/java/lt/eimantas/entity/Produktas.java`.
- Konflikto apdorojimas servise `src/main/java/lt/eimantas/service/ParduotuveService.java` (metodas `atnaujintiProdukta`).
- REST API su `GET`, `POST`, `PUT` per `src/main/java/lt/eimantas/rest/ProduktasResource.java`.
- Asinchroninis ilgas darbas su `ManagedExecutorService` per `src/main/java/lt/eimantas/service/AsyncTaskService.java` ir `src/main/java/lt/eimantas/rest/AsyncResource.java`.
- CDI `@Alternative`, `@Specializes`, `Interceptor`, `Decorator` demonstracija per `src/main/java/lt/eimantas/cdi/*` ir `src/main/webapp/WEB-INF/beans.xml`.

## REST pavyzdžiai

### 1) GET visi produktai

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai"
```

### 2) POST naujas produktas

```powershell
$body = @{ pavadinimas = "Test produktas"; kaina = 12.50; kategorijaId = 1 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/produktai" -ContentType "application/json" -Body $body
```

### 3) PUT produkto atnaujinimas (su version)

```powershell
$body = @{ pavadinimas = "Atnaujintas"; kaina = 15.00; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/1" -ContentType "application/json" -Body $body
```

## Kaip pademonstruoti OptimisticLockException

1. `GET /api/produktai/1` ir užsirašyti `version`.
2. Tą patį `version` panaudoti dviejose skirtingose `PUT` užklausose.
3. Pirma `PUT` pavyksta, antra grąžina `409 Conflict` su klaida `OPTIMISTIC_LOCK_CONFLICT`.

## Klausimai atsiskaitymui

### Kas nutinka su einamąja transakcija gavus `OptimisticLockException`?

- Transakcija pažymima `rollback-only` ir nebegali būti sėkmingai commitinama.

### Kas nutinka su einamuoju `EntityManager`?

- Esamas `persistence context` po tokios klaidos laikomas nepatikimu.
- Šiame projekte po konflikto kviečiamas `clear()` ir metama domeninė klaida.

### Kaip išsaugoti esybę po `OptimisticLockException`?

- Reikia iš naujo perskaityti naujausią esybės būseną iš DB (su nauja `version`), pritaikyti pakeitimus ir kartoti `PUT` su atnaujinta `version`.

## Asinchroninis komunikavimas

### Startuoti ilgą darbą

```powershell
$body = @{ sleepSeconds = 8 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/async/tasks" -ContentType "application/json" -Body $body
```

### Tikrinti būseną

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/async/tasks/{taskId}"
```

### Ar asinchroninis komponentas įsijungia į kvietėjo transakciją?

- Ne. Asinchroninis darbas vyksta kitame threade ir netęsia kvietėjo transakcijos konteksto.

### Ar asinchroninis komponentas gali naudoti `@RequestScoped EntityManager`?

- Ne, nes `@RequestScoped` kontekstas susietas su HTTP request gyvenimo ciklu.
- Asinchroniniam darbui reikia atskiro, tinkamai valdomo transakcinio/EM konteksto.

## CDI demonstracija

Tikrinimo endpoint:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/cdi"
```

Ko tikėtis:

- `alternative`: `GREITAS_ALTERNATIVE` (parinkta `beans.xml`).
- `specialization`: `SPECIALIZED` (aktyvus `@Specializes`).
- `interceptorDecorator`: grąžinime matysis dekoratoriaus prierašas `(dekoruota)`, o serverio loge bus `AuditInterceptor` įrašas.

## Įjungimas/išjungimas per beans.xml

Failas: `src/main/webapp/WEB-INF/beans.xml`

- `Alternative` valdomas `<alternatives>` bloke.
- Interceptor valdomas `<interceptors>` bloke.
- Decorator valdomas `<decorators>` bloke.

