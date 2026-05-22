# CRM Customer Web API Documentation

Дата: 2026-05-14  
Статус: backend contract snapshot for `FrontClient`  
Проект: `autoshop-core`

---

## 1. Цель документа

Этот документ описывает **полный backend API-контур**, который сейчас доступен или частично доступен для клиентского веб-приложения `FrontClient`.

Документ собран по реальному коду backend, а не по желаемому future-state.

Он нужен, чтобы фронтенд мог:

- подключить уже существующие customer flows без догадок;
- понять, какие API можно использовать прямо сейчас;
- увидеть ограничения текущей авторизации и ролей;
- выявить missing backend contracts для экранов из:
  - `docs/CRM_CUSTOMER_UI_DESIGN_SPEC_RU.md`
  - `docs/CRM_CUSTOMER_FRONTEND_IMPLEMENTATION_PLAN_RU.md`

---

## 2. Executive summary

На текущий момент backend **частично** готов для customer web.

### Что реально можно использовать уже сейчас

1. Просмотр заказа по `id`
2. Просмотр списка заказов по `customerId`
3. Просмотр списка заказов по `vehicleId`
4. Просмотр customer timeline по заказу
5. Просмотр approval requests по заказу
6. Approve / reject approval request клиентом
7. Отмена заказа клиентом через изменение статуса на `CANCELLED_BY_CUSTOMER`

### Что для customer UI задумано, но сейчас не готово как public customer API

1. Профиль клиента
2. Мои автомобили
3. Loyalty overview / history
4. Документы / фото
5. Самостоятельное создание booking клиентом
6. Customer-safe dashboard endpoint
7. Customer-safe aggregated vehicles/orders endpoints без передачи чужих `customerId`

### Самые важные backend gaps

1. Роль `CUSTOMER` **не имеет доступа** к `/api/customers/**`
2. Роль `CUSTOMER` **не имеет доступа** к `/api/vehicles/**`
3. Роль `CUSTOMER` **не имеет доступа** к `/api/loyalty/**`
4. Нет отдельного customer profile endpoint вида `/api/me` или `/api/customers/me`
5. Нет customer dashboard aggregate endpoint
6. Нет documents/files API для клиента
7. Некоторые `GET /api/orders/**` endpoint’ы доступны роли `CUSTOMER` по security, но не всегда защищены `requireCustomerAccess(...)` на сервисном уровне — это значит, что фронту **нельзя** считать их customer-safe без явной backend-ограничительной логики

---

## 3. Источники истины в коде

### Primary Locations

- Security rules: `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:60`
- Customer access enforcement: `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java:40`
- Orders API: `src/main/java/com/vladko/autoshopcore/order/controller/OrderController.java:22`
- Orders service: `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:62`
- Order approvals API: `src/main/java/com/vladko/autoshopcore/order/approval/controller/OrderApprovalController.java:13`
- Order timeline API: `src/main/java/com/vladko/autoshopcore/order/timeline/controller/OrderTimelineController.java:12`
- Loyalty API: `src/main/java/com/vladko/autoshopcore/loyalty/controller/LoyaltyController.java:18`
- Vehicles API: `src/main/java/com/vladko/autoshopcore/vehicle/controller/VehicleController.java:16`
- Customers API: `src/main/java/com/vladko/autoshopcore/client/controller/CustomerController.java:15`
- Global error format: `src/main/java/com/vladko/autoshopcore/shared/exception/ErrorResponse.java:5`

---

## 4. Авторизация и общий протокол

## 4.1. Base URL

По локальной разработке запросы идут через frontend proxy:

- `http://localhost:5173/api/...`

На backend все маршруты начинаются с:

- `/api/...`

## 4.2. Auth model

Backend **не выдает токен сам**, а валидирует `Bearer` access token через внешний auth service.

Во всех защищённых запросах нужно передавать:

```http
Authorization: Bearer <access_token>
```

Внутри backend используются поля аутентифицированного пользователя:

- `userId`
- `email`
- `roles`
- `jti`
- `expiresAt`

См. `src/main/java/com/vladko/autoshopcore/security/AuthenticatedUser.java:6`

## 4.3. Роли

Основные роли, встречающиеся в customer context:

- `CUSTOMER`
- `MECHANIC`
- `RECEPTIONIST`
- `MANAGER`
- `ADMIN`

Для клиентского веба целевая роль:

- `CUSTOMER`

## 4.4. Формат ошибок

Единый error payload:

```json
{
  "timestamp": "2026-05-14T16:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order with id '10' was not found",
  "path": "/api/orders/10"
}
```

Контракт: `src/main/java/com/vladko/autoshopcore/shared/exception/ErrorResponse.java:5`

---

## 5. Customer-safe vs customer-unsafe endpoints

Это важный раздел.

В проекте есть разница между:

- endpoint, который **security разрешает** роли `CUSTOMER`
- endpoint, который **реально безопасен** для customer и проверяет владение заказом

### Customer-safe сейчас

Эти endpoint’ы либо явно рассчитаны на клиента, либо дополнительно проверяют доступ через `requireCustomerAccess(order)`:

1. `GET /api/orders/{id}`
2. `GET /api/orders/{orderId}/timeline/customer`
3. `GET /api/orders/{orderId}/approvals`
4. `POST /api/orders/{orderId}/approvals/{requestId}/approve`
5. `POST /api/orders/{orderId}/approvals/{requestId}/reject`
6. `PUT /api/orders/{id}/status` с `status=CANCELLED_BY_CUSTOMER`

### Customer-allowed by security, but not guaranteed safe by service logic

Эти endpoint’ы роль `CUSTOMER` технически может вызвать, но в сервисах не видно жёсткой проверки владения тем же способом:

1. `GET /api/orders/customer/{customerId}`
2. `GET /api/orders/vehicle/{vehicleId}`
3. `GET /api/orders/status/{status}`

Для production customer frontend **не рекомендуется** строить UX на этих endpoint’ах как на безусловно безопасных, пока backend не добавит строгую привязку к текущему customer.

### Вообще недоступно роли `CUSTOMER`

1. `/api/customers/**`
2. `/api/vehicles/**`
3. `/api/loyalty/**`
4. `/api/crm/orders/**`
5. `/api/orders/my` — это механик-only
6. Создание/редактирование заказа staff-операциями

---

## 6. API coverage по экранам FrontClient

## 6.1. Dashboard

### Можно собрать частично

Из текущего backend можно собрать dashboard только через несколько запросов:

- список заказов клиента
- список approval requests по каждому заказу
- timeline по нужным заказам

### Нет готового dashboard endpoint

Сейчас отсутствует endpoint вида:

- `GET /api/customer/dashboard`

### Рекомендация для фронта

Собирать `ClientDashboardViewModel` на frontend BFF/client layer из:

1. списка заказов
2. pending approvals
3. derived next action
4. derived vehicle summary

### Backend gap

Нужен aggregate endpoint для dashboard, если хочется меньше клиентской orchestration-логики.

## 6.2. Orders

### Поддерживается

- orders list
- order details
- order cancellation by customer
- timeline preview/details
- approvals from order details

## 6.3. Vehicles

### Для customer UI в дизайне нужны

- список автомобилей
- детали автомобиля
- история заказов по авто

### По факту сейчас

- vehicle endpoints существуют
- но роль `CUSTOMER` к ним не допущена

Итого: экран можно проектировать, но backend contract для customer пока отсутствует.

## 6.4. Approvals

### Поддерживается хорошо

- список approval requests по заказу
- approve/reject клиентом
- tokens/idempotency в решениях

Это один из самых готовых customer flows.

## 6.5. Loyalty

### По UI плану экран нужен

Да, loyalty есть в дизайн- и implementation docs.

### По backend факту

Loyalty API существует, но доступно только staff ролям:

- `ADMIN`
- `MANAGER`
- `RECEPTIONIST`

Роль `CUSTOMER` не имеет доступа к loyalty endpoint’ам.

### Итог

Для customer loyalty screen backend contract пока **не готов**.

## 6.6. Profile

Отдельного customer profile endpoint нет.

Нет endpoint’ов вида:

- `GET /api/me`
- `GET /api/customers/me`
- `PUT /api/customers/me`

## 6.7. Documents / files / photos

В текущем backend не найдено customer-facing documents API.

## 6.8. Booking / self-service order creation

Customer не может создать заказ напрямую:

- `POST /api/orders`
- `POST /api/orders/drop-off`

доступны только staff ролям.

Это полностью совпадает с замечанием из implementation plan о missing booking contracts.

---

## 7. Подробное API

## 7.1. Orders API

Контроллер: `src/main/java/com/vladko/autoshopcore/order/controller/OrderController.java:22`

### 7.1.1. GET `/api/orders/{id}`

Получить детали заказа.

**Customer access:** да  
**Customer-safe:** да, потому что в сервисе вызывается `requireCustomerAccess(order)`

См.:

- `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:110`
- `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java:40`

#### Response DTO

`OrderResponseDTO`:

```json
{
  "id": 5,
  "customerId": 12,
  "customerFirstName": "Ivan",
  "customerLastName": "Petrov",
  "customerEmail": "ivan@test.com",
  "customerPhoneNumber": "+79990001122",
  "vehicleId": 3,
  "vehicleBrand": "BMW",
  "vehicleModel": "X5",
  "vehicleVin": "WBA12345678901234",
  "vehicleLicensePlate": "A123AA77",
  "employeeId": 7,
  "employeeFirstName": "Pavel",
  "employeeLastName": "Ivanov",
  "employeeEmail": "mechanic@test.com",
  "problem": "Шум в подвеске",
  "status": "WAITING_FOR_PART",
  "crmStatus": "WAITING_FOR_PART",
  "legacyStatus": "IN_PROGRESS",
  "plannedVisitAt": "2026-05-15T15:30:00Z",
  "plannedSlotMinutes": 60,
  "bookingChannel": "WEB",
  "intakeNotes": "Проверить переднюю ось",
  "requiresOwnerApprovalForEveryExtraWork": true,
  "plannedDropOff": false,
  "checkedInAt": null,
  "readyForOwnerAt": null,
  "handedOverAt": null,
  "cancelledAt": null,
  "cancellationReason": null,
  "laborTotal": 5000.00,
  "partsTotal": 7000.00,
  "costsTotal": 12000.00,
  "manualDiscountAmount": 0.00,
  "pointsDiscountAmount": 0.00,
  "loyaltyPointsSpent": 0,
  "discountAmount": 0.00,
  "finalAmount": 12000.00,
  "createdAt": "2026-05-14T10:00:00Z",
  "updatedAt": "2026-05-14T12:00:00Z",
  "completedAt": null,
  "serviceLines": [
    {
      "serviceId": 11,
      "serviceName": "Диагностика подвески",
      "price": 1500.00
    }
  ]
}
```

Контракт DTO:

- `src/main/java/com/vladko/autoshopcore/order/dto/OrderResponseDTO.java:20`
- `src/main/java/com/vladko/autoshopcore/order/dto/OrderServiceLineDTO.java:8`

### 7.1.2. GET `/api/orders/customer/{customerId}`

Получить список заказов по `customerId`.

**Customer access:** технически да  
**Customer-safe:** условно / не гарантировано

В сервисе нет явного `requireCustomerAccess` на этот endpoint:

- `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:277`

#### Response

`OrderResponseDTO[]`

#### Рекомендация для фронта

Использовать только если backend-гарантии будут подтверждены отдельно, либо если customerId берётся из trusted backend-for-frontend слоя.

### 7.1.3. GET `/api/orders/vehicle/{vehicleId}`

Получить список заказов по автомобилю.

**Customer access:** технически да  
**Customer-safe:** условно / не гарантировано

В сервисе нет customer ownership check:

- `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:284`

#### Response

`OrderResponseDTO[]`

### 7.1.4. PUT `/api/orders/{id}/status`

Обновить статус заказа.

**Customer access:** частично да  
**Реально для customer сценария:** отмена заказа клиентом

В сервисе переход в `CANCELLED_BY_CUSTOMER` разрешён ролям:

- `CUSTOMER`
- `ADMIN`
- `MANAGER`

См. `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:593`

#### Request body

```json
{
  "status": "CANCELLED_BY_CUSTOMER",
  "cancellationReason": "CUSTOMER_CANCELLED"
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/order/dto/OrderStatusUpdateDTO.java:11`
- `src/main/java/com/vladko/autoshopcore/order/entity/OrderStatus.java:3`
- `src/main/java/com/vladko/autoshopcore/order/entity/CancellationReason.java:3`

#### Frontend usage

Использовать как action для:

- отмены бронирования/визита клиентом;
- отмены ремонта до завершения, если backend допускает переход из текущего статуса.

#### Важное ограничение

Не каждый статус можно перевести в `CANCELLED_BY_CUSTOMER`. Логика переходов жёстко проверяется в сервисе.

### 7.1.5. Остальные orders endpoint’ы

Есть, но для customer web сейчас не являются целевыми:

- `POST /api/orders`
- `POST /api/orders/drop-off`
- `PUT /api/orders/{id}`
- `PUT /api/orders/{id}/assign`
- `PUT /api/orders/{id}/estimate`
- `PUT /api/orders/{id}/check-in`
- `PUT /api/orders/{id}/no-show`
- `GET /api/orders/status/{status}`
- `GET /api/orders/bookings`
- `GET /api/orders/bookings/daily`
- `GET /api/orders/bookings/unassigned`
- `GET /api/orders/my`

Для customer UI их лучше не использовать.

---

## 7.2. Order Timeline API

Контроллер: `src/main/java/com/vladko/autoshopcore/order/timeline/controller/OrderTimelineController.java:12`

### 7.2.1. GET `/api/orders/{orderId}/timeline/customer`

Получить клиентскую timeline-ленту заказа.

**Customer access:** да  
**Customer-safe:** да

Проверка ownership есть в:

- `src/main/java/com/vladko/autoshopcore/order/timeline/service/OrderTimelineServiceImpl.java:47`

Из timeline автоматически исключаются `STAFF_ONLY` записи.

#### Response

`OrderTimelineEntryResponseDTO[]`

```json
[
  {
    "id": 1001,
    "eventType": "ORDER_BOOKED",
    "actorType": "CUSTOMER",
    "actorId": 12,
    "effectiveStatus": "WAITING_FOR_VISIT",
    "summary": "Запись создана",
    "detailsJson": "{\"channel\":\"WEB\"}",
    "occurredAt": "2026-05-14T10:00:00Z"
  }
]
```

DTO:

- `src/main/java/com/vladko/autoshopcore/order/timeline/dto/OrderTimelineEntryResponseDTO.java:11`

Enums:

- `OrderTimelineEventType`: `src/main/java/com/vladko/autoshopcore/order/timeline/entity/OrderTimelineEventType.java:3`
- `OrderTimelineActorType`: `src/main/java/com/vladko/autoshopcore/order/timeline/entity/OrderTimelineActorType.java:3`

#### Как использовать на фронте

Подходит для:

- timeline section на order details;
- timeline preview на dashboard;
- derived user-facing event labels через frontend dictionary.

#### Важное замечание

`detailsJson` приходит строкой JSON. Фронт должен:

- либо парсить её безопасно;
- либо относиться к ней как к opaque payload;
- либо использовать только `summary` + `eventType`.

---

## 7.3. Order Approvals API

Контроллер: `src/main/java/com/vladko/autoshopcore/order/approval/controller/OrderApprovalController.java:13`

Это один из самых зрелых customer flows в проекте.

### 7.3.1. GET `/api/orders/{orderId}/approvals`

Получить все approval request’ы по заказу.

**Customer access:** да  
**Customer-safe:** да

Список используется для секции согласований на order details и на dashboard.

#### Response

`OrderApprovalRequestResponseDTO[]`

```json
[
  {
    "requestId": 41,
    "orderId": 5,
    "proposalId": 22,
    "approvalType": "MIXED_SCOPE_CHANGE",
    "requestStatus": "OPEN",
    "proposalStatus": "PENDING_APPROVAL",
    "requestToken": "token-abc",
    "title": "Замена передних колодок",
    "description": "Обнаружен критический износ",
    "laborAmount": 2500.00,
    "partsAmount": 4300.00,
    "totalAmount": 6800.00,
    "requestedAt": "2026-05-14T11:00:00Z",
    "expiresAt": "2026-05-16T11:00:00Z",
    "customerContactChannel": "PHONE",
    "requestedPart": {
      "id": 71,
      "orderId": 5,
      "articleNumber": "0986AB1234",
      "brand": "BOSCH",
      "name": "Brake Pads",
      "umapiArticleId": 12345,
      "matchedLocalPartId": null,
      "requestedQuantity": 1,
      "status": "PENDING_CUSTOMER_APPROVAL",
      "selectedSupplier": null,
      "selectedQuoteSignature": null,
      "purchasePrice": null,
      "salePrice": null,
      "currency": null,
      "deliveryDaysMin": null,
      "deliveryDaysMax": null,
      "quoteFetchedAt": null,
      "orderedAt": null,
      "receivedAt": null,
      "createdAt": "2026-05-14T11:00:00Z",
      "updatedAt": "2026-05-14T11:00:00Z"
    }
  }
]
```

Контракт DTO:

- `src/main/java/com/vladko/autoshopcore/order/approval/dto/OrderApprovalRequestResponseDTO.java:13`
- `src/main/java/com/vladko/autoshopcore/parts/dto/OrderRequestedPartResponseDTO.java:12`

Enums:

- `OrderApprovalRequestStatus`: `src/main/java/com/vladko/autoshopcore/order/approval/entity/OrderApprovalRequestStatus.java:3`
- `OrderApprovalType`: `src/main/java/com/vladko/autoshopcore/order/approval/entity/OrderApprovalType.java:3`
- `OrderWorkProposalStatus`: `src/main/java/com/vladko/autoshopcore/order/approval/entity/OrderWorkProposalStatus.java:3`

### 7.3.2. POST `/api/orders/{orderId}/approvals/{requestId}/approve`

Подтвердить допработы / согласование.

**Customer access:** да  
**Customer-safe:** да

#### Request body

```json
{
  "decisionToken": "token-abc",
  "comment": "Согласен"
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/order/approval/dto/OrderApprovalDecisionCreateDTO.java:7`

#### Response

Возвращает обновлённый `OrderApprovalRequestResponseDTO`.

#### Поведение

- backend проверяет принадлежность заказа клиенту;
- backend использует `decisionToken`;
- при повторных запросах учитывается idempotency logic.

### 7.3.3. POST `/api/orders/{orderId}/approvals/{requestId}/reject`

Отклонить согласование.

**Customer access:** да  
**Customer-safe:** да

#### Request body

```json
{
  "decisionToken": "token-abc",
  "comment": "Пока не готов"
}
```

#### Response

Обновлённый `OrderApprovalRequestResponseDTO`.

### 7.3.4. Customer UX рекомендации

Для `FrontClient` approvals можно считать fully implementable:

- pending approvals list
- approval detail card
- approve CTA
- reject CTA
- resolved approvals history

---

## 7.4. Loyalty API

Контроллеры:

- `src/main/java/com/vladko/autoshopcore/loyalty/controller/LoyaltyController.java:18`
- `src/main/java/com/vladko/autoshopcore/loyalty/controller/OrderLoyaltyController.java:17`

### Текущее состояние для customer web

Loyalty API **существует**, но роль `CUSTOMER` к нему не допущена.

Security rule:

- `GET /api/loyalty/**` доступен только `ADMIN`, `MANAGER`, `RECEPTIONIST`
- `PUT/DELETE /api/orders/{id}/loyalty/**` также staff-only

См. `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:82` и `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:116`

### Существующие endpoint’ы

1. `GET /api/loyalty/settings`
2. `GET /api/loyalty/accounts/customer/{customerId}`
3. `GET /api/loyalty/accounts/{accountId}/transactions`
4. `GET /api/loyalty/tiers`
5. `PUT /api/orders/{orderId}/loyalty/spend`
6. `DELETE /api/orders/{orderId}/loyalty/spend`

### Почему это важно для фронта

Экран loyalty по дизайну нужен, но **напрямую customer client его сейчас не подключит**.

### Варианты решения

1. Открыть customer-safe loyalty read endpoints под роль `CUSTOMER`
2. Сделать `/api/customers/me/loyalty`
3. Сделать BFF, который будет проксировать loyalty customer-safe способом

### Уже существующие DTO loyalty

- `LoyaltySettingsResponseDTO`: `src/main/java/com/vladko/autoshopcore/loyalty/dto/LoyaltySettingsResponseDTO.java:6`
- `LoyaltyAccountResponseDTO`: `src/main/java/com/vladko/autoshopcore/loyalty/dto/LoyaltyAccountResponseDTO.java:11`
- `LoyaltyTierResponseDTO`: `src/main/java/com/vladko/autoshopcore/loyalty/dto/LoyaltyTierResponseDTO.java:10`
- `LoyaltyTransactionResponseDTO`: `src/main/java/com/vladko/autoshopcore/loyalty/dto/LoyaltyTransactionResponseDTO.java:12`

---

## 7.5. Vehicles API

Контроллер: `src/main/java/com/vladko/autoshopcore/vehicle/controller/VehicleController.java:16`

### Существующие endpoint’ы

1. `POST /api/vehicles`
2. `GET /api/vehicles/{id}`
3. `GET /api/vehicles/vin/{vin}`
4. `GET /api/vehicles/customer/{customerId}`
5. `PUT /api/vehicles/{id}`
6. `PUT /api/vehicles/{id}/catalog-link`
7. `DELETE /api/vehicles/{id}/catalog-link`
8. `DELETE /api/vehicles/{id}`

### Customer доступ

Сейчас роль `CUSTOMER` **не имеет доступа ни к одному** endpoint’у `/api/vehicles/**`.

Security rule:

- `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:103`

### DTO

#### VehicleResponseDTO

```json
{
  "id": 3,
  "customerId": 12,
  "brand": "BMW",
  "model": "X5",
  "vin": "WBA12345678901234",
  "licensePlate": "A123AA77",
  "umapiType": null,
  "umapiId": null,
  "createdAt": "2026-05-10T10:00:00Z"
}
```

См. `src/main/java/com/vladko/autoshopcore/vehicle/dto/VehicleResponseDTO.java:10`

#### VehicleCreateDTO

```json
{
  "customerId": 12,
  "brand": "BMW",
  "model": "X5",
  "vin": "WBA12345678901234",
  "licensePlate": "A123AA77"
}
```

См. `src/main/java/com/vladko/autoshopcore/vehicle/dto/VehicleCreateDTO.java:12`

### Вывод для FrontClient

Экран `Мои автомобили` и `Vehicle details` сейчас нельзя подключить напрямую под роль `CUSTOMER`.

---

## 7.6. Customer / Profile API

Контроллер: `src/main/java/com/vladko/autoshopcore/client/controller/CustomerController.java:15`

### Существующие endpoint’ы

1. `POST /api/customers`
2. `GET /api/customers/{id}`
3. `PUT /api/customers/{id}`
4. `DELETE /api/customers/{id}`
5. `GET /api/customers/search`

### Customer доступ

Роль `CUSTOMER` **не имеет доступа** к `/api/customers/**`.

Security rule:

- `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:95`

### DTO

#### CustomerResponseDTO

```json
{
  "id": 12,
  "firstName": "Ivan",
  "lastName": "Petrov",
  "email": "ivan@test.com",
  "phoneNumber": "+79990001122",
  "createdAt": "2026-05-10T10:00:00Z",
  "updatedAt": "2026-05-14T11:00:00Z"
}
```

См. `src/main/java/com/vladko/autoshopcore/client/dto/CustomerResponseDTO.java:11`

### Вывод для FrontClient

Экран `Профиль` сейчас требует нового customer-facing API.

---

## 7.7. CRM Search API

Контроллер: `src/main/java/com/vladko/autoshopcore/order/query/controller/OrderQueryController.java:14`

### Endpoint’ы

1. `GET /api/crm/orders/search`
2. `GET /api/crm/orders/queue-summary`

### Customer доступ

Недоступно роли `CUSTOMER`.

### Зачем это знать фронту

Некоторые данные тут были бы полезны для dashboard/staff search, но это **не customer API**.

---

## 8. Статусы и словари, которые нужны фронту

## 8.1. OrderStatus

Backend enum:

- `NEW`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`
- `WAITING_FOR_VISIT`
- `ACCEPTED`
- `DIAGNOSIS_IN_PROGRESS`
- `WAITING_FOR_OWNER_APPROVAL`
- `WAITING_FOR_PART`
- `REPAIR_IN_PROGRESS`
- `READY_FOR_OWNER`
- `HANDED_OVER`
- `CANCELLED_NO_SHOW`
- `CANCELLED_BY_CUSTOMER`
- `CANCELLED_INTERNAL`

Источник: `src/main/java/com/vladko/autoshopcore/order/entity/OrderStatus.java:3`

## 8.2. LegacyOrderStatus

Упрощённый статус:

- `NEW`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

Источник: `src/main/java/com/vladko/autoshopcore/order/entity/LegacyOrderStatus.java:3`

Для customer UI это полезно как fallback grouping.

## 8.3. BookingChannel

- `WALK_IN`
- `WEB`

Источник: `src/main/java/com/vladko/autoshopcore/order/entity/BookingChannel.java:3`

## 8.4. Approval словари

### Request status

- `OPEN`
- `APPROVED`
- `REJECTED`
- `EXPIRED`
- `CANCELLED`

### Approval type

- `EXTRA_WORK`
- `PART_ONLY`
- `MIXED_SCOPE_CHANGE`

### Proposal status

- `DRAFT`
- `PENDING_APPROVAL`
- `APPROVED`
- `REJECTED`
- `CONVERTED_TO_WORK`
- `CANCELLED`

Источники:

- `src/main/java/com/vladko/autoshopcore/order/approval/entity/OrderApprovalRequestStatus.java:3`
- `src/main/java/com/vladko/autoshopcore/order/approval/entity/OrderApprovalType.java:3`
- `src/main/java/com/vladko/autoshopcore/order/approval/entity/OrderWorkProposalStatus.java:3`

## 8.5. Timeline event dictionary

Timeline events:

- `ORDER_BOOKED`
- `VEHICLE_CHECKED_IN`
- `APPROVAL_REQUESTED`
- `APPROVAL_APPROVED`
- `APPROVAL_REJECTED`
- `WAITING_FOR_PART_ENTERED`
- `PART_ORDERED`
- `PART_RECEIVED`
- `REPAIR_RESUMED`
- `READY_FOR_OWNER_MARKED`
- `ORDER_CANCELLED`
- `VEHICLE_HANDED_OVER`
- `STATUS_CHANGED`

Источник: `src/main/java/com/vladko/autoshopcore/order/timeline/entity/OrderTimelineEventType.java:3`

---

## 9. Recommended frontend integration strategy

## 9.1. Что можно подключать прямо сейчас

### Orders page

Использовать:

- `GET /api/orders/customer/{customerId}` — только если customer identity trusted layer уже есть
- либо лучше будущий customer-safe endpoint

### Order details page

Использовать:

- `GET /api/orders/{id}`
- `GET /api/orders/{orderId}/timeline/customer`
- `GET /api/orders/{orderId}/approvals`

### Approval actions

Использовать:

- `POST /api/orders/{orderId}/approvals/{requestId}/approve`
- `POST /api/orders/{orderId}/approvals/{requestId}/reject`

### Cancel order action

Использовать:

- `PUT /api/orders/{id}/status`

с телом:

```json
{
  "status": "CANCELLED_BY_CUSTOMER",
  "cancellationReason": "CUSTOMER_CANCELLED"
}
```

## 9.2. Что лучше не подключать напрямую пока не появится новый backend contract

1. Profile
2. Vehicles
3. Loyalty
4. Documents
5. Booking creation
6. Customer dashboard aggregate

## 9.3. Лучший следующий backend пакет для FrontClient

Минимально нужный набор новых endpoint’ов:

1. `GET /api/customers/me`
2. `PUT /api/customers/me`
3. `GET /api/customers/me/vehicles`
4. `GET /api/customers/me/orders`
5. `GET /api/customers/me/loyalty`
6. `GET /api/customers/me/dashboard`
7. `GET /api/customers/me/documents`

---

## 10. Missing backend contracts относительно FrontClient плана

Это самое важное для roadmap.

### Profile

Нужно добавить:

- customer self profile read/update API

### Vehicles

Нужно добавить:

- customer-owned vehicles list/details API
- customer-safe vehicle history API

### Loyalty

Нужно добавить:

- customer loyalty overview
- loyalty transactions history
- maybe tier explanation payload

### Dashboard

Нужно добавить:

- aggregate dashboard endpoint с open orders, nearest visit, approvals summary, loyalty preview

### Documents

Нужно добавить:

- orders documents list
- photos/media list
- file metadata DTO

### Booking

Нужно добавить:

- customer booking create flow
- booking slot lookup
- service catalog subset for customer

### Notifications / communication

Нужно добавить:

- notification preferences
- maybe unread approvals / reminders counters

---

## 11. Готовность по фичам

| Feature | UI нужен | Backend endpoint есть | Customer access есть | Реально можно подключить |
|---|---|---:|---:|---:|
| Orders list | Да | Частично | Частично | Ограниченно |
| Order details | Да | Да | Да | Да |
| Timeline | Да | Да | Да | Да |
| Approvals list | Да | Да | Да | Да |
| Approve / reject | Да | Да | Да | Да |
| Cancel order | Да | Да | Да | Да |
| Vehicles | Да | Да | Нет | Нет |
| Loyalty overview | Да | Да | Нет | Нет |
| Loyalty history | Да | Да | Нет | Нет |
| Profile | Да | Частично staff-only | Нет | Нет |
| Documents | Да | Нет | Нет | Нет |
| Booking self-service | Нужен позже | Нет для customer | Нет | Нет |
| Dashboard aggregate | Да | Нет | Нет | Нет |

---

## 12. Рекомендуемый frontend contract layer

Для `FrontClient` лучше сразу сделать отдельный client API layer с чётким разделением:

### `ordersApi`

- `getOrderById(orderId)`
- `getOrdersByCustomer(customerId)` — временно, пока нет `/me/orders`
- `cancelOrder(orderId, comment?)`

### `orderTimelineApi`

- `getCustomerTimeline(orderId)`

### `orderApprovalApi`

- `getApprovals(orderId)`
- `approve(orderId, requestId, decisionToken, comment)`
- `reject(orderId, requestId, decisionToken, comment)`

### `profileApi`

Пока только placeholder, до появления backend contract.

### `vehiclesApi`

Пока только placeholder, до появления customer access.

### `loyaltyApi`

Пока только placeholder, до появления customer access.

---

## 13. Key insights

1. Самый готовый customer flow сейчас — это `order details + timeline + approvals + cancel`.
2. Loyalty уже реализован в домене, но не открыт для customer роли.
3. Vehicles и profile в домене есть, но не доступны customer напрямую.
4. Customer dashboard пока должен собираться либо на фронте из нескольких endpoint’ов, либо через новый aggregate contract.
5. Некоторые order-list endpoint’ы формально доступны customer, но не выглядят как полностью customer-safe API. Для product-grade web client лучше сделать отдельные `/me/*` маршруты.
6. FrontClient уже можно начать с MVP вокруг order journey, а profile / vehicles / loyalty оставить как backend follow-up phases.

---

## 14. Рекомендуемый следующий шаг

Для полноценного подключения всего клиентского фронта я бы рекомендовал следующим backend-PR сделать customer-safe API пакет:

1. `/api/customers/me`
2. `/api/customers/me/orders`
3. `/api/customers/me/vehicles`
4. `/api/customers/me/loyalty`
5. `/api/customers/me/dashboard`
6. `/api/customers/me/documents`

Это резко упростит frontend и уберёт зависимость от staff-oriented route structure.
