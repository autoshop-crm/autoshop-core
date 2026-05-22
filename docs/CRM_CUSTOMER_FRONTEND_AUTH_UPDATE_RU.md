# CRM Customer Frontend Update — Auth & Self-Service API

Дата: 2026-05-14  
Статус: ready for frontend integration update  
Проект: `FrontClient`

---

## 1. Зачем этот документ

Этот документ кратко и прикладно описывает, **что изменилось в backend для клиентского фронта** после реализации customer auth/self-service foundation.

Используй его как update-note для фронтенда:

- какие новые endpoint’ы появились;
- какие старые assumptions больше неактуальны;
- что теперь можно подключать;
- какие ограничения всё ещё остаются;
- что нужно изменить в frontend API layer.

---

## 2. Что появилось в backend

Backend теперь поддерживает 2 новых блока API:

1. **Customer Auth Facade**
2. **Customer Self-Service API**

### Customer Auth Facade

Новые маршруты:

- `POST /api/customer-auth/register`
- `POST /api/customer-auth/login`
- `POST /api/customer-auth/refresh`
- `POST /api/customer-auth/logout`
- `POST /api/customer-auth/password/forgot`
- `POST /api/customer-auth/password/reset`
- `POST /api/customer-auth/email/verify`
- `GET /api/customer-auth/me`

### Customer Self-Service API

Новые маршруты:

- `GET /api/customers/me`
- `PUT /api/customers/me`
- `GET /api/customers/me/orders`
- `GET /api/customers/me/vehicles`
- `GET /api/customers/me/loyalty`
- `GET /api/customers/me/dashboard`

---

## 3. Главное изменение в frontend модели

Раньше frontend должен был мыслить так:

- customer identity отдельно
- customer profile отдельно
- некоторые customer screens вообще нельзя было подключить напрямую

Теперь основной happy-path должен строиться через:

- `customer-auth/*` для auth lifecycle
- `customers/me/*` для self-service domain data

### Новый preferred frontend подход

Не использовать в клиентском приложении как основной путь:

- `GET /api/orders/customer/{customerId}`
- `GET /api/orders/vehicle/{vehicleId}`
- staff-style customer/vehicle routes

Использовать вместо этого:

- `GET /api/customers/me`
- `GET /api/customers/me/orders`
- `GET /api/customers/me/vehicles`
- `GET /api/customers/me/loyalty`
- `GET /api/customers/me/dashboard`

---

## 4. Новые auth endpoint’ы

## 4.1. `POST /api/customer-auth/register`

### Для чего

Регистрация customer account через backend facade.

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

### Response

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

### Frontend note

После регистрации фронт должен быть готов к 2 вариантам:

1. пользователь уже логинен;
2. пользователь зарегистрирован, но должен подтвердить email.

Лучше не зашивать жёстко только auto-login сценарий.

---

## 4.2. `POST /api/customer-auth/login`

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
  "phoneNumber": "+79990001122",
  "roles": ["CUSTOMER"],
  "accessToken": "...",
  "refreshToken": "...",
  "expiresAt": "2026-05-14T18:00:00Z",
  "emailVerified": true,
  "profileCompleted": true,
  "requiresEmailVerification": false
}
```

### Frontend note

Это теперь основной login endpoint для `FrontClient`.

---

## 4.3. `POST /api/customer-auth/refresh`

### Request

```json
{
  "refreshToken": "..."
}
```

### Response

Тот же shape, что у login/register response.

### Frontend note

Если refresh token хранится на фронте, auth storage layer должен уметь обновлять:

- `accessToken`
- `refreshToken`
- `expiresAt`

---

## 4.4. `POST /api/customer-auth/logout`

### Request

```json
{
  "refreshToken": "..."
}
```

### Response

- `204 No Content`

### Frontend note

После успешного logout нужно очистить локальное auth storage полностью.

---

## 4.5. `POST /api/customer-auth/password/forgot`

### Request

```json
{
  "email": "ivan@test.com"
}
```

### Response

```json
{
  "success": true,
  "message": "Password recovery flow started"
}
```

### Frontend note

UI не должен ожидать подтверждения существования пользователя.

---

## 4.6. `POST /api/customer-auth/password/reset`

### Request

```json
{
  "resetToken": "...",
  "newPassword": "StrongPass456!"
}
```

### Response

```json
{
  "success": true,
  "message": "Password has been reset"
}
```

---

## 4.7. `POST /api/customer-auth/email/verify`

### Request

```json
{
  "verificationToken": "..."
}
```

### Response

Возвращает такой же auth payload, как login/register.

### Frontend note

После verify фронт может:

- либо обновить auth/session state напрямую из ответа;
- либо вызвать `GET /api/customer-auth/me`.

---

## 4.8. `GET /api/customer-auth/me`

### Для чего

Auth bootstrap endpoint.

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
    "email": "ivan@test.com",
    "emailVerified": true,
    "createdAt": "2026-05-10T10:00:00Z",
    "updatedAt": "2026-05-14T11:00:00Z"
  },
  "profileCompleted": true
}
```

### Frontend note

Это лучший endpoint для:

- app bootstrap;
- restore session;
- protected route hydration;
- profile header / nav state.

---

## 5. Новые self-service endpoint’ы

## 5.1. `GET /api/customers/me`

### Для чего

Получить текущий профиль клиента.

### Response

```json
{
  "id": 45,
  "firstName": "Ivan",
  "lastName": "Petrov",
  "phoneNumber": "+79990001122",
  "email": "ivan@test.com",
  "emailVerified": true,
  "createdAt": "2026-05-10T10:00:00Z",
  "updatedAt": "2026-05-14T11:00:00Z"
}
```

---

## 5.2. `PUT /api/customers/me`

### Для чего

Обновить self-service профиль клиента.

### Request

```json
{
  "firstName": "Ivan",
  "lastName": "Petrov",
  "phoneNumber": "+79990002233"
}
```

### Важное ограничение

**Email менять через этот endpoint нельзя.**

Если frontend попытается отправить новый `email`, backend вернёт ошибку:

- `400 Bad Request`
- message: `Customer email change must go through auth flow`

### Frontend implication

На profile screen:

- либо не давать редактировать email вообще;
- либо пометить его как read-only;
- либо вынести в отдельный future flow “Change email”.

---

## 5.3. `GET /api/customers/me/orders`

### Для чего

Получить список заказов текущего клиента без передачи `customerId`.

### Response

`OrderResponseDTO[]`

### Frontend note

Это теперь основной endpoint для:

- orders list page;
- dashboard recent orders;
- vehicles history composition.

### Replaces

Логически заменяет старый клиентский dependency на:

- `GET /api/orders/customer/{customerId}`

---

## 5.4. `GET /api/customers/me/vehicles`

### Для чего

Получить список автомобилей текущего клиента.

### Response

`VehicleResponseDTO[]`

Пример:

```json
[
  {
    "id": 3,
    "customerId": 45,
    "brand": "BMW",
    "model": "X5",
    "vin": "WBA12345678901234",
    "licensePlate": "A123AA77",
    "umapiType": null,
    "umapiId": null,
    "createdAt": "2026-05-10T10:00:00Z"
  }
]
```

### Frontend note

Теперь можно полноценно подключать экран `Мои автомобили`.

---

## 5.5. `GET /api/customers/me/loyalty`

### Для чего

Получить loyalty overview текущего клиента.

### Response

```json
{
  "account": {
    "id": 11,
    "customerId": 45,
    "balance": 320,
    "totalSpent": 42000.00,
    "totalEarnedPoints": 890,
    "tier": {
      "id": 3,
      "name": "GOLD",
      "entrySpentMoney": 30000.00,
      "discountPercent": 5,
      "maxPointsPaymentPercent": 30
    },
    "createdAt": "2026-05-01T10:00:00Z",
    "updatedAt": "2026-05-14T12:00:00Z"
  },
  "recentTransactions": [
    {
      "id": 100,
      "accountId": 11,
      "orderId": 5,
      "operationType": "EARN",
      "reason": "ORDER_COMPLETED",
      "pointsAmount": 120,
      "createdAt": "2026-05-14T11:00:00Z"
    }
  ],
  "tiers": [
    {
      "id": 1,
      "name": "BRONZE",
      "entrySpentMoney": 0.00,
      "discountPercent": 0,
      "maxPointsPaymentPercent": 10
    }
  ]
}
```

### Frontend note

Это теперь основной endpoint для loyalty screen.

---

## 5.6. `GET /api/customers/me/dashboard`

### Для чего

Получить готовый self-service dashboard payload.

### Response

```json
{
  "customer": {
    "id": 45,
    "firstName": "Ivan",
    "lastName": "Petrov",
    "phoneNumber": "+79990001122",
    "email": "ivan@test.com",
    "emailVerified": true,
    "createdAt": "2026-05-10T10:00:00Z",
    "updatedAt": "2026-05-14T11:00:00Z"
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

### Frontend note

Это лучший новый backend endpoint для `ClientDashboardPage`.

Используй его как основной источник для:

- greeting / profile summary
- recent orders
- pending approvals
- vehicles summary
- loyalty teaser

---

## 6. Что изменилось в безопасности

Это важно для frontend assumptions.

### 6.1. Customer order access tightened

Backend больше не должен использовать broad customer exposure для старых order list routes как основной контракт.

### 6.2. Customer cancel path защищён ownership check

Клиент может отменить только **свой** заказ.

### 6.3. Email больше не редактируется локально

Это теперь auth-owned поле.

---

## 7. Что нужно обновить на фронте

## 7.1. Новый auth API client

Нужен отдельный `clientAuthApi.ts` или аналог:

- `registerCustomer(payload)`
- `loginCustomer(payload)`
- `refreshCustomerSession(payload)`
- `logoutCustomer(payload)`
- `forgotPassword(payload)`
- `resetPassword(payload)`
- `verifyCustomerEmail(payload)`
- `getCustomerAuthMe()`

## 7.2. Новый self-service API client

Нужен `clientProfileApi.ts` / `clientSelfServiceApi.ts`:

- `getCurrentCustomer()`
- `updateCurrentCustomer(payload)`
- `getCurrentCustomerOrders()`
- `getCurrentCustomerVehicles()`
- `getCurrentCustomerLoyalty()`
- `getCurrentCustomerDashboard()`

## 7.3. Что убрать из старого customer happy-path

Не использовать как базовый путь:

- `GET /api/orders/customer/{customerId}`
- `GET /api/orders/vehicle/{vehicleId}`
- staff customer routes
- staff vehicle routes
- старые loyalty staff routes

---

## 8. UI implications

## 8.1. Login page

Теперь можно подключать к реальному backend endpoint:

- `POST /api/customer-auth/login`

## 8.2. Register page

Теперь можно подключать к:

- `POST /api/customer-auth/register`

## 8.3. Recovery flow

Можно убирать заглушки и подключать:

- `POST /api/customer-auth/password/forgot`
- `POST /api/customer-auth/password/reset`

## 8.4. Profile page

Можно подключать к:

- `GET /api/customers/me`
- `PUT /api/customers/me`

Но email должен быть read-only.

## 8.5. Vehicles page

Можно подключать к:

- `GET /api/customers/me/vehicles`

## 8.6. Orders page

Основной endpoint:

- `GET /api/customers/me/orders`

## 8.7. Dashboard page

Основной endpoint:

- `GET /api/customers/me/dashboard`

## 8.8. Loyalty page

Основной endpoint:

- `GET /api/customers/me/loyalty`

---

## 9. Error handling notes for frontend

Все ошибки идут в стандартном формате:

```json
{
  "timestamp": "2026-05-14T16:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Customer email change must go through auth flow",
  "path": "/api/customers/me"
}
```

### Important frontend-specific cases

#### Attempt to change email via profile update

Ожидаемая ошибка:

- `400`
- `message = "Customer email change must go through auth flow"`

#### Auth linkage issue

Возможна ошибка:

- `409`
- если auth account и local customer profile не смогли корректно связаться

#### Invalid credentials / invalid token

Ожидай:

- `401`

---

## 10. Что ещё зависит от внешнего auth-service

Важно: эти backend routes уже реализованы в `autoshop-core`, но для реальной работы внешний auth-service должен поддержать соответствующие пути.

То есть frontend может начинать интеграцию контракта уже сейчас, но end-to-end работа будет зависеть от готовности auth-service.

Зависимые маршруты:

- register
- login
- refresh
- logout
- forgot password
- reset password
- verify email

---

## 11. Recommended frontend rollout order

1. Обновить auth storage model под новые response payloads
2. Подключить `GET /api/customer-auth/me` как bootstrap
3. Подключить login page
4. Подключить register page
5. Подключить profile page (`/customers/me`)
6. Подключить orders list (`/customers/me/orders`)
7. Подключить vehicles page (`/customers/me/vehicles`)
8. Подключить dashboard (`/customers/me/dashboard`)
9. Подключить loyalty (`/customers/me/loyalty`)
10. Подключить forgot/reset password UX

---

## 12. Краткий итог

После backend update у фронтенда теперь есть нормальный foundation для customer app:

- auth facade
- self-service `/me` API
- dashboard endpoint
- vehicles endpoint
- loyalty endpoint
- safer ownership model

Главные frontend изменения:

- строить auth через `customer-auth/*`
- строить customer domain через `customers/me/*`
- перестать опираться на старые staff-style customer routes
- сделать email read-only в profile form

