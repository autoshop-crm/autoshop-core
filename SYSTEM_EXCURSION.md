# AutoShop: текущая архитектура и как это поднимать

Документ собран по фактическому состоянию четырех репозиториев на текущий момент:

- `autoshop-core` — основной бизнес-монолит
- `autoshop-auth` — отдельный сервис аутентификации и авторизации
- `autoshop-notification` — отдельный сервис email-уведомлений
- `autoshop-files` — отдельный сервис хранения файлов поверх MinIO

Ниже описано:

1. как система устроена сейчас;
2. как сервисы взаимодействуют между собой;
3. что именно нужно для локального запуска;
4. как это разумно раскладывать на проде;
5. какие есть реальные эндпоинты, Kafka topics, Redis keys и MinIO buckets;
6. какие есть важные ограничения и нестыковки в текущем состоянии.

## 1. Общая картина

Система сейчас выглядит так:

```text
Frontend / сайт
    |
    v
Reverse proxy / ingress
    |
    +--> autoshop-auth
    |       |
    |       +--> PostgreSQL (auth_db)
    |       +--> Redis (blacklist access token jti)
    |
    +--> autoshop-core
    |       |
    |       +--> PostgreSQL (core DB)
    |       +--> Redis (cache внешних интеграций)
    |       +--> autoshop-auth (/api/auth/validate)
    |       +--> Kafka (publish order events)
    |       +--> UMAPI
    |       +--> Carreta
    |
    +--> autoshop-files
    |       |
    |       +--> PostgreSQL (files_db)
    |       +--> MinIO (file bytes)
    |
    +--> autoshop-notification
            |
            +--> PostgreSQL (notifications_db)
            +--> Kafka (consume order events, DLT)
            +--> SMTP / Mailhog / Mailjet
```

Ключевая идея:

- `core` — это не набор микросервисов, а один монолит с несколькими доменами внутри:
  `customers`, `vehicles`, `orders`, `parts`, `loyalty`, `procurement`, внешние интеграции.
- `auth`, `notification`, `files` — уже вынесены в отдельные сервисы.
- `core` не делает локальную JWT-валидацию сам, а ходит в `auth`.
- `core` не отправляет email напрямую, а публикует события в Kafka.
- `notification` забирает события из Kafka и сам отправляет письма.
- `files` инкапсулирует MinIO и метаданные файлов. Остальные сервисы должны работать через его HTTP API.

## 2. Что является монолитом, а что микросервисом

### 2.1. `autoshop-core` — монолит

Это основной backend приложения. Внутри него уже живут:

- клиенты (`customers`)
- автомобили (`vehicles`)
- заказы (`orders`)
- склад и запчасти (`parts`)
- программа лояльности (`loyalty`)
- закупки (`procurement`)
- интеграция с внешними каталогами и поставщиками (`UMAPI`, `Carreta`)
- публикация order events в Kafka
- проверка access token через внешний `auth`

То есть `core` сейчас — это центр бизнес-логики.

### 2.2. `autoshop-auth` — отдельный auth-сервис

Держит:

- пользователей
- роли
- refresh token’ы
- выпуск JWT access token
- blacklist access token через Redis
- endpoint валидации токена для `core`

### 2.3. `autoshop-notification` — отдельный notification-сервис

Держит:

- Kafka consumer
- inbox/idempotency таблицы
- историю уведомлений и попыток отправки
- шаблоны писем
- SMTP/Mailjet отправку
- DLQ для неуспешных Kafka-сообщений

### 2.4. `autoshop-files` — отдельный file-storage сервис

Держит:

- HTTP API загрузки и скачивания файлов
- PostgreSQL-метаданные файлов
- MinIO buckets/objects
- presigned URL
- soft delete и жизненный цикл статусов файлов

## 3. Как сервисы связаны между собой

### 3.1. Аутентификация

Поток запроса сейчас такой:

1. Пользователь логинится в `autoshop-auth`.
2. `auth` выдает:
   - `accessToken` (JWT)
   - `refreshToken` (случайная строка, хранится в БД)
3. Фронт вызывает `autoshop-core` с `Authorization: Bearer ...`.
4. Фильтр `core` не доверяет токену вслепую, а вызывает `autoshop-auth`:
   `POST /api/auth/validate`.
5. Если `auth` подтверждает валидность токена, `core` пускает запрос дальше и назначает роли.

Важно:

- `core` сейчас зависит от доступности `auth`.
- Если `auth` недоступен, `core` отвечает `503 Service Unavailable`.
- Конфиг `app.auth.enabled` в `core` есть, но по факту не участвует в условном создании клиента и не отключает auth-цепочку сам по себе.

### 3.2. Уведомления по заказам

Поток событий по заказу:

1. В `core` создается заказ или меняется его статус.
2. Внутри `core` публикуется доменное событие.
3. После коммита транзакции `core` публикует Kafka message в topic `autoshop.order-events`.
4. `notification` читает это сообщение.
5. `notification`:
   - валидирует envelope;
   - пишет `eventId` в inbox;
   - рендерит шаблон;
   - отправляет email;
   - сохраняет результат;
   - при ошибках ретраит;
   - при исчерпании ретраев перекладывает сообщение в DLT.

Типы событий:

- `ORDER_CREATED`
- `ORDER_STATUS_CHANGED`
- `ORDER_COMPLETED`

### 3.3. Файлы

Логически задуман такой поток:

1. `core` или frontend загружает файл в `autoshop-files`.
2. `files` сохраняет байты в MinIO.
3. `files` сохраняет метаданные в PostgreSQL.
4. `core` хранит у себя только ссылку/`fileId` или запрашивает список файлов по владельцу.

Но важный факт текущего состояния:

- в `core` сейчас нет живой интеграции с `autoshop-files`;
- в `core` есть MinIO-настройки (`app.minio.*`), но в коде прямого MinIO-клиента или использования `files` API нет;
- то есть file-service уже написан, но в общую рабочую цепочку `core` еще не вшит.

## 4. Что делает каждый сервис подробно

## 4.1. `autoshop-core`

### Основная роль

Это центр всей предметной логики автосервиса.

### Главные домены внутри `core`

#### `customers`

- создание клиента
- чтение клиента
- обновление
- удаление
- поиск по email / телефону / имени / фамилии

#### `vehicles`

- создание машины, привязанной к клиенту
- поиск по id / VIN / customerId
- обновление машины
- удаление
- привязка машины к UMAPI catalog context

#### `orders`

- создание заказа
- обновление описания проблемы
- назначение сотрудника
- изменение сметы
- смена статуса
- получение заказов по клиенту / машине / статусу

#### `parts`

- локальный каталог деталей в БД
- обновление остатков
- резервирование деталей под заказ
- списание при завершении заказа
- возврат резерва при отмене
- поиск аналогов и каталожных деталей через UMAPI

#### `loyalty`

- создание/получение loyalty account клиента
- начисление баллов при завершении заказа
- списание баллов в заказ
- пересчет допустимого списания при изменении сметы/деталей
- возврат баллов при отмене заказа

#### `procurement`

- поиск supplier quotes через Carreta
- создание purchase order на основе выбранной quote
- приемка поставки и пополнение склада

### Как `core` работает внутри на примере заказа

#### Создание заказа

При `POST /api/orders` сервис:

1. проверяет существование клиента;
2. проверяет существование автомобиля;
3. проверяет, что машина действительно принадлежит этому клиенту;
4. при наличии `employeeId` проверяет, что сотрудник может быть назначен (`MECHANIC` или `MANAGER`);
5. создает заказ в статусе `NEW`;
6. инициализирует финансовые поля;
7. сохраняет заказ;
8. формирует payload `ORDER_CREATED`;
9. публикует доменное событие;
10. после коммита транзакции отправляет Kafka event.

#### Добавление деталей в заказ

При `POST /api/orders/{orderId}/parts`:

1. заказ должен быть в `NEW` или `IN_PROGRESS`;
2. деталь ищется с блокировкой;
3. проверяется доступный остаток = `stockQuantity - reservedQuantity`;
4. количество резервируется;
5. создается `OrderPartItem`;
6. пересчитываются `partsTotal`, `costsTotal`, `discountAmount`, `finalAmount`;
7. пересчитывается допустимое количество списываемых loyalty points.

#### Смена статуса заказа

При `PUT /api/orders/{id}/status`:

- если новый статус `COMPLETED`:
  - резерв деталей превращается в реальное списание со склада;
  - начисляются loyalty points;
  - ставится `completedAt`;
  - публикуются события `ORDER_STATUS_CHANGED` и `ORDER_COMPLETED`;
- если новый статус `CANCELLED`:
  - резерв деталей освобождается;
  - ранее примененные баллы возвращаются;
  - публикуется `ORDER_STATUS_CHANGED`.

### Внешние интеграции внутри `core`

#### `auth`

Используется для проверки access token через `POST /api/auth/validate`.

#### `UMAPI`

Используется для:

- поиска аналогов деталей;
- дерева производителей / серий / модификаций;
- поиска product groups;
- поиска catalog articles.

`core` оборачивает UMAPI вызовы в retry и Redis cache.

#### `Carreta`

Используется для:

- поиска supplier quotes;
- опционального создания внешнего заказа поставщику.

Тоже оборачивается retry и Redis cache для quote search.

### Redis в `core`

Redis здесь используется как интеграционный кэш, а не как storage сессий.

Примеры реальных key-pattern’ов:

```text
umapi:parts:search:v1:lang=ru:region=WWW:mode=analogs:article=90915YZZJ1:brand=TOYOTA:limit=10:offset=0
umapi:catalog:manufacturers:ru:WWW:PC:true
umapi:catalog:model-series:ru:WWW:PC:123
umapi:catalog:modifications:ru:WWW:PC:456
umapi:catalog:fuse:ru:WWW:PC:789
umapi:catalog:product-groups-search:ru:WWW:PC:789:1c9d...
umapi:catalog:articles:ru:WWW:PC:789:1001,1002:ANY:10:0
carreta:quotes:search:v1:account=default:query=90915YZZJ1
```

Поведение:

- сначала читается кэш;
- если внешнее API доступно, ответ обновляет кэш;
- если внешнее API упало, сервис пытается вернуть stale-кэш как fallback;
- в DTO это отражается флагами `cached` и `fallback`.

### Kafka в `core`

`core` — producer order notifications.

Topic по умолчанию:

```text
autoshop.order-events
```

Key:

```text
eventId
```

Event envelope:

```json
{
  "eventId": "8f2cb0f6-41f0-4b79-a4d8-73d0d862fa33",
  "eventType": "ORDER_CREATED",
  "occurredAt": "2026-04-19T10:15:30Z",
  "source": "autoshop-core",
  "version": 1,
  "correlationId": "order-42-created",
  "payload": {
    "orderId": 42,
    "orderNumber": "AS-2026-00042",
    "customerId": 7,
    "customerFirstName": "Ivan",
    "customerLastName": "Petrov",
    "customerEmail": "ivan@example.com",
    "vehicleId": 12,
    "vehicleBrand": "Toyota",
    "vehicleModel": "Camry",
    "vehiclePlateNumber": "A123BC77",
    "createdAt": "2026-04-19T10:15:30Z"
  }
}
```

Публикацию можно выключить:

```text
APP_EVENTS_ORDER_NOTIFICATIONS_ENABLED=false
```

Тогда вместо Kafka publisher будет использоваться no-op publisher.

### Security и роли в `core`

`core` мапит роли из `auth` на Spring Security роли:

- `ADMIN`
- `MANAGER`
- `MECHANIC`
- `RECEPTIONIST`
- `CLIENT`

Доступ грубо распределен так:

- `customers` — в основном `ADMIN`, `MANAGER`, `RECEPTIONIST`
- `vehicles` — `ADMIN`, `MANAGER`, `RECEPTIONIST`, частично `MECHANIC`
- `orders` — чтение шире, изменение зависит от операции
- `parts` — чтение широко, запись в основном `ADMIN`/`MANAGER`
- `procurement` — `ADMIN`/`MANAGER`, частично `RECEPTIONIST`
- `loyalty` — `ADMIN`/`MANAGER`/`RECEPTIONIST`

## 4.2. `autoshop-auth`

### Основная роль

Отдельный сервис identity/authentication для всего проекта.

### Что хранит

- таблицу `users`
- таблицу `roles`
- join-таблицу `user_roles`
- таблицу `refresh_tokens`

### Как работает auth-модель

#### Register

- пользователь регистрируется;
- ему автоматически дается роль `CLIENT`;
- пароль хэшируется;
- access token сразу не выдается, только создается пользователь.

#### Login

- сервис аутентифицирует email/password;
- генерирует JWT access token;
- создает refresh token в БД;
- возвращает оба токена.

#### Refresh

- refresh token проверяется в БД;
- старый refresh token ревокается;
- создается новый refresh token;
- создается новый access token.

#### Logout

- refresh token ревокается в БД;
- `jti` access token кладется в Redis blacklist до истечения TTL токена.

#### Validate

- JWT парсится;
- проверяется `type=access`;
- проверяется blacklist в Redis;
- проверяется существование и активность пользователя;
- возвращается DTO для `core`.

### Redis в `auth`

Redis здесь используется для blacklist access token.

Формат ключа:

```text
blacklist:access:{jti}
```

Пример:

```text
blacklist:access:9d09c4e9-1f74-475d-91b2-732d8d7294ee
```

TTL ключа равен оставшемуся времени жизни access token.

### JWT access token

Внутри токена лежит:

- `jti`
- `sub` = userId
- `email`
- `roles`
- `type=access`
- `issuedAt`
- `expiration`

### Bootstrap и dev users

Что уже есть:

- Liquibase сидит роли:
  - `ADMIN`
  - `MANAGER`
  - `MECHANIC`
  - `RECEPTIONIST`
  - `CLIENT`
- в профилях `dev`/`test` есть сидирование дев-аккаунтов:
  - `admin@autoshop.local / Admin123!`
  - `manager@autoshop.local / Manager123!`
  - `reception@autoshop.local / Reception123!`
  - `mechanic@autoshop.local / Mechanic123!`
  - `client@autoshop.local / Client123!`
- в `dev`/`test` также есть optional bootstrap-user initializer.

Важно:

- bootstrap initializer не работает в `prod`, только в `dev`/`test`;
- значит на проде первого администратора надо создавать отдельно осознанно.

## 4.3. `autoshop-notification`

### Основная роль

Сервис не предоставляет бизнес-HTTP API. Его главная задача — брать order events из Kafka и отправлять email.

HTTP endpoints у него только технические:

- `GET /actuator/health`
- `GET /actuator/info`

### Входной транспорт

Consumer:

- topic: `autoshop.order-events`
- group: `notification-service`
- DLT: `autoshop.order-events.dlt`

### Как он обрабатывает событие

1. читает Kafka message;
2. десериализует JSON в envelope;
3. валидирует `eventId`, `eventType`, `source`, `version`, `payload`;
4. пишет запись в `notification_event_inbox`;
5. если событие уже было успешно обработано, пропускает его;
6. ищет/создает `notification`;
7. рендерит шаблон письма;
8. отправляет письмо;
9. сохраняет provider metadata;
10. фиксирует `PROCESSED`.

### Защита от дублей

Есть две линии идемпотентности:

- inbox по `eventId`
- уникальность уведомления по `(event_id, channel)`

То есть повторная доставка Kafka не должна приводить к повторной успешной отправке email, если письмо уже ушло.

### Retry и DLT

Есть два уровня retry:

#### Email retry

Если упала отправка email:

- сервис повторяет попытки отправки письма;
- количество попыток и backoff настраиваются через `app.retry.email.*`.

#### Kafka retry

Если процессинг события в целом не удался:

- `DefaultErrorHandler` повторяет обработку Kafka record;
- после исчерпания попыток запись уходит в DLT topic.

Non-retryable исключения:

- невалидный JSON;
- unsupported event version;
- unsupported event type;
- отсутствие обязательных полей payload;
- отсутствие активного шаблона.

### Шаблоны

В БД сидируются активные email templates:

- `ORDER_CREATED_EMAIL`
- `ORDER_STATUS_CHANGED_EMAIL`
- `ORDER_COMPLETED_EMAIL`

HTML-файлы лежат в ресурсах сервиса.

### Поставщики email

Поддержаны два режима:

#### SMTP

Подходит для локалки и простого прод-контура.

#### Mailjet

Подходит для боевого прод-отправления.

Есть sandbox mode:

```text
MAILJET_SANDBOX_MODE=true
```

## 4.4. `autoshop-files`

### Основная роль

Это abstraction boundary над MinIO.

Идея сервиса правильная и важная:

- только `files` знает про buckets, object keys, presigned URLs и byte storage;
- остальные сервисы не должны ходить в MinIO напрямую.

### Как работает upload

1. клиент отправляет multipart upload;
2. сервис валидирует category, ownerType, ownerId, contentType, extension, size, filename;
3. генерирует `fileId`;
4. вычисляет SHA-256;
5. выбирает bucket по `category`;
6. генерирует object key;
7. пишет метаданные в PostgreSQL со статусом `PENDING`;
8. грузит байты в MinIO;
9. если успех — статус `AVAILABLE`;
10. если неуспех — статус `UPLOAD_FAILED`.

### Bucket strategy

Категории маппятся в bucket’ы:

| Category | Bucket |
|---|---|
| `ORDER_DOCUMENT` | `documents` |
| `ORDER_ESTIMATE` | `estimates` |
| `ORDER_INSPECTION_PHOTO` | `car-inspections` |
| `VEHICLE_PHOTO` | `car-inspections` |
| `VEHICLE_DOCUMENT` | `documents` |
| `CUSTOMER_DOCUMENT` | `documents` |
| `CUSTOMER_AVATAR` | `avatars` |
| `EMPLOYEE_AVATAR` | `avatars` |
| `INVOICE` | `estimates` |
| `REPORT` | `estimates` |

### OwnerType

Допустимые владельцы:

- `VEHICLE`
- `ORDER`
- `CUSTOMER`
- `CLIENT`
- `EMPLOYEE`
- `PART`
- `PURCHASE_ORDER`
- `SYSTEM`

`CLIENT` оставлен как совместимость с текущим неймингом `core`.

### Жизненный цикл файла

Статусы:

- `PENDING`
- `AVAILABLE`
- `UPLOAD_FAILED`
- `DELETED`

Delete — soft delete на уровне метаданных, но при `AVAILABLE` объект удаляется и из MinIO.

### Presigned URL

Сервис умеет генерировать presigned download URL.

Ограничения:

- минимум: `60` секунд
- дефолт: `900` секунд
- максимум: `3600` секунд на уровне сервиса

### Access policy

Сейчас политика доступа — `AllowAllFileAccessPolicy`.

Это означает:

- upload разрешен всем;
- read разрешен всем;
- delete разрешен всем.

Для prod это временное состояние. Без интеграции с auth/core это еще не законченный security boundary.

### MinIO buckets bootstrap

На старте сервиса `files` сам гарантирует создание bucket’ов, если включен:

```text
MINIO_INITIALIZE_BUCKETS=true
```

### Пример object key

Формат:

```text
{category-lowercase}/{yyyy}/{MM}/{dd}/{fileId}/{baseName}-{sha8}.{ext}
```

Пример:

```text
order-document/2026/04/24/4ec2f0a6-2d5f-4b24-8a57-a24e4a8f7b90/invoice-2f4a9c1b.pdf
```

## 5. Реальные API эндпоинты

Ниже — каталог того, что реально есть в коде сейчас.

## 5.1. `autoshop-auth`

Базовый URL локально:

```text
http://localhost:8082
```

### Публичные endpoints

#### `POST /api/auth/register`

Request:

```json
{
  "email": "client@example.com",
  "password": "Client123!",
  "firstName": "Ivan",
  "lastName": "Petrov"
}
```

#### `POST /api/auth/login`

Request:

```json
{
  "email": "manager@autoshop.local",
  "password": "Manager123!"
}
```

Response:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "2e5d9f9a-...",
  "tokenType": "Bearer",
  "accessExpiresIn": 900,
  "refreshExpiresIn": 604800,
  "userId": 2,
  "email": "manager@autoshop.local",
  "roles": ["MANAGER"]
}
```

#### `POST /api/auth/refresh`

Request:

```json
{
  "refreshToken": "2e5d9f9a-..."
}
```

### Требуют access token

#### `POST /api/auth/logout`

Request:

```json
{
  "refreshToken": "2e5d9f9a-..."
}
```

#### `POST /api/auth/validate`

Используется `core`.

Response:

```json
{
  "valid": true,
  "userId": 2,
  "email": "manager@autoshop.local",
  "roles": ["MANAGER"],
  "tokenType": "access",
  "jti": "9d09c4e9-1f74-475d-91b2-732d8d7294ee",
  "expiresAt": "2026-04-24T10:15:30Z",
  "message": null
}
```

#### `POST /api/auth/verify-token`

Фактически alias к `validate`.

#### `GET /api/auth/me`

Возвращает текущего пользователя по токену.

### Админский endpoint

#### `POST /api/admin/users`

Request:

```json
{
  "email": "boss@example.com",
  "password": "Boss12345!",
  "firstName": "Big",
  "lastName": "Boss",
  "roles": ["ADMIN", "MANAGER"]
}
```

## 5.2. `autoshop-core`

Базовый URL локально:

```text
http://localhost:8080
```

### Customers

#### `POST /api/customers`

```json
{
  "firstName": "Ivan",
  "lastName": "Petrov",
  "phoneNumber": "+79991234567",
  "email": "ivan@example.com"
}
```

#### `GET /api/customers/{id}`

#### `PUT /api/customers/{id}`

```json
{
  "firstName": "Ivan",
  "lastName": "Petrov",
  "phoneNumber": "+79991234568",
  "email": "ivan.new@example.com"
}
```

#### `DELETE /api/customers/{id}`

#### `GET /api/customers/search?email=...&phoneNumber=...&firstName=...&lastName=...`

### Vehicles

#### `POST /api/vehicles`

```json
{
  "customerId": 1,
  "brand": "Toyota",
  "model": "Camry",
  "vin": "JTNBE46K173012345",
  "licensePlate": "A123BC77"
}
```

#### `GET /api/vehicles/{id}`

#### `GET /api/vehicles/vin/{vin}`

#### `GET /api/vehicles/customer/{customerId}`

#### `PUT /api/vehicles/{id}`

```json
{
  "brand": "Toyota",
  "model": "Camry XV70",
  "licensePlate": "A777BC77"
}
```

#### `PUT /api/vehicles/{id}/catalog-link`

```json
{
  "type": "PC",
  "manufacturerId": 171,
  "manufacturerName": "Toyota",
  "modelSeriesId": 1045,
  "modelSeriesName": "Camry",
  "modificationId": 998877,
  "modificationName": "2.5 Hybrid",
  "engineDescription": "A25A-FXS"
}
```

#### `DELETE /api/vehicles/{id}/catalog-link`

#### `DELETE /api/vehicles/{id}`

### Orders

#### `POST /api/orders`

```json
{
  "customerId": 1,
  "vehicleId": 3,
  "employeeId": 2,
  "problem": "Посторонний шум в подвеске"
}
```

#### `GET /api/orders/{id}`

#### `PUT /api/orders/{id}`

```json
{
  "problem": "Посторонний шум в подвеске и вибрация при торможении"
}
```

#### `PUT /api/orders/{id}/assign`

```json
{
  "employeeId": 5
}
```

#### `PUT /api/orders/{id}/estimate`

```json
{
  "laborTotal": 4500.00,
  "discountAmount": 500.00
}
```

#### `PUT /api/orders/{id}/status`

```json
{
  "status": "IN_PROGRESS"
}
```

Доступные статусы:

- `NEW`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

#### `GET /api/orders/customer/{customerId}`

#### `GET /api/orders/vehicle/{vehicleId}`

#### `GET /api/orders/status/{status}`

### Loyalty

#### `GET /api/loyalty/accounts/customer/{customerId}`

#### `GET /api/loyalty/accounts/{accountId}/transactions`

#### `GET /api/loyalty/tiers`

#### `PUT /api/orders/{orderId}/loyalty/spend`

```json
{
  "points": 500
}
```

#### `DELETE /api/orders/{orderId}/loyalty/spend`

### Parts

#### `POST /api/parts`

```json
{
  "brand": "Toyota",
  "name": "Oil filter",
  "articleNumber": "90915YZZJ1",
  "cost": 550.00
}
```

#### `GET /api/parts/{id}`

#### `PUT /api/parts/{id}`

```json
{
  "name": "Oil filter original",
  "cost": 590.00
}
```

#### `PUT /api/parts/{id}/stock`

```json
{
  "stockQuantity": 15
}
```

#### `DELETE /api/parts/{id}`

#### `GET /api/parts?articleNumber=...&brand=...&name=...&availableOnly=true`

### Parts inside order

#### `POST /api/orders/{orderId}/parts`

```json
{
  "partId": 10,
  "quantity": 2
}
```

#### `GET /api/orders/{orderId}/parts`

#### `PUT /api/orders/{orderId}/parts/{itemId}`

```json
{
  "quantity": 3
}
```

#### `DELETE /api/orders/{orderId}/parts/{itemId}`

### UMAPI-backed part search

#### `GET /api/parts/external/search?articleNumber=90915YZZJ1&brand=TOYOTA&limit=10&offset=0`

#### `GET /api/parts/catalog/manufacturers?type=PC&popular=true`

#### `GET /api/parts/catalog/model-series?type=PC&manufacturerId=171`

#### `GET /api/parts/catalog/modifications?type=PC&modelSeriesId=1045`

#### `GET /api/parts/catalog/product-groups/search?type=PC&modificationId=998877&query=filter`

#### `GET /api/parts/catalog/articles?type=PC&modificationId=998877&productGroupIds=1001,1002&supplierId=12&limit=10&offset=0`

### Order-bound catalog search

#### `GET /api/orders/{orderId}/parts/catalog/product-groups/search?...`

#### `GET /api/orders/{orderId}/parts/catalog/articles?...`

Это те же каталожные поиски, но с привязкой к конкретному заказу/автомобилю.

### Procurement

#### `GET /api/procurement/supplier-quotes/search?query=90915YZZJ1`

#### `POST /api/procurement/purchase-orders`

```json
{
  "quote": {
    "positionSignature": "sig-123",
    "articleNumber": "90915YZZJ1",
    "brand": "TOYOTA",
    "name": "Oil filter",
    "purchasePrice": 400.00,
    "deliveryDaysMin": 1,
    "deliveryDaysMax": 3,
    "minOrderQuantity": 1,
    "quantityRaw": "10"
  },
  "quantity": 2,
  "salePrice": 550.00,
  "clientComment": "Заказать срочно",
  "createExternalOrder": true
}
```

#### `POST /api/procurement/stock-receipts`

```json
{
  "targetPartId": 10,
  "receivedQuantity": 5,
  "salePrice": 590.00
}
```

### Technical

#### `GET /actuator/health`

## 5.3. `autoshop-files`

Базовый URL локально:

```text
http://localhost:8083
```

Но смотри раздел с граблями: у `notification` тот же дефолтный порт.

### Upload

#### `POST /api/files`

Multipart fields:

- `file`
- `category`
- `ownerType`
- `ownerId`
- `uploadedBy` optional

Пример:

```bash
curl -X POST http://localhost:8083/api/files \
  -F 'category=ORDER_DOCUMENT' \
  -F 'ownerType=ORDER' \
  -F 'ownerId=42' \
  -F 'uploadedBy=employee-1' \
  -F 'file=@/tmp/order.pdf;type=application/pdf'
```

### Metadata

#### `GET /api/files/{fileId}`

### Owner listing

#### `GET /api/files?ownerType=ORDER&ownerId=42&includeDeleted=false&page=0&size=20`

### Download

#### `GET /api/files/{fileId}/download`

### Presigned URL

#### `POST /api/files/{fileId}/presigned-download-url`

Request:

```json
{
  "ttlSeconds": 900
}
```

### Delete

#### `DELETE /api/files/{fileId}`

### Technical

#### `GET /actuator/health`

#### `GET /actuator/info`

## 5.4. `autoshop-notification`

Базовый URL локально:

```text
http://localhost:8083
```

Реально exposed HTTP endpoints:

- `GET /actuator/health`
- `GET /actuator/info`

Бизнес-функция сервиса идет не через REST, а через Kafka.

## 6. Kafka: как это устроено сейчас

## 6.1. Producer и consumer

Producer:

- `autoshop-core`

Consumer:

- `autoshop-notification`

Topics:

- основной: `autoshop.order-events`
- DLT: `autoshop.order-events.dlt`

Consumer group:

- `notification-service`

## 6.2. Что публикуется

Типовой envelope:

```json
{
  "eventId": "2f7aa0b0-58c4-4a6f-a9d8-2791a44d17dd",
  "eventType": "ORDER_STATUS_CHANGED",
  "occurredAt": "2026-04-24T09:30:00Z",
  "source": "autoshop-core",
  "version": 1,
  "correlationId": "order-42-status-changed",
  "payload": {
    "orderId": 42,
    "orderNumber": "AS-2026-00042",
    "customerId": 1,
    "customerFirstName": "Ivan",
    "customerLastName": "Petrov",
    "customerEmail": "ivan@example.com",
    "previousStatus": "NEW",
    "newStatus": "IN_PROGRESS",
    "changedAt": "2026-04-24T09:30:00Z",
    "managerComment": ""
  }
}
```

## 6.3. Полезные команды Kafka

Подписаться на основной topic:

```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic autoshop.order-events \
  --from-beginning
```

Подписаться на DLT:

```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic autoshop.order-events.dlt \
  --from-beginning
```

## 7. Redis: как используется сейчас

## 7.1. Redis usage map

`autoshop-auth`:

- blacklist access token по `jti`

`autoshop-core`:

- кэш внешних API UMAPI / Carreta

`autoshop-notification`:

- Redis не использует

`autoshop-files`:

- Redis не использует

## 7.2. Реальные примеры ключей

Blacklist:

```text
blacklist:access:9d09c4e9-1f74-475d-91b2-732d8d7294ee
```

UMAPI analogs:

```text
umapi:parts:search:v1:lang=ru:region=WWW:mode=analogs:article=90915YZZJ1:brand=TOYOTA:limit=10:offset=0
```

UMAPI manufacturers:

```text
umapi:catalog:manufacturers:ru:WWW:PC:true
```

Carreta quotes:

```text
carreta:quotes:search:v1:account=default:query=90915YZZJ1
```

## 7.3. Полезные команды Redis

Проверить blacklist:

```bash
redis-cli -p 6379 KEYS 'blacklist:access:*'
```

Посмотреть UMAPI cache:

```bash
redis-cli -p 6379 KEYS 'umapi:*'
```

Посмотреть Carreta cache:

```bash
redis-cli -p 6379 KEYS 'carreta:*'
```

Прочитать значение:

```bash
redis-cli -p 6379 GET 'carreta:quotes:search:v1:account=default:query=90915YZZJ1'
```

## 8. MinIO: как используется сейчас

## 8.1. Кто работает с MinIO

Только `autoshop-files`.

`core` сейчас MinIO не использует, несмотря на наличие настроек `app.minio.*`.

## 8.2. Buckets

- `car-inspections`
- `documents`
- `avatars`
- `estimates`

## 8.3. Полезные endpoints MinIO

API:

```text
http://localhost:9000
```

Console:

```text
http://localhost:9001
```

## 8.4. Полезные MinIO / S3 сценарии

Проверка bucket’ов через console:

- зайти на `http://localhost:9001`
- логин/пароль из `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`

Получение файла лучше делать через `autoshop-files`, а не напрямую в MinIO.

Если нужен временный доступ снаружи:

- получать presigned URL через `POST /api/files/{fileId}/presigned-download-url`

## 9. Как поднимать локально

Ниже — рекомендованный путь, чтобы все реально заработало вместе.

## 9.1. Шаг 1. Поднять инфраструктуру

Самый удобный общий локальный контур — использовать compose из `autoshop-core`:

```bash
cd /Users/vladislavkovrigin/Projects/IdeaProjects/autoshop-core
docker compose --profile messaging up -d
```

Это поднимет:

- PostgreSQL на `localhost:5433`
- Redis на `localhost:6379`
- Kafka на `localhost:9092`
- MinIO на `localhost:9000`
- MinIO Console на `localhost:9001`
- Mailhog SMTP на `localhost:1025`
- Mailhog UI на `localhost:8025`

## 9.2. Шаг 2. Создать базы данных

Нужны отдельные БД:

- `postgres` или `core` БД для `autoshop-core`
- `auth_db`
- `notifications_db`
- `files_db`

Пример:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE notifications_db;
CREATE DATABASE files_db;
```

## 9.3. Шаг 3. Запустить `autoshop-auth`

Нужно обратить внимание на URL БД.

По умолчанию `auth` смотрит в:

```text
jdbc:postgresql://localhost:5432/auth_db
```

А shared compose поднимает Postgres на `5433`.

Поэтому запускать так:

```bash
cd /Users/vladislavkovrigin/Projects/IdeaProjects/autoshop-auth
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/auth_db \
SPRING_DATASOURCE_USERNAME=autoshop-admin \
SPRING_DATASOURCE_PASSWORD=pass \
REDIS_HOST=localhost \
REDIS_PORT=6379 \
./gradlew bootRun
```

Если нужны дев-аккаунты, лучше запустить с профилем `dev`.

## 9.4. Шаг 4. Запустить `autoshop-core`

```bash
cd /Users/vladislavkovrigin/Projects/IdeaProjects/autoshop-core
./gradlew bootRun --args='--spring.profiles.active=local'
```

`core` ожидает:

- PostgreSQL `localhost:5433`
- Redis `localhost:6379`
- Kafka `localhost:9092`
- `auth` на `localhost:8082`

## 9.5. Шаг 5. Запустить `autoshop-notification`

```bash
cd /Users/vladislavkovrigin/Projects/IdeaProjects/autoshop-notification
./gradlew bootRun --args='--spring.profiles.active=local'
```

Он ожидает:

- PostgreSQL `localhost:5433/notifications_db`
- Kafka `localhost:9092`
- SMTP `localhost:1025`

## 9.6. Шаг 6. Запустить `autoshop-files`

По умолчанию `files` тоже сидит на `8083`, как и `notification`.

Чтобы они не конфликтовали локально, лучше явно сменить порт:

```bash
cd /Users/vladislavkovrigin/Projects/IdeaProjects/autoshop-files
export FILES_DB_URL=jdbc:postgresql://localhost:5433/files_db
export FILES_DB_USERNAME=autoshop-admin
export FILES_DB_PASSWORD=pass
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export SERVER_PORT=8084
./gradlew bootRun
```

Тогда получится:

- `auth` — `8082`
- `core` — `8080`
- `notification` — `8083`
- `files` — `8084`

## 9.7. Проверка контура

Проверить health:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Проверить Mailhog:

```text
http://localhost:8025
```

Проверить MinIO Console:

```text
http://localhost:9001
```

## 10. Как это раскладывать на проде

## 10.1. Рекомендуемая прод-схема

На проде логично поднимать это как 4 отдельных backend-сервиса:

- `autoshop-auth`
- `autoshop-core`
- `autoshop-notification`
- `autoshop-files`

И 5 инфраструктурных зависимостей:

- PostgreSQL
- Redis
- Kafka
- MinIO
- SMTP provider / Mailjet

### Лучше всего разложить так

- один ingress/reverse proxy наружу
- отдельный host/path routing до каждого backend
- отдельные database schema/DB per service
- один Redis cluster/instance
- один Kafka cluster
- один MinIO instance или S3-compatible storage

## 10.2. Какие публичные маршруты реально нужны с сайта

Снаружи обычно нужны:

- `auth`
- `core`
- `files`

`notification` наружу почти не нужен, кроме internal monitoring/actuator.

## 10.3. Прод-конфиги, которые обязательны

### Для `auth`

- сильный `JWT_SECRET`
- реальный Postgres
- Redis
- корректный bootstrap/seed процесс для первого администратора

### Для `core`

- `APP_AUTH_BASE_URL`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATASOURCE_URL`
- реальные `UMAPI` / `Carreta` credentials

### Для `notification`

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_DATASOURCE_URL`
- либо SMTP, либо Mailjet credentials
- `APP_MAIL_FROM`

### Для `files`

- `FILES_DB_URL`
- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- при необходимости отдельный bucket policy / backups / lifecycle

## 10.4. Что важно для production hardening

### Auth

- хранить `JWT_SECRET` только в secret storage
- включить TLS на внешнем периметре
- ограничить admin endpoints
- мониторить Redis, потому что logout/blacklist от него зависит

### Core

- выставить таймауты и retry-policy осознанно
- мониторить доступность `auth`
- мониторить Kafka publish failures
- отделить prod credentials UMAPI/Carreta от локальных

### Notification

- отдельный consumer lag monitoring
- отдельный alerting по DLT
- контроль SMTP/Mailjet rate limits

### Files

- убрать `AllowAllFileAccessPolicy`
- связать file access с auth/user context
- продумать antivirus / content scanning / quotas / retention
- настроить backup и lifecycle MinIO

## 11. Текущее состояние интеграционной готовности

## Уже реально работает

- `core` <-> `auth` по token validation
- `core` -> Kafka order events
- `notification` <- Kafka order events -> email
- `auth` -> Redis blacklist
- `core` -> Redis cache UMAPI/Carreta
- `files` -> MinIO + PostgreSQL

## Еще не доведено до сквозной рабочей цепочки

- `core` <-> `files` интеграция
- полноценный auth-aware доступ к файлам
- единый production deployment manifest для всех четырех сервисов
- явный bootstrap процесса первого прод-админа

## 12. Главные грабли и реальные нестыковки

### 1. Портовый конфликт `notification` и `files`

Оба по умолчанию слушают `8083`.

Локально один из них надо переносить, например `files` на `8084`.

### 2. `auth` по умолчанию смотрит в Postgres `5432`

Но shared compose из `core` поднимает Postgres на `5433`.

Значит для совместного локального запуска `auth` требует env override.

### 3. `core` конфигурирует MinIO, но не использует его в коде

Это выглядит как задел под будущую интеграцию, но не рабочая зависимость в текущем состоянии.

### 4. `files` пока без настоящей авторизации

Сервис написан как будто готов к интеграции, но security boundary временно пустой.

### 5. `notification` не имеет пользовательского HTTP API

Это нормально архитектурно, но важно понимать: его работа тестируется не через REST, а через Kafka + Mailhog/Mailjet.

## 13. Минимальный end-to-end сценарий

Если хочется быстро вспомнить “как это живет вместе”, вот самый показательный сценарий:

1. Поднять infra.
2. Запустить `auth`, `core`, `notification`.
3. Залогиниться в `auth`.
4. Создать клиента в `core`.
5. Создать автомобиль в `core`.
6. Создать заказ в `core`.
7. Посмотреть, что в Kafka пришел `ORDER_CREATED`.
8. Посмотреть, что `notification` забрал событие.
9. Открыть Mailhog и увидеть письмо.
10. Перевести заказ в `COMPLETED`.
11. Проверить второе и третье Kafka-событие.
12. Проверить начисление loyalty.
13. Отдельно загрузить файл в `files` и убедиться, что он лежит в MinIO и метаданные лежат в PostgreSQL.

## 14. Короткий вывод

На текущий момент у вас уже не “просто монолит”, а гибридная система:

- бизнес-ядро живет в монолите `core`;
- auth, notifications и files уже вынесены в отдельные сервисы;
- асинхронная интеграция через Kafka уже живая;
- Redis используется и как инфраструктура безопасности, и как кэш внешних API;
- file-service уже написан как отдельный bounded context, но еще не вшит в основной пользовательский поток через `core`.

Если поднимать это на сайт прямо сейчас, правильная модель — запускать 4 backend-сервиса отдельно, а не пытаться втиснуть все в один процесс.

## 15. Source map

Ключевые файлы, по которым собран этот документ:

- `autoshop-core/src/main/resources/application.properties`
- `autoshop-core/src/main/resources/application-local.properties`
- `autoshop-core/docker-compose.yml`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/security/BearerTokenAuthenticationFilter.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/security/RestClientAuthServiceClient.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/order/event/OrderNotificationDomainEventHandler.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/event/notification/KafkaOrderNotificationEventPublisher.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/parts/service/OrderPartInventoryCoordinator.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/loyalty/service/LoyaltyServiceImpl.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/integration/shared/JsonRedisCacheService.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/integration/umapi/client/RestClientUmapiClient.java`
- `autoshop-core/src/main/java/com/vladko/autoshopcore/integration/carreta/client/RestClientCarretaClient.java`
- `autoshop-auth/src/main/resources/application.yml`
- `autoshop-auth/src/main/java/com/vladko/autoshopauth/auth/controller/AuthController.java`
- `autoshop-auth/src/main/java/com/vladko/autoshopauth/auth/service/AuthService.java`
- `autoshop-auth/src/main/java/com/vladko/autoshopauth/security/JwtAuthenticationFilter.java`
- `autoshop-auth/src/main/java/com/vladko/autoshopauth/security/RedisAccessTokenBlacklistService.java`
- `autoshop-auth/src/main/java/com/vladko/autoshopauth/config/DevUsersInitializer.java`
- `autoshop-notification/src/main/resources/application.properties`
- `autoshop-notification/src/main/java/com/vladko/autoshopnotification/event/NotificationEventConsumer.java`
- `autoshop-notification/src/main/java/com/vladko/autoshopnotification/notification/service/NotificationProcessingService.java`
- `autoshop-notification/src/main/java/com/vladko/autoshopnotification/config/KafkaConsumerConfig.java`
- `autoshop-notification/src/main/java/com/vladko/autoshopnotification/template/service/NotificationTemplateService.java`
- `autoshop-notification/src/main/resources/db/changelog/db.changelog-1.0-notifications.sql`
- `autoshop-files/src/main/resources/application.properties`
- `autoshop-files/src/main/java/com/vladko/autoshopfilestorage/file/FileController.java`
- `autoshop-files/src/main/java/com/vladko/autoshopfilestorage/file/FileService.java`
- `autoshop-files/src/main/java/com/vladko/autoshopfilestorage/storage/MinioObjectStorageService.java`
- `autoshop-files/src/main/java/com/vladko/autoshopfilestorage/bucket/BucketInitializer.java`
- `autoshop-files/src/main/java/com/vladko/autoshopfilestorage/presign/PresignService.java`
