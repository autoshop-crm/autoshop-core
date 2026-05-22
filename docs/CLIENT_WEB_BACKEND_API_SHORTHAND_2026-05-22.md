# Autoshop Client Web: public backend API шпаргалка — 2026-05-22

Этот файл нужен команде `autoshop-client-web`, чтобы синхронизироваться с **реальным публичным API**, который отдан клиенту через `autoshop-core`.

Главная цель этой шпаргалки — не путать:

- **public API для `client-web`**
- **internal bridge `core -> auth`**

Это два разных слоя.

## Короткий вывод

Для `client-web` canonical публичный контракт сейчас такой:

- `POST /api/customer-auth/register`
- `POST /api/customer-auth/login`
- `POST /api/customer-auth/refresh`
- `POST /api/customer-auth/logout`
- `GET /api/customer-auth/me`
- `GET /api/customers/me`
- `PUT /api/customers/me`
- `GET /api/customers/me/dashboard`

`client-web` **не должен** напрямую ходить в:

- `/api/auth/customers/*`
- `/api/auth/me`

Пути вида `/api/auth/customers/*` — это internal downstream API, который `autoshop-core` сам вызывает внутри при обращении к `autoshop-auth`.

## Архитектурная схема

### Что вызывает `client-web`

Frontend должен вызывать только public routes:

- `/api/customer-auth/*`
- `/api/customers/me*`

### Что вызывает `autoshop-core`

Внутри `core` customer auth facade мостит запросы в `auth` сервис на:

- `/api/auth/customers/register`
- `/api/auth/customers/login`
- `/api/auth/customers/refresh`
- `/api/auth/customers/logout`
- `/api/auth/customers/password/forgot`
- `/api/auth/customers/password/reset`
- `/api/auth/customers/email/verify`

То есть оба утверждения одновременно верны:

- для клиента правильный API — **`/api/customer-auth/*`**
- для внутреннего вызова `core -> auth` используются **`/api/auth/customers/*`**

## Что фронту нельзя делать

Нельзя переключать `client-web` на internal downstream contract:

- нельзя вызывать `/api/auth/customers/register`
- нельзя вызывать `/api/auth/customers/login`
- нельзя вызывать `/api/auth/customers/refresh`
- нельзя вызывать `/api/auth/customers/logout`
- нельзя вызывать `/api/auth/me`

Если frontend начнёт зависеть от этих маршрутов, он будет опираться не на свой публичный API, а на внутреннюю интеграцию между сервисами.

## Public customer auth API

### `POST /api/customer-auth/register`

Это public endpoint для `client-web`.

#### Request body

```json
{
  "email": "customer@example.com",
  "phoneNumber": "+79990000000",
  "password": "strongPassword123",
  "firstName": "Ivan",
  "lastName": "Petrov",
  "acceptTerms": true,
  "acceptPrivacyPolicy": true
}
```

#### Validation

- `email` — email, max 50
- `phoneNumber` — regex `^\\+?[0-9]{10,15}$`
- `password` — min 8, max 100
- `firstName` — min 2, max 50
- `lastName` — min 2, max 50
- `acceptTerms` must be `true`
- `acceptPrivacyPolicy` must be `true`

#### Response

Status:

- `201 Created`

Body:

```json
{
  "customerId": 123,
  "authUserId": 456,
  "email": "customer@example.com",
  "phoneNumber": "+79990000000",
  "roles": ["CUSTOMER"],
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "expiresAt": "2026-05-22T12:34:56Z",
  "emailVerified": false,
  "profileCompleted": true,
  "requiresEmailVerification": true
}
```

### `POST /api/customer-auth/login`

Это public endpoint для `client-web`.

#### Request body

```json
{
  "email": "customer@example.com",
  "password": "strongPassword123"
}
```

#### Response

Status:

- `200 OK`

Body:

```json
{
  "customerId": 123,
  "authUserId": 456,
  "email": "customer@example.com",
  "phoneNumber": "+79990000000",
  "roles": ["CUSTOMER"],
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "expiresAt": "2026-05-22T12:34:56Z",
  "emailVerified": true,
  "profileCompleted": true,
  "requiresEmailVerification": false
}
```

### `POST /api/customer-auth/refresh`

Это public endpoint для `client-web`.

#### Request body

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

#### Response

Status:

- `200 OK`

Body shape совпадает с `login`.

### `POST /api/customer-auth/logout`

Это public endpoint для `client-web`.

#### Request body

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

#### Response

Status:

- `204 No Content`

### `GET /api/customer-auth/me`

Это public endpoint для `client-web`.

Важно:

- это **не** прямой endpoint `auth` сервиса
- это facade endpoint внутри `autoshop-core`
- он собирает auth/session state из `SecurityContext`
- он возвращает вложенный customer profile

#### Auth requirements

- `Authorization: Bearer <accessToken>`
- в токене должна быть роль `CUSTOMER`

#### Response

Status:

- `200 OK`

Body:

```json
{
  "authenticated": true,
  "authUserId": 456,
  "email": "customer@example.com",
  "roles": ["CUSTOMER"],
  "emailVerified": true,
  "customer": {
    "id": 123,
    "firstName": "Ivan",
    "lastName": "Petrov",
    "phoneNumber": "+79990000000",
    "email": "customer@example.com",
    "emailVerified": true,
    "createdAt": "2026-05-20T10:00:00Z",
    "updatedAt": "2026-05-22T11:00:00Z"
  },
  "profileCompleted": true
}
```

#### Frontend note

Для bootstrap current user frontend должен читать профиль клиента из:

- `response.customer`

То есть:

- верхний уровень — auth/session state
- вложенный `customer` — профиль клиента

## Public customer self-service API

### `GET /api/customers/me`

#### Auth requirements

- `Authorization: Bearer <accessToken>`
- роль `CUSTOMER`

#### Response

```json
{
  "id": 123,
  "firstName": "Ivan",
  "lastName": "Petrov",
  "phoneNumber": "+79990000000",
  "email": "customer@example.com",
  "emailVerified": true,
  "createdAt": "2026-05-20T10:00:00Z",
  "updatedAt": "2026-05-22T11:00:00Z"
}
```

### `PUT /api/customers/me`

#### Request body

```json
{
  "firstName": "Ivan",
  "lastName": "Petrov",
  "phoneNumber": "+79990000000",
  "email": "customer@example.com"
}
```

#### Validation

- `firstName` — pattern `^(?!\\s*$).{2,50}$`
- `lastName` — pattern `^(?!\\s*$).{2,50}$`
- `phoneNumber` — regex `^\\+?[0-9]{10,15}$`
- `email` — email, max 50

#### Response

Возвращает тот же `CustomerResponseDTO`, что и `GET /api/customers/me`.

### `GET /api/customers/me/dashboard`

Этот endpoint **реально существует** в `core` и открыт для `CUSTOMER`.

#### Auth requirements

- `Authorization: Bearer <accessToken>`
- роль `CUSTOMER`

#### Response shape

```json
{
  "customer": {
    "id": 123,
    "firstName": "Ivan",
    "lastName": "Petrov",
    "phoneNumber": "+79990000000",
    "email": "customer@example.com",
    "emailVerified": true,
    "createdAt": "2026-05-20T10:00:00Z",
    "updatedAt": "2026-05-22T11:00:00Z"
  },
  "recentOrders": [],
  "pendingApprovals": [],
  "vehicles": [],
  "loyalty": {
    "account": null,
    "recentTransactions": [],
    "tiers": []
  },
  "loyaltySettings": {
    "enabled": true,
    "earnEnabled": true,
    "spendEnabled": true,
    "visible": true
  }
}
```

#### Top-level fields

- `customer`
- `recentOrders`
- `pendingApprovals`
- `vehicles`
- `loyalty`
- `loyaltySettings`

#### Если frontend получает `403`

Это почти наверняка не route mismatch.

Проверять нужно:

- ушёл ли `Authorization: Bearer <accessToken>`
- не протух ли access token
- есть ли в токене роль `CUSTOMER`
- не теряется ли токен после login/register/refresh
- не теряется ли auth state перед dashboard bootstrap

## Что нужно поправить в `client-web`

### Auth routes

Использовать только:

- `POST /api/customer-auth/register`
- `POST /api/customer-auth/login`
- `POST /api/customer-auth/refresh`
- `POST /api/customer-auth/logout`
- `GET /api/customer-auth/me`

Не использовать:

- `/api/auth/customers/*`
- `/api/auth/me`

### Current user bootstrap

Frontend должен ожидать у `GET /api/customer-auth/me` именно такой смысл:

- auth state
- роли
- email verification
- вложенный `customer`

Нельзя ожидать плоский customer profile на верхнем уровне.

### Dashboard bootstrap

Frontend должен:

- вызывать `GET /api/customers/me/dashboard`
- всегда отправлять `Authorization: Bearer <accessToken>`
- трактовать `403` как проблему токена/роли/заголовка, а не как отсутствие endpoint

## Практический чек-лист

1. Убедиться, что auth API в frontend использует `/api/customer-auth/*`
2. Убедиться, что нигде не осталось `/api/auth/customers/*`
3. Для bootstrap current user использовать `GET /api/customer-auth/me`
4. Читать customer profile из `response.customer`
5. Для dashboard использовать `GET /api/customers/me/dashboard`
6. После register/login сохранять `accessToken`
7. После refresh обновлять `accessToken`
8. Во все customer protected endpoints подставлять `Authorization: Bearer <accessToken>`
9. Проверить, что роль `CUSTOMER` не теряется при разборе auth response

## Финальный вывод

Для `autoshop-client-web` правильный публичный контракт сейчас такой:

- customer auth base path: **`/api/customer-auth`**
- current auth/customer bootstrap: **`GET /api/customer-auth/me`**
- customer profile: **`GET /api/customers/me`**
- customer dashboard: **`GET /api/customers/me/dashboard`**

Internal routes вида `/api/auth/customers/*` — это не frontend contract, а внутренний bridge между `core` и `auth`.

Если `client-web` синхронизируется именно с этим public contract, клиентская auth-цепочка должна заработать корректно.
