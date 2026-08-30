[English](README.md) | **Magyar**

# score-service

A Mini Arcade Portal pontszám- és ranglista-szolgáltatása. Ellenőrzi és
elmenti a játékok pontszámait, és nehézségi szint szerinti ranglistákat
szolgál ki.

## Stack

- Java 21, Spring Boot 4
- Spring Security + JJWT (az auth-service által kiállított JWT-k
  ellenőrzése)
- Spring Data JPA + PostgreSQL
- springdoc-openapi (Swagger UI)
- Testcontainers az integrációs tesztekhez

## Végpontok

- `POST /api/scores/sessions` — játékmenet indítása (anti-cheat token)
- `POST /api/scores` — pontszám beküldése
- `GET /api/scores` — ranglista
- `GET /api/scores/me` — a bejelentkezett felhasználó pontszámai

Teljes API dokumentáció: `http://localhost:8082/swagger-ui.html`

## Futtatás lokálisan

A szolgáltatáshoz PostgreSQL és egy közös `JWT_SECRET` kell, ezért a
legegyszerűbb az egész stacket elindítani az [`infra`](../infra) mappából:

```bash
cd ../infra
docker compose up --build
```

Ezután a score-service a `http://localhost:8082` címen érhető el.

Alternatívaként a szolgáltatás önállóan is futtatható egy saját PostgreSQL
példány ellen:

```bash
./mvnw spring-boot:run
```

## Tesztek

```bash
./mvnw test
```

Az integrációs tesztek Testcontainers segítségével indítanak PostgreSQL-t —
nincs szükség lokális adatbázisra.
