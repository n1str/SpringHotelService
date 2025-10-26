# Spring Cloud Hotel Booking System — Система бронирования отелей

Многомодульное распределённое приложение на Spring Boot/Cloud для управления бронированием номеров в отелях:
- API Gateway (Spring Cloud Gateway) для маршрутизации и JWT валидации
- Booking Service (JWT-аутентификация, управление бронированиями, согласованность данных)
- Hotel Management Service (CRUD отелей и номеров, статистика загруженности)
- Eureka Server (Service Registry для динамического обнаружения сервисов)

Все сервисы используют встроенную БД H2. Взаимодействие между сервисами выполняется как последовательность локальных транзакций (без глобальных распределённых транзакций). Два сервиса связаны Saga pattern с компенсацией и полной идемпотентностью.

## Возможности

- Регистрация и вход пользователей (JWT HS512, срок 1 час)
- Создание бронирований с двухшаговой согласованностью (PENDING → CONFIRMED или CANCELLED с освобождением блока)
- Идемпотентность запросов с уникальным requestId
- Повторы с экспоненциальной паузой (1s, 2s, 4s) и таймауты (10 сек) при удалённых вызовах
- Circuit Breaker (Resilience4j) для обработки сбоев Hotel Service
- Рекомендации по выбору номера (сортировка по timesBooked, затем по id)
- Администрирование пользователей (CRUD) и отелей/номеров (CRUD) для админов
- Агрегации: популярность номеров по timesBooked
- Сквозная корреляция запросов с заголовком X-Correlation-Id
- Структурированное логирование с traceId для отладки

## Архитектура и порты

- eureka-server: порт 8761 (Service Registry)
- api-gateway: порт 8080 (маршрутизация и JWT валидация)
- hotel-service: порт 8081 (CRUD отелей и номеров)
- booking-service: порт 8082 (бронирования и аутентификация)

API Gateway маршрутизирует запросы к сервисам через Eureka load balancer и прокидывает заголовок Authorization (JWT) в backend-сервисы. Каждый backend-сервис самостоятельно валидирует JWT.

## Требования

- Java 17+
- Maven 3.9+

## Сборка и запуск

1. Собрать проект:
```bash
mvn clean install -DskipTests
```

2. Запустить Eureka Server (терминал 1):
```bash
mvn -pl eureka-server spring-boot:run
```

3. Запустить API Gateway (терминал 2):
```bash
mvn -pl api-gateway spring-boot:run
```

4. Запустить Hotel Service (терминал 3):
```bash
mvn -pl hotel-service spring-boot:run
```

5. Запустить Booking Service (терминал 4):
```bash
mvn -pl booking-service spring-boot:run
```

После старта сервисы зарегистрируются в Eureka. Проверить статус: http://localhost:8761

## Конфигурация JWT

Для демонстрации используется симметричный ключ HMAC HS512. Секрет задаётся в:
- api-gateway/src/main/resources/application.yml
- hotel-service/src/main/resources/application.yml
- booking-service/src/main/resources/application.yml

Текущее значение в application.yml (dev-окружение):
```
jwt:
  secret: "my-secret-key-for-jwt-token-generation-at-least-256-bits-long-for-HS256-algorithm"
  expiration: 3600000  # 1 час в миллисекундах
```


## Быстрый сценарий (через Gateway на 8080)

Предзагруженные учётные данные:
- admin / admin123 (роль ADMIN)

1. Регистрация нового пользователя:
```bash
curl -X POST http://localhost:8080/user/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"john_doe","password":"password123","email":"john@example.com","fullName":"John Doe"}'
```

Ответ содержит JWT токен:
```json
{
  "id": 1,
  "username": "john_doe",
  "role": "USER",
  "token": "eyJhbGc..."
}
```

2. Вход существующего пользователя:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/user/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"john_doe","password":"password123"}' | jq -r '.token')

echo $TOKEN
```

3. Получить список отелей (USER может):
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/hotels
```

4. Создание отеля (только ADMIN):
```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/user/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Grand Plaza Hotel","address":"123 Main Street, Downtown","description":"Luxurious hotel","starRating":5}'
```

5. Создание номера в отеле (только ADMIN):
```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"hotelId":1,"number":"101","available":true,"roomType":"Standard","pricePerNight":120.00,"capacity":2}'
```

6. Получить рекомендованные номера (сортировка по популярности):
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/rooms/recommend
```

7. Создание бронирования (USER может, idempotent по requestId):
```bash
REQUEST_ID="req-$(date +%s%N)"
curl -X POST http://localhost:8080/booking \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"roomId\":1,\"startDate\":\"2025-11-01\",\"endDate\":\"2025-11-03\",\"autoSelect\":false,\"requestId\":\"$REQUEST_ID\"}"
```

Статус бронирования: PENDING -> CONFIRMED (при успехе) или CANCELLED (при сбое).

8. История бронирований пользователя:
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/bookings
```

9. Получить одно бронирование:
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/booking/1
```

10. Отменить бронирование:
```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/booking/1
```

## Основные эндпойнты

Через API Gateway (порт 8080):

Аутентификация (Booking Service):
- POST /user/register - регистрация нового пользователя (PUBLIC)
- POST /user/auth - получение JWT токена (PUBLIC)
- POST /user - создание пользователя (ADMIN)
- PATCH /user/{id} - обновление пользователя (ADMIN)
- DELETE /user/{id} - удаление пользователя (ADMIN)

Бронирования (Booking Service):
- POST /booking - создать бронирование (USER, с autoSelect или roomId)
- GET /bookings - мои бронирования (USER)
- GET /booking/{id} - получить бронирование по ID (USER)
- DELETE /booking/{id} - отменить бронирование (USER)

Отели (Hotel Service):
- GET /api/hotels - список всех отелей (USER)
- GET /api/hotels/{id} - получить отель по ID (USER)
- POST /api/hotels - создать отель (ADMIN)
- PUT /api/hotels/{id} - обновить отель (ADMIN)
- DELETE /api/hotels/{id} - удалить отель (ADMIN)

Номера (Hotel Service):
- GET /api/rooms - список доступных номеров (USER)
- GET /api/rooms/{id} - получить номер по ID (USER)
- GET /api/rooms/recommend - рекомендованные номера, отсортированные по timesBooked (USER)
- POST /api/rooms - создать номер (ADMIN)
- PUT /api/rooms/{id} - обновить номер (ADMIN)
- DELETE /api/rooms/{id} - удалить номер (ADMIN)
- POST /api/rooms/{id}/confirm-availability - подтвердить доступность (INTERNAL, для Booking Service)
- POST /api/rooms/{id}/release - освободить блок номера (INTERNAL, компенсация)
- POST /api/rooms/{id}/increment-booking - увеличить счётчик бронирований (INTERNAL)

Примечание: эндпойнты marked as INTERNAL не публикуются через Gateway и используются для межсервисной коммуникации.

## Согласованность и надёжность

Saga Pattern с компенсацией:
1. Booking Service создаёт бронирование со статусом PENDING в локальной транзакции
2. Booking Service запрашивает подтверждение доступности у Hotel Service (с retry и timeout)
3. При успехе: бронирование переводится в CONFIRMED, счётчик timesBooked увеличивается
4. При ошибке: бронирование переводится в CANCELLED, и инициируется компенсация (освобождение блока)

Идемпотентность:
- Каждый запрос содержит requestId
- Первое выполнение: создаёт ресурс, сохраняет requestId
- Повторное выполнение: находит существующий ресурс и возвращает его без побочных эффектов
- Это предотвращает создание дубликатов при timeouts и retries

Resilience:
- Retry: максимум 3 попытки с экспоненциальным backoff (1s, 2s, 4s)
- Timeout: 10 секунд на удалённые вызовы Feign
- Circuit Breaker: открывается при 60% ошибок, восстанавливается после 10s

Логирование:
- Структурные логи на каждом критическом шаге
- Сквозная корреляция через X-Correlation-Id (генерируется на Gateway, пробрасывается всем сервисам)
- Уровни: DEBUG (разработка), INFO (важные события), WARN (предупреждения), ERROR (ошибки)

## Консоль H2

H2 консоль включена для обоих сервисов:

Hotel Service:
- URL: http://localhost:8081/h2-console
- JDBC URL: jdbc:h2:mem:hoteldb
- Username: sa
- Password: (пусто)

Booking Service:
- URL: http://localhost:8082/h2-console
- JDBC URL: jdbc:h2:mem:bookingdb
- Username: sa
- Password: (пусто)

## Swagger / OpenAPI

Интерактивная документация API:

- Booking Service UI: http://localhost:8082/swagger-ui.html
- Hotel Service UI: http://localhost:8081/swagger-ui.html
- Через Gateway: http://localhost:8080/swagger-ui.html

Все эндпойнты документированы с примерами и указанием ролей.

## Тестирование

Запуск всех тестов:
```bash
mvn clean test
```

Запуск тестов конкретного модуля:
```bash
mvn test -pl booking-service
mvn test -pl hotel-service
```

Типы тестов:

Unit-тесты (Mockito):
- BookingServiceTest: успешное создание бронирования, обработка ошибок, компенсация, идемпотентность
- RoomBlockServiceTest: проверка hold, confirm, release операций, идемпотентность

Integration-тесты (WebTestClient, real H2):
- BookingIntegrationTest: полный цикл бронирования с Saga pattern
- HotelIntegrationTest: CRUD отелей и номеров, статистика

API-тесты (Python):
```bash
python3 test_api.py http://localhost:8080
```

Покрывает более 20 сценариев аутентификации, CRUD, валидации и обработки ошибок.

## Предзаполненные данные

При запуске сервисов загружаются тестовые данные (data.sql):

Отели (Hotel Service):
- Grand Plaza Hotel (5 звёзд)
- Seaside Resort (4 звезды)
- Mountain Lodge (3 звезды)

Номера: 13 номеров разных типов и ценовых категорий (от 80 до 350 за ночь)

Пользователи (Booking Service):
- admin / admin123 (роль ADMIN)
- john_doe / password123 (роль USER)
- jane_smith / password123 (роль USER)

Все пароли BCrypt-хэшированы.
---

Версия: 1.0.0
Автор: n1str