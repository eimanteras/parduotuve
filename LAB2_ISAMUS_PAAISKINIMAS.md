# 2 laboratorinis darbas - išsamus paaiškinimas

## Tikslas

Šis failas skirtas ne kodui, o supratimui: ką tiksliai reikia pademonstruoti atsiskaityme, kodėl tai vyksta, ir kaip tai aiškiai paaiškinti dėstytojui.

---

## 1) Optimistinis rakinimas (`OptimisticLockException`)

### Kas tai yra

Optimistinis rakinimas reiškia, kad du naudotojai gali skaityti tą pačią eilutę vienu metu, bet įrašant pakeitimus tikrinama versija (`@Version`).

- Jei versija sutampa -> `UPDATE` pavyksta.
- Jei versija nebesutampa (kažkas jau pakeitė įrašą) -> gaunamas `OptimisticLockException`.

### Kaip tai realizuota projekte

- Versijos laukas: `src/main/java/lt/eimantas/entity/Produktas.java`
- Atnaujinimas su klaidos gaudymu: `src/main/java/lt/eimantas/service/ParduotuveService.java`
- Konflikto vertimas į HTTP 409: `src/main/java/lt/eimantas/rest/OptimisticConflictExceptionMapper.java`

### Kaip pademonstruoti konfliktą gyvai

1. Gauti produktą su `GET` ir užsirašyti `version`.
2. Išsiųsti 2 `PUT` užklausas su ta pačia sena `version`.
3. Pirma užklausa pavyks, antra gaus `409 Conflict`.

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai/1"
```

```powershell
$body1 = @{ pavadinimas = "Pirmas atnaujinimas"; kaina = 10.00; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/1" -ContentType "application/json" -Body $body1
```

```powershell
$body2 = @{ pavadinimas = "Antras atnaujinimas"; kaina = 11.00; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/1" -ContentType "application/json" -Body $body2
```

### Klausimai atsiskaitymui (labai svarbu)

#### Kas nutinka su einamąja transakcija, kai gaunamas `OptimisticLockException`?

- Transakcija laikoma nesėkminga ir pažymima `rollback-only`.
- Tokioje transakcijoje nebegalima saugiai tęsti normalaus `commit`.

#### Kas nutinka su einamuoju `EntityManager`?

- Einamas `persistence context` po konflikto gali būti nekonsistentinis.
- Praktika: nelaikyti jo patikimu tolesnėms operacijoms tame pačiame sraute.
- Šiame projekte po konflikto kviečiamas `clear()` ir grąžinama klaida klientui.

#### Kaip po to vėl išsaugoti esybę?

Teisingas kelias:

1. Iš naujo perskaityti naujausią būseną iš DB.
2. Pritaikyti vartotojo pakeitimus ant naujausios būsenos.
3. Kartoti atnaujinimą su nauja `version`.

Trumpai: "refresh ir retry su aktualia versija".

---

## 2) Asinchroninis komunikavimas

### Idėja

Ilgas darbas neturi blokuoti HTTP užklausos. Todėl:

- Klientas iškviečia "start" endpointą.
- Serveris grąžina `taskId`.
- Klientas periodiškai tikrina būseną per "status" endpointą.

### Kur tai projekte

- Asinchroninio darbo logika: `src/main/java/lt/eimantas/service/AsyncTaskService.java`
- REST endpointai: `src/main/java/lt/eimantas/rest/AsyncResource.java`

### Demonstravimo komandos

```powershell
$start = @{ sleepSeconds = 8 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/async/tasks" -ContentType "application/json" -Body $start
```

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/async/tasks/{taskId}"
```

### Klausimai atsiskaitymui

#### Ar asinchroninis komponentas gali įsijungti į kvietėjo pradėtą transakciją?

- Ne, nes vykdymas vyksta kitame threade/kontekste.
- Kvietėjo transakcijos kontekstas automatiškai "nenusineša".

#### Ar asinchroninis komponentas gali naudoti `@RequestScoped EntityManager`?

- Praktikoje ne, nes `@RequestScoped` yra susietas su konkretaus HTTP request gyvavimo ciklu.
- Asinchroniniam darbui reikia atskiro tinkamo konteksto ir transakcijų valdymo.

---

## 3) Glass-box extensibility (CDI)

Ši dalis rodo, kaip be didelio kodo perrašymo galima pakeisti elgseną CDI mechanizmais.

### 3.1 `@Alternative`

- Idėja: turėti alternatyvią realizaciją ir ją įjungti konfigūracija.
- Klasės:
  - `src/main/java/lt/eimantas/cdi/PristatymoService.java`
  - `src/main/java/lt/eimantas/cdi/StandartinisPristatymoService.java`
  - `src/main/java/lt/eimantas/cdi/GreitasPristatymoService.java`
- Įjungimas: `src/main/webapp/WEB-INF/beans.xml` per `<alternatives>`.

### 3.2 `@Specializes`

- Idėja: viena bean realizacija "paveldi ir pakeičia" bazinę.
- Klasės:
  - `src/main/java/lt/eimantas/cdi/SkaiciavimoService.java`
  - `src/main/java/lt/eimantas/cdi/SpecializuotasSkaiciavimoService.java`

### 3.3 CDI Interceptor

- Idėja: skersinis funkcionalumas (pvz., audit/log/timing) be verslo kodo teršimo.
- Klasės:
  - `src/main/java/lt/eimantas/cdi/Audited.java`
  - `src/main/java/lt/eimantas/cdi/AuditInterceptor.java`
- Įjungimas/išjungimas: `beans.xml` per `<interceptors>`.

### 3.4 CDI Decorator

- Idėja: apgaubti esamą bean ir papildyti rezultatą/elgseną.
- Klasės:
  - `src/main/java/lt/eimantas/cdi/SveikinimoService.java`
  - `src/main/java/lt/eimantas/cdi/NumatytasSveikinimoService.java`
  - `src/main/java/lt/eimantas/cdi/SveikinimoDecorator.java`
- Įjungimas/išjungimas: `beans.xml` per `<decorators>`.

### Vienas endpoint viskam patikrinti

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/cdi"
```

---

## 4) RESTful paslaugos (`GET`, `POST`, `PUT`)

### Kur realizuota

- JAX-RS aktyvacija: `src/main/java/lt/eimantas/rest/RestApplication.java`
- Resursas: `src/main/java/lt/eimantas/rest/ProduktasResource.java`
- DTO: `src/main/java/lt/eimantas/rest/ProduktasDto.java`

### Ką pademonstruoti

- `GET` - gauti produktų sąrašą arba konkretų produktą.
- `POST` - sukurti naują produktą.
- `PUT` - modifikuoti esamą produktą (su `version` lauku dėl optimistic lock).

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/parduotuve/api/produktai"
```

```powershell
$newBody = @{ pavadinimas = "Naujas"; kaina = 20.00; kategorijaId = 1 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/parduotuve/api/produktai" -ContentType "application/json" -Body $newBody
```

```powershell
$updBody = @{ pavadinimas = "Pakeistas"; kaina = 22.00; kategorijaId = 1; version = 0 } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/parduotuve/api/produktai/1" -ContentType "application/json" -Body $updBody
```

---

## 5) Kaip trumpai kalbėti atsiskaityme (špargalkė)

- "Naudoju `@Version`, todėl konkurentiniai atnaujinimai aptinkami automatiškai." 
- "Kai gaunu `OptimisticLockException`, transakcija nebetinkama commitui, todėl grąžinu `409` ir prašau kliento perskaityti naujausią būseną." 
- "Asinchroniniam darbui grąžinu `taskId`, o klientas daro polling - taip neblokuojamas request." 
- "CDI išplėtimus rodau per `@Alternative`, `@Specializes`, Interceptor ir Decorator, kurie valdomi per `beans.xml`." 
- "REST sluoksnyje turiu pilną minimalų ciklą: `GET`, `POST`, `PUT`."

---

## 6) Ką darysime toliau

Kitas žingsnis: atskirame `.md` faile paaiškinsime jau konkrečiai kiekvieną sukurtą klasę eilutė-po-eilutės logika (kad galėtum ramiai apginti bet kurį klausimą apie kodą).

