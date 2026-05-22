# CRM Customer Auth & Registration Implementation Plan

Дата: 2026-05-14  
Статус: proposed implementation plan  
Проекты: `autoshop-core` + внешний `auth-service`

---

## 1. Цель документа

Этот документ описывает **подробный план реализации customer registration / login / self-service account flows** для клиентского продукта `FrontClient`.

План учитывает:

- текущую архитектуру `autoshop-core`;
- уже существующий внешний `auth-service`, который сейчас валидирует access token;
- доменную модель `Customer` внутри `autoshop-core`;
- необходимость открыть отдельные customer-facing endpoint’ы;
- необходимость синхронизировать auth identity и локальный customer profile.

Документ отвечает на вопросы:

1. Где должна жить логика регистрации и логина?
2. Какие изменения нужны в `autoshop-core`?
3. Какие изменения нужны в `auth-service`?
4. Как сделать rollout безопасно и по фазам?
5. Какие риски есть и как их закрыть?

---

## 2. Executive summary

### Короткий ответ

Да, регистрацию и вход нужно делать **через отдельные endpoint’ы**, но не как вторую локальную auth-систему в `autoshop-core`.

### Рекомендуемая архитектура

- `auth-service` остаётся **единственным владельцем**:
  - паролей
  - refresh token lifecycle
  - email verification
  - password recovery
  - access/refresh token issuance
- `autoshop-core` становится **customer auth facade + domain owner** для:
  - customer profile
  - customer identity linkage
  - customer-owned orders / vehicles / loyalty / dashboard
  - self-service business endpoints `/api/customers/me/*`

### Главный structural change

Нужно отвязать customer access от сравнения:

- `JWT.userId == customer.id`

и перейти к модели:

- `JWT.userId == customer.authUserId`

### Итоговая целевая схема

1. Public auth endpoints:
   - `POST /api/customer-auth/register`
   - `POST /api/customer-auth/login`
   - `POST /api/customer-auth/refresh`
   - `POST /api/customer-auth/logout`
   - `POST /api/customer-auth/password/forgot`
   - `POST /api/customer-auth/password/reset`
   - `POST /api/customer-auth/email/verify`
   - `GET /api/customer-auth/me`
2. Public self-service endpoints:
   - `GET /api/customers/me`
   - `PUT /api/customers/me`
   - `GET /api/customers/me/orders`
   - `GET /api/customers/me/vehicles`
   - `GET /api/customers/me/loyalty`
   - `GET /api/customers/me/dashboard`
3. Internal sync endpoint:
   - `POST /api/internal/customers/sync`

---

## 3. Текущий контекст проекта

## 3.1. Что уже есть

### Primary Locations

- Customer entity: `src/main/java/com/vladko/autoshopcore/client/entity/Customer.java:19`
- Customer repository: `src/main/java/com/vladko/autoshopcore/client/repository/CustomerRepository.java:11`
- Customer CRUD service: `src/main/java/com/vladko/autoshopcore/client/service/CustomerServiceImpl.java:1`
- Current bearer auth filter: `src/main/java/com/vladko/autoshopcore/security/BearerTokenAuthenticationFilter.java:23`
- External auth validation client: `src/main/java/com/vladko/autoshopcore/security/RestClientAuthServiceClient.java:19`
- Auth client properties: `src/main/java/com/vladko/autoshopcore/security/AuthServiceProperties.java:7`
- Current customer access check: `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java:40`
- Existing employee sync pattern: `src/main/java/com/vladko/autoshopcore/employee/controller/InternalEmployeeSyncController.java:17`, `src/main/java/com/vladko/autoshopcore/employee/service/EmployeeSyncServiceImpl.java:24`

### Что умеет backend сейчас

1. Валидировать access token через внешний auth-service
2. Извлекать из токена:
   - `userId`
   - `email`
   - `roles`
   - `jti`
   - `expiresAt`
3. Работать с `Customer` как с локальной CRM-сущностью
4. Проверять customer access к заказу

### Что backend не умеет сейчас

1. Самостоятельно регистрировать customer auth identity
2. Логинить клиента
3. Обновлять refresh token
4. Делать logout
5. Делать reset password
6. Верифицировать email
7. Возвращать current customer profile по `/me`
8. Безопасно связывать auth user и local customer через отдельный identity key

---

## 3.2. Главная архитектурная проблема текущего состояния

В `CoreSecurityServiceImpl.requireCustomerAccess(...)` сейчас используется логика, где customer access завязан на локальный `customer.id`.

Это видно в `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java:40`.

### Почему это плохо

`customer.id` в `autoshop-core` и `userId` во внешнем `auth-service` — это разные идентификаторы из разных bounded contexts.

Если эти id не совпадают, доступ ломается.

### Что это уже показало на staff side

Аналогичная проблема уже проявилась у mechanic flow `/api/orders/my`, где поиск по `JWT.sub -> employee.id` пришлось исправлять на более устойчивую связь через email.

Для customer так делать через email можно как временную меру, но **правильный** долгосрочный вариант — отдельный `authUserId`.

---

## 4. Целевые принципы решения

## 4.1. Не дублировать auth domain

`autoshop-core` не должен хранить:

- password hash
- refresh token registry
- verification tokens
- password reset tokens

Иначе получится вторая auth-система с дублированием рисков и логики.

## 4.2. Единый source of truth для identity

`auth-service` должен быть единственным источником истины для:

- auth user id
- credentials
- roles
- email verification state
- account lock / active state

## 4.3. `autoshop-core` владеет customer domain

`autoshop-core` должен владеть:

- customer profile
- customer vehicles
- customer orders
- customer loyalty
- customer dashboard projections

## 4.4. Все customer-facing contract’ы должны быть self-safe

Фронт не должен передавать голые чужие `customerId` в основном happy-path flow.

Предпочтительная модель:

- `/api/customers/me`
- `/api/customers/me/orders`
- `/api/customers/me/vehicles`

## 4.5. Привязка identity должна быть стабильной

Основной ключ привязки:

- `Customer.authUserId`

Вторичные ключи для recovery / reconciliation:

- `email`
- `phoneNumber`

---

## 5. Целевая архитектура

## 5.1. Bounded contexts

### Auth-service

Отвечает за:

- register
- login
- refresh
- logout
- email verification
- password recovery
- auth user state
- token issuance and validation

### autoshop-core

Отвечает за:

- customer profile provisioning
- customer domain linkage
- self-service profile read/update
- access control by `authUserId`
- customer data aggregation

---

## 5.2. High-level sequence flows

### Registration flow

1. Front → `autoshop-core`: `POST /api/customer-auth/register`
2. `autoshop-core` validates request and business constraints
3. `autoshop-core` → `auth-service`: create auth user with role `CUSTOMER`
4. `auth-service` returns auth user identity + tokens or pending verification state
5. `autoshop-core` creates local `Customer`
6. `autoshop-core` stores `authUserId`
7. `autoshop-core` returns composed response to frontend

### Login flow

1. Front → `autoshop-core`: `POST /api/customer-auth/login`
2. `autoshop-core` proxies login to `auth-service`
3. `auth-service` returns access/refresh tokens + identity
4. `autoshop-core` resolves local customer by `authUserId`
5. If customer missing:
   - either auto-provision from auth payload
   - or return onboarding-required response
6. `autoshop-core` returns composed login response

### Me flow

1. Front → `autoshop-core`: `GET /api/customer-auth/me`
2. Bearer token validated as today
3. `autoshop-core` loads customer by `authUserId`
4. Returns auth summary + profile summary + onboarding state

### Password recovery flow

1. Front → `autoshop-core`: `POST /api/customer-auth/password/forgot`
2. `autoshop-core` proxies to `auth-service`
3. `auth-service` handles email/SMS recovery initiation

---

## 6. Data model changes in `autoshop-core`

## 6.1. Customer entity changes

### New fields

Add to `Customer`:

- `authUserId: Long`
- `emailVerified: Boolean`
- `accountStatus: CustomerAccountStatus` or `String`
- optional `lastLoginAt: Instant`

### Minimum required version

Если делать минимально, достаточно уже первого шага:

- `authUserId BIGINT NULL UNIQUE`

### Recommended version

Лучше сразу добавить:

- `auth_user_id BIGINT`
- `email_verified BOOLEAN NOT NULL DEFAULT FALSE`
- `account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'`
- index/unique on `auth_user_id`

---

## 6.2. New enum proposal

`CustomerAccountStatus`:

- `ACTIVE`
- `PENDING_EMAIL_VERIFICATION`
- `SUSPENDED`
- `DELETED`
- `PENDING_PROFILE_COMPLETION`

Это необязательно для MVP, но полезно для долгосрочной устойчивости.

---

## 6.3. Repository changes

Add methods to `CustomerRepository`:

- `Optional<Customer> findByAuthUserId(Long authUserId)`
- `boolean existsByAuthUserId(Long authUserId)`

Опционально:

- `Optional<Customer> findByEmailIgnoreCase(String email)`
- `Optional<Customer> findByPhoneNumber(String phoneNumber)` уже есть

---

## 6.4. Migration plan

Новый changelog, например:

- `src/main/resources/db/changelog/db.changelog-2.4-customer-auth-linkage.sql`

### Migration steps

1. Add nullable `auth_user_id`
2. Add `email_verified`
3. Add `account_status`
4. Create unique index on `auth_user_id` where not null
5. Backfill existing customers if mappings already known
6. Add not-null later only if all records linked

### Important rollout rule

На первом этапе `auth_user_id` должен быть **nullable**, чтобы не ломать существующие записи.

---

## 7. Security changes in `autoshop-core`

## 7.1. `requireCustomerAccess(...)`

### Current state

Сейчас сравнение идёт на уровне текущего authenticated principal и локального customer без отдельного linkage id.

### Target state

Нужно изменить на:

- extract `AuthenticatedUser.userId()`
- compare with `order.customer.authUserId`

### Fallback strategy during migration

Чтобы rollout был мягким, можно временно поддержать fallback:

1. if `customer.authUserId != null` → compare with it
2. else fallback to old logic or email-based linkage

### Recommended final state

После миграции убрать fallback и оставить только `authUserId`.

---

## 7.2. SecurityConfiguration changes

Нужно открыть customer-facing public auth routes без Bearer token:

- `POST /api/customer-auth/register`
- `POST /api/customer-auth/login`
- `POST /api/customer-auth/refresh`
- `POST /api/customer-auth/password/forgot`
- `POST /api/customer-auth/password/reset`
- `POST /api/customer-auth/email/verify`

Нужно открыть authenticated customer self-service routes:

- `GET /api/customer-auth/me`
- `GET /api/customers/me`
- `PUT /api/customers/me`
- `GET /api/customers/me/orders`
- `GET /api/customers/me/vehicles`
- `GET /api/customers/me/loyalty`
- `GET /api/customers/me/dashboard`

### Important rule

Не открывать staff CRUD endpoints под customer role. Лучше вводить отдельные `/me` маршруты.

---

## 8. New modules/classes in `autoshop-core`

## 8.1. New package proposal

Создать новый пакет:

- `src/main/java/com/vladko/autoshopcore/customerauth/`

Структура:

- `controller/CustomerAuthController.java`
- `service/CustomerAuthService.java`
- `service/CustomerAuthServiceImpl.java`
- `dto/CustomerRegisterRequestDTO.java`
- `dto/CustomerRegisterResponseDTO.java`
- `dto/CustomerLoginRequestDTO.java`
- `dto/CustomerLoginResponseDTO.java`
- `dto/CustomerRefreshRequestDTO.java`
- `dto/CustomerLogoutRequestDTO.java`
- `dto/CustomerForgotPasswordRequestDTO.java`
- `dto/CustomerResetPasswordRequestDTO.java`
- `dto/CustomerVerifyEmailRequestDTO.java`
- `dto/CustomerAuthMeResponseDTO.java`
- `dto/InternalCustomerSyncRequestDTO.java`
- `exception/CustomerAuthLinkageException.java`
- `exception/CustomerRegistrationException.java`

---

## 8.2. New controller endpoints

### Public auth facade

- `POST /api/customer-auth/register`
- `POST /api/customer-auth/login`
- `POST /api/customer-auth/refresh`
- `POST /api/customer-auth/logout`
- `POST /api/customer-auth/password/forgot`
- `POST /api/customer-auth/password/reset`
- `POST /api/customer-auth/email/verify`
- `GET /api/customer-auth/me`

### Internal sync

- `POST /api/internal/customers/sync`

---

## 8.3. New self-service controller

Отдельный customer self-service контроллер лучше выделить отдельно от staff `CustomerController`.

Например:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerSelfServiceController.java`

Endpoints:

- `GET /api/customers/me`
- `PUT /api/customers/me`
- `GET /api/customers/me/orders`
- `GET /api/customers/me/vehicles`
- `GET /api/customers/me/loyalty`
- `GET /api/customers/me/dashboard`

---

## 9. Changes required in external `auth-service`

Это критически важный блок.

`autoshop-core` сам не сможет реализовать полноценную регистрацию/логин без расширения внешнего auth-service.

## 9.1. New auth-service endpoints

### Registration

- `POST /internal/auth/customers/register` or public equivalent

### Login

- `POST /internal/auth/customers/login`

### Refresh

- `POST /internal/auth/customers/refresh`

### Logout

- `POST /internal/auth/customers/logout`

### Password recovery

- `POST /internal/auth/customers/password/forgot`
- `POST /internal/auth/customers/password/reset`

### Email verification

- `POST /internal/auth/customers/email/verify`

### Optional identity fetch

- `GET /internal/auth/users/{id}`
- or `GET /internal/auth/me`

---

## 9.2. What auth-service must return

For register/login/refresh flows backend needs stable response payload including:

- `userId`
- `email`
- `roles`
- `accessToken`
- `refreshToken`
- `expiresAt`
- `emailVerified`
- `accountStatus`

### Example shared response DTO

```json
{
  "userId": 9012,
  "email": "ivan@test.com",
  "roles": ["CUSTOMER"],
  "accessToken": "...",
  "refreshToken": "...",
  "expiresAt": "2026-05-14T18:00:00Z",
  "emailVerified": false,
  "accountStatus": "ACTIVE"
}
```

---

## 9.3. Required auth-service business rules

1. Registration must create users with role `CUSTOMER`
2. Email must be unique globally within auth domain
3. Optional phone uniqueness policy must be explicit
4. Refresh token lifecycle must be secure and revocable
5. Logout must invalidate refresh session or refresh family
6. Email verification should be available before full account activation if needed
7. Password reset must not leak whether user exists in system

---

## 9.4. Auth-service events / sync options

Есть 2 хороших способа синхронизации.

### Option A — synchronous orchestration

`autoshop-core` сам вызывает auth-service в register/login flows.

Плюсы:

- проще MVP
- быстрее вывести в прод

Минусы:

- сильная runtime coupling

### Option B — event/webhook sync

auth-service публикует событие или шлёт webhook:

- `CustomerRegistered`
- `CustomerEmailChanged`
- `CustomerDeleted`
- `CustomerEmailVerified`

Плюсы:

- лучше долгосрочно
- чище ownership

Минусы:

- сложнее инфраструктурно

### Recommendation

Для MVP:

- synchronous orchestration

Для phase 2+:

- optional webhook/event sync

---

## 10. Detailed API contract proposal

## 10.1. `POST /api/customer-auth/register`

### Purpose

Создать auth account + local customer profile.

### Request

```json
{
  "email": "ivan@test.com",
  "phoneNumber": "+79990001122",
  "password": "StrongPass123!",
  "firstName": "Ivan",
  "lastName": "Petrov",
  "acceptTerms": true,
  "acceptPrivacyPolicy": true
}
```

### Validation

- email format
- phone format
- password complexity
- names required
- terms required

### Response variants

#### Immediate success with auto-login

```json
{
  "customerId": 45,
  "authUserId": 9012,
  "email": "ivan@test.com",
  "phoneNumber": "+79990001122",
  "roles": ["CUSTOMER"],
  "accessToken": "...",
  "refreshToken": "...",
  "expiresAt": "2026-05-14T18:00:00Z",
  "emailVerified": false,
  "profileCompleted": true,
  "requiresEmailVerification": true
}
```

#### Registration success but verification pending

```json
{
  "customerId": 45,
  "authUserId": 9012,
  "email": "ivan@test.com",
  "roles": ["CUSTOMER"],
  "emailVerified": false,
  "requiresEmailVerification": true,
  "accessToken": null,
  "refreshToken": null,
  "expiresAt": null
}
```

---

## 10.2. `POST /api/customer-auth/login`

### Request

```json
{
  "email": "ivan@test.com",
  "password": "StrongPass123!"
}
```

### Response

```json
{
  "customerId": 45,
  "authUserId": 9012,
  "email": "ivan@test.com",
  "roles": ["CUSTOMER"],
  "accessToken": "...",
  "refreshToken": "...",
  "expiresAt": "2026-05-14T18:00:00Z",
  "emailVerified": true,
  "profileCompleted": true
}
```

### Important business decision

Если auth user найден, а local customer не найден:

#### Option 1

Auto-provision customer from auth identity.

#### Option 2

Return onboarding state.

### Recommendation

Для customer experience лучше:

- auto-provision if enough profile data available
- otherwise return `409` with explicit onboarding-needed message

---

## 10.3. `POST /api/customer-auth/refresh`

### Request

```json
{
  "refreshToken": "..."
}
```

### Response

Новый token pair + current account state.

---

## 10.4. `POST /api/customer-auth/logout`

### Request

```json
{
  "refreshToken": "..."
}
```

### Response

- `204 No Content` preferred

---

## 10.5. `GET /api/customer-auth/me`

### Purpose

Дать фронту единый bootstrap endpoint для auth + profile linkage state.

### Response

```json
{
  "authenticated": true,
  "authUserId": 9012,
  "email": "ivan@test.com",
  "roles": ["CUSTOMER"],
  "emailVerified": true,
  "customer": {
    "id": 45,
    "firstName": "Ivan",
    "lastName": "Petrov",
    "phoneNumber": "+79990001122",
    "email": "ivan@test.com"
  },
  "profileCompleted": true
}
```

---

## 10.6. `GET /api/customers/me`

### Purpose

Customer self profile read.

### Response

Можно переиспользовать `CustomerResponseDTO`, но лучше сделать новый self-service DTO с auth-related flags.

---

## 11. Detailed changes inside `autoshop-core`

## 11.1. Expand auth client abstraction

Current `AuthServiceClient` only has:

- `validateAccessToken(String accessToken)`

Need to expand with methods like:

- `registerCustomer(...)`
- `loginCustomer(...)`
- `refreshCustomerSession(...)`
- `logoutCustomerSession(...)`
- `requestCustomerPasswordReset(...)`
- `resetCustomerPassword(...)`
- `verifyCustomerEmail(...)`

### Recommendation

Не смешивать validation-only и interactive auth flows в одном минимальном интерфейсе без структуры.

Лучше:

- либо расширить `AuthServiceClient`
- либо завести отдельный `CustomerAuthGateway`

### Preferred structure

- `AuthTokenValidationClient` — только validate
- `CustomerAuthGateway` — register/login/refresh/logout/recovery

Так контекст будет чище.

---

## 11.2. New linkage service

Создать сервис вроде:

- `CustomerIdentityLinkService`

Responsibilities:

- find customer by authUserId
- find customer by email for migration fallback
- link customer to authUserId
- reconcile auth identity with local profile
- guard against duplicate linkages

---

## 11.3. New self-service customer service

Нужен сервис вроде:

- `CustomerSelfService`

Responsibilities:

- getCurrentCustomer()
- updateCurrentCustomer(...)
- getCurrentCustomerOrders()
- getCurrentCustomerVehicles()
- getCurrentCustomerLoyalty()
- getCurrentCustomerDashboard()

Это лучше, чем пытаться переиспользовать staff-oriented `CustomerService` как есть.

---

## 11.4. Order service changes

Нужно заменить customer ownership checks на `authUserId` linkage.

Также желательно убрать customer reliance на routes с path `/{customerId}` в обычном клиентском happy path.

---

## 11.5. Loyalty exposure changes

После появления self-service auth нужно открыть customer-safe loyalty read contract.

Лучший вариант:

- `GET /api/customers/me/loyalty`
- `GET /api/customers/me/loyalty/transactions`

А не открывать напрямую staff loyalty routes.

---

## 12. Rollout plan by phases

## Phase 0 — Discovery & contract alignment

### Goal

Согласовать ownership между командами `autoshop-core` и `auth-service`.

### Tasks

1. Подтвердить, что внешний auth-service остаётся identity owner
2. Подтвердить целевой registration flow
3. Подтвердить login response contract
4. Подтвердить refresh/logout mechanics
5. Подтвердить email verification policy
6. Подтвердить password recovery policy
7. Подтвердить whether auto-login after registration is allowed
8. Подтвердить whether customer can login before email verification

### Deliverables

- signed API contract between services
- agreed error codes
- agreed idempotency behavior

---

## Phase 1 — Data foundation in `autoshop-core`

### Goal

Подготовить `Customer` к корректной identity linkage.

### Tasks

1. Добавить migration с `auth_user_id`
2. Добавить `email_verified`
3. Добавить `account_status` if agreed
4. Обновить `Customer` entity
5. Обновить repository methods
6. Добавить unit tests на repository/service logic

### Acceptance criteria

- customer can exist with null `authUserId`
- customer can be linked to auth identity later
- uniqueness is enforced for non-null linkage

---

## Phase 2 — Security refactor

### Goal

Перевести customer authorization на `authUserId`.

### Tasks

1. Refactor `requireCustomerAccess(...)`
2. Add temporary fallback strategy if needed
3. Add tests for:
   - matching authUserId
   - non-matching authUserId
   - missing linkage
   - staff bypass behavior
4. Audit all customer-facing services that rely on customer ownership

### Acceptance criteria

- no customer access depends on `customer.id == jwt.userId`
- all tests green

---

## Phase 3 — Auth-service extension

### Goal

Добавить в auth-service необходимые customer auth endpoints.

### Tasks

1. Add register endpoint
2. Add login endpoint
3. Add refresh endpoint
4. Add logout endpoint
5. Add forgot-password endpoint
6. Add reset-password endpoint
7. Add email-verify endpoint
8. Add DTO responses with stable identity payload
9. Add role assignment policy for `CUSTOMER`
10. Add tests for auth-service flows

### Acceptance criteria

- auth-service supports full customer auth lifecycle
- response contracts match `autoshop-core` needs

---

## Phase 4 — Auth facade in `autoshop-core`

### Goal

Открыть customer auth facade routes для frontend.

### Tasks

1. Add `CustomerAuthController`
2. Add request/response DTOs
3. Add `CustomerAuthService`
4. Implement auth-service orchestration
5. Add linkage logic authUserId ↔ customer
6. Handle rollback/compensation on partial failures
7. Add integration tests

### Important design decision

При регистрации есть distributed transaction problem:

- auth user created
- local customer creation failed

### Options

1. compensating delete in auth-service
2. mark auth identity as unlinked and retry later
3. outbox/saga style orchestration

### Recommendation for MVP

- compensating delete if supported
- otherwise mark as `PENDING_PROFILE_COMPLETION` and recover via sync flow

---

## Phase 5 — Customer self-service `/me` API

### Goal

Открыть безопасные клиентские доменные endpoint’ы.

### Tasks

1. Add `CustomerSelfServiceController`
2. Add `GET /api/customers/me`
3. Add `PUT /api/customers/me`
4. Add `GET /api/customers/me/orders`
5. Add `GET /api/customers/me/vehicles`
6. Add `GET /api/customers/me/loyalty`
7. Add `GET /api/customers/me/dashboard`
8. Optionally add `GET /api/customers/me/orders/{id}` alias later

### Acceptance criteria

- frontend no longer depends on raw `customerId` in happy path
- all customer reads are self-safe

---

## Phase 6 — Recovery, verification, polish

### Goal

Довести auth UX до production-ready уровня.

### Tasks

1. Add forgot/reset password end-to-end
2. Add email verification flow end-to-end
3. Add resend verification if needed
4. Add rate limiting strategy on register/login/recovery
5. Add abuse protection and audit logging
6. Add observability and metrics

---

## Phase 7 — Optional sync hardening

### Goal

Сделать систему устойчивой к рассинхрону между auth-service и autoshop-core.

### Tasks

1. Add `/api/internal/customers/sync`
2. Add shared internal token or mTLS/auth mechanism
3. Support events/webhooks:
   - customer registered
   - customer email changed
   - customer email verified
   - customer deactivated
4. Add reconciliation job for orphaned/unlinked records

---

## 13. Detailed testing strategy

## 13.1. Unit tests in `autoshop-core`

1. `CustomerIdentityLinkServiceTest`
2. `CustomerAuthServiceTest`
3. `CustomerSelfServiceTest`
4. `CoreSecurityServiceImplTest` for authUserId-based access

## 13.2. Integration tests in `autoshop-core`

1. register success
2. register duplicate email
3. login success
4. login with missing local customer and auto-provision
5. `/api/customer-auth/me` happy path
6. `/api/customers/me` happy path
7. order access by authUserId
8. forbidden cross-customer access

## 13.3. Contract tests between services

1. auth-service register response shape
2. auth-service login response shape
3. auth-service refresh response shape
4. auth-service error payload compatibility

## 13.4. E2E scenarios

1. Register → verify email → login → me → orders
2. Register → partial failure recovery
3. Forgot password → reset → login
4. Logout → refresh invalidated

---

## 14. Error model proposal

Нужно согласовать ошибки между `auth-service` и `autoshop-core`.

### Suggested status mapping

- `400` — invalid request
- `401` — invalid credentials / invalid token
- `403` — forbidden / unverified if policy chooses
- `404` — customer profile not found when should exist
- `409` — duplicate email / duplicate phone / already linked identity
- `422` — business validation issues if desired
- `429` — rate limited
- `503` — auth-service unavailable

### Important rule

Password recovery endpoint не должен раскрывать, существует ли email.

---

## 15. Observability and operations

## 15.1. Metrics

Add metrics for:

- registration attempts
- successful registrations
- login attempts
- successful logins
- auth-service failures
- customer linkage failures
- unlinked customer login cases

## 15.2. Logs

Structured logs with:

- email hash or masked email
- authUserId if available
- customerId if available
- operation type
- failure reason category

### Never log

- passwords
- full refresh tokens
- full access tokens
- reset secrets

---

## 16. Security & privacy considerations

1. Never store password in `autoshop-core`
2. Never log password or full token values
3. Add rate limiting on public auth facade routes
4. Ensure refresh token transport policy is agreed
5. Prefer httpOnly secure cookie for refresh token if architecture allows
6. If refresh token stays in JSON body, protect client storage carefully
7. Support email verification before privileged flows if required
8. Ensure customer linkage cannot be hijacked by email collision or stale data

---

## 17. Open questions requiring product/architecture decision

1. Должен ли registration автоматически логинить пользователя?
2. Может ли пользователь логиниться до email verification?
3. Нужен ли phone-based login, или только email?
4. Должен ли phone быть уникальным в auth domain, в customer domain, или в обоих?
5. Что делать при существующем auth user без local customer?
6. Что делать при существующем local customer без auth user?
7. Нужен ли social login позже?
8. Где хранится refresh token — cookie или body/local storage?
9. Нужен ли soft-delete / suspend для customer account?
10. Нужен ли resend verification endpoint сразу?

---

## 18. Recommended implementation order

Если делать максимально прагматично, я рекомендую такой порядок:

1. Add `Customer.authUserId` foundation
2. Refactor customer security checks
3. Extend auth-service with register/login/refresh/logout
4. Add `CustomerAuthController` facade
5. Add `/api/customer-auth/me`
6. Add `/api/customers/me`
7. Add `/api/customers/me/orders`
8. Add `/api/customers/me/vehicles`
9. Add `/api/customers/me/loyalty`
10. Add `/api/customers/me/dashboard`
11. Add forgot/reset password
12. Add email verification/resend
13. Add internal sync / reconciliation hardening

---

## 19. Suggested deliverables by PRs

## PR 1

- DB migration for customer auth linkage
- entity + repository changes
- security refactor to authUserId
- tests

## PR 2

- auth-service endpoint extension
- shared response contracts
- auth-service tests

## PR 3

- `CustomerAuthController`
- register/login/refresh/logout/me
- orchestration service
- integration tests

## PR 4

- `CustomerSelfServiceController`
- `/me`, `/me/orders`, `/me/vehicles`
- tests

## PR 5

- loyalty/dashboard customer endpoints
- frontend-ready DTOs
- tests

## PR 6

- forgot/reset password
- email verification
- rate limiting / observability

## PR 7

- internal sync / webhook reconciliation
- cleanup tasks

---

## 20. Final recommendation

Для этого проекта оптимальное решение — **не строить локальную auth-систему внутри `autoshop-core`**, а реализовать:

1. **customer auth facade** в `autoshop-core`
2. **identity source of truth** во внешнем `auth-service`
3. **stable linkage** через `Customer.authUserId`
4. **customer self-service API** через `/api/customers/me/*`

Это даст:

- чистую архитектуру без дублирования паролей;
- корректную и масштабируемую customer authorization model;
- хороший фундамент для `FrontClient`;
- минимальный риск повторения проблемы `jwt user id != local domain id`.

---

## 21. Next step

Следующий практический шаг после согласования этого плана:

1. зафиксировать contracts с командой `auth-service`;
2. сделать migration + `authUserId` refactor;
3. затем открывать `register/login/me` facade.
