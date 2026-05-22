# CRM Customer Self-Service Frontend API Guide

## Цель документа

Этот документ фиксирует **все новые customer self-service API**, которые были добавлены в `autoshop-core`, и объясняет, **что именно должен сделать Front**, чтобы замкнуть полный пользовательский контур.

Документ нужен как bridge между backend и frontend:

- какие endpoint’ы теперь доступны клиенту;
- какие данные они принимают и возвращают;
- какие сценарии UI можно закрыть прямо сейчас;
- что ещё остаётся сделать на фронте, чтобы цепочка стала end-to-end.

---

## Главная продуктовая цель

После текущего пакета backend-изменений клиент в своём кабинете должен получить полноценный self-service flow:

1. посмотреть свои машины;
2. добавить машину;
3. изменить машину;
4. удалить машину, если нет активных заказов;
5. выбрать машину для записи;
6. посмотреть доступные услуги;
7. посмотреть доступные слоты записи;
8. создать booking;
9. открыть детали своего заказа;
10. изменить booking до check-in;
11. отменить booking;
12. открыть список документов:
    - своих;
    - по машине;
    - по заказу;
13. открыть presigned download URL для файла;
14. просмотреть approvals по заказу;
15. approve / reject approval из self-service namespace.

---

## Backend status summary

### Уже реализовано в backend

#### Vehicles

- `GET /api/customers/me/vehicles`
- `GET /api/customers/me/vehicles/{vehicleId}`
- `POST /api/customers/me/vehicles`
- `PUT /api/customers/me/vehicles/{vehicleId}`
- `DELETE /api/customers/me/vehicles/{vehicleId}`

Контроллер:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerVehicleSelfServiceController.java:15`

#### Booking

- `GET /api/customers/me/booking/services`
- `GET /api/customers/me/booking/slots`
- `POST /api/customers/me/bookings`
- `PUT /api/customers/me/bookings/{orderId}`
- `POST /api/customers/me/bookings/{orderId}/cancel`
- `GET /api/customers/me/orders/{orderId}`

Контроллеры:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerBookingSelfServiceController.java:16`
- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerBookingSlotController.java:15`

#### Files / documents

- `GET /api/customers/me/documents`
- `GET /api/customers/me/vehicles/{vehicleId}/documents`
- `GET /api/customers/me/orders/{orderId}/documents`
- `POST /api/customers/me/files/{fileId}/presigned-download-url`

Контроллер:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerFilesSelfServiceController.java:15`

#### Approvals aliases

- `GET /api/customers/me/orders/{orderId}/approvals`
- `POST /api/customers/me/orders/{orderId}/approvals/{requestId}/approve`
- `POST /api/customers/me/orders/{orderId}/approvals/{requestId}/reject`

Контроллер:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerApprovalSelfServiceController.java:18`

---

## Customer API catalog

## 1. Vehicles API

## 1.1. Get current customer vehicles

### Endpoint

`GET /api/customers/me/vehicles`

### Purpose

Показать экран `Мои автомобили`.

### Response

`VehicleResponseDTO[]`

Пример:

```json
[
  {
    "id": 15,
    "customerId": 45,
    "brand": "BMW",
    "model": "X5",
    "vin": "WBA12345678901234",
    "licensePlate": "A123AA77",
    "umapiType": null,
    "umapiManufacturerId": null,
    "umapiManufacturerName": null,
    "umapiModelSeriesId": null,
    "umapiModelSeriesName": null,
    "umapiModificationId": null,
    "umapiModificationName": null,
    "umapiEngineDescription": null,
    "umapiCatalogLinkedAt": null,
    "createdAt": "2026-05-14T20:00:00Z",
    "updatedAt": "2026-05-14T20:00:00Z"
  }
]
```

### Front usage

Использовать как primary source для:

- список машин клиента;
- vehicle selector в booking flow;
- быстрый summary на dashboard/booking.

---

## 1.2. Get one vehicle

### Endpoint

`GET /api/customers/me/vehicles/{vehicleId}`

### Purpose

Открыть экран деталей конкретной машины.

### Response

`VehicleResponseDTO`

### Front usage

Использовать для:

- vehicle details page;
- prefill при edit vehicle;
- documents section по машине;
- history-by-vehicle composition на фронте.

---

## 1.3. Create vehicle

### Endpoint

`POST /api/customers/me/vehicles`

### Request

```json
{
  "brand": "BMW",
  "model": "X5",
  "vin": "WBA12345678901234",
  "licensePlate": "A123AA77"
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerVehicleCreateDTO.java:1`

### Response

`VehicleResponseDTO`

### Front usage

Новый сценарий:

- `Add vehicle` modal/page;
- после success:
  - закрыть форму;
  - обновить vehicles list;
  - optional: сразу выбрать эту машину в booking flow.

### Validation / UX expectations

- VIN обязателен;
- license plate обязателен;
- дубликаты VIN/plate вернут conflict/error;
- `customerId` фронт не передаёт.

---

## 1.4. Update vehicle

### Endpoint

`PUT /api/customers/me/vehicles/{vehicleId}`

### Request

```json
{
  "brand": "BMW",
  "model": "X5 LCI",
  "vin": "WBA12345678901234",
  "licensePlate": "A123AA77"
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerVehicleUpdateDTO.java:1`

### Response

`VehicleResponseDTO`

### Front usage

Новый сценарий:

- edit vehicle form;
- optimistic refresh vehicle details/list after success.

---

## 1.5. Delete vehicle

### Endpoint

`DELETE /api/customers/me/vehicles/{vehicleId}`

### Purpose

Удалить машину клиента.

### Behavior

Удаление запрещено, если по машине есть активные заказы.

Current backend rule:

- `WAITING_FOR_VISIT`
- `ACCEPTED`
- `DIAGNOSIS_IN_PROGRESS`
- `WAITING_FOR_OWNER_APPROVAL`
- `WAITING_FOR_PART`
- `REPAIR_IN_PROGRESS`
- `READY_FOR_OWNER`

См.:

- `src/main/java/com/vladko/autoshopcore/client/service/CustomerVehicleSelfServiceImpl.java:28`

### Front usage

Новый сценарий:

- delete vehicle CTA;
- confirmation modal;
- если backend вернул `400`, показать понятное сообщение:
  - “Нельзя удалить автомобиль, пока по нему есть активные заказы”.

---

## 2. Booking catalog API

## 2.1. Get available booking services

### Endpoint

`GET /api/customers/me/booking/services`

### Purpose

Получить customer-safe подмножество услуг для booking flow.

### Response

`CustomerBookingServiceCatalogItemDTO[]`

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingServiceCatalogItemDTO.java:1`

Пример:

```json
[
  {
    "id": 7,
    "name": "Brake inspection",
    "description": "Diagnostics and visual brake inspection",
    "basePrice": 100.00,
    "categoryId": 2,
    "categoryName": "Diagnostics",
    "defaultDurationMinutes": 90
  }
]
```

### Front usage

Использовать для:

- service picker;
- grouped category list;
- предварительного расчёта expected duration;
- составления booking summary before submit.

### Important

Это read-only customer-safe контракт.

Фронт не должен использовать admin/staff `/api/service-catalog` напрямую.

---

## 3. Slot lookup API

## 3.1. Lookup booking slots

### Endpoint

`GET /api/customers/me/booking/slots`

### Query params

- `vehicleId` — required
- `serviceIds` — optional, repeatable or list-style on frontend adapter
- `dateFrom` — optional, ISO date
- `days` — optional
- `slotMinutes` — optional override

### Example

```http
GET /api/customers/me/booking/slots?vehicleId=15&serviceIds=1&serviceIds=2&dateFrom=2026-05-20&days=3
```

### Response

`CustomerBookingSlotResponseDTO[]`

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingSlotResponseDTO.java:1`

Пример:

```json
[
  {
    "startAt": "2026-05-20T09:00:00Z",
    "slotMinutes": 90,
    "available": true,
    "availableEmployeeCount": 2
  },
  {
    "startAt": "2026-05-20T10:00:00Z",
    "slotMinutes": 90,
    "available": false,
    "availableEmployeeCount": 0
  }
]
```

### Backend behavior

- проверяет ownership машины;
- если `slotMinutes` не передан, пытается вычислить длительность по выбранным активным услугам;
- использует internal availability search;
- не раскрывает имена сотрудников, только агрегированную доступность.

Сервис:

- `src/main/java/com/vladko/autoshopcore/client/service/CustomerBookingSlotServiceImpl.java:22`

### Front usage

Это основной endpoint для:

- календаря/слотов в booking wizard;
- выбора времени без знания о механиках;
- предзаписи клиента на свободный интервал.

### Front requirements

Frontend должен:

1. выбрать машину;
2. выбрать услуги;
3. вызвать `slot lookup`;
4. показать только `available=true` слоты;
5. сохранить выбранный `startAt` и `slotMinutes`;
6. подставить их в create booking request.

### UX recommendation

Если нет `serviceIds`, можно:

- либо просить сначала выбрать услуги;
- либо использовать fallback duration;
- лучше явно не давать идти дальше без выбора услуг, если booking flow должен быть точным.

---

## 4. Booking create API

## 4.1. Create booking

### Endpoint

`POST /api/customers/me/bookings`

### Request

```json
{
  "vehicleId": 15,
  "plannedVisitAt": "2026-05-20T09:00:00Z",
  "plannedSlotMinutes": 90,
  "problem": "Brake noise",
  "intakeNotes": "Need morning appointment",
  "selectedServiceIds": [1, 2]
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingCreateDTO.java:1`

### Response

`OrderResponseDTO`

### Backend behavior

- `customerId` вычисляется из auth context;
- `employeeId` клиент не передаёт и не контролирует;
- `bookingChannel` сервер выставляет сам;
- ownership машины проверяется сервером.

Сервис:

- `src/main/java/com/vladko/autoshopcore/client/service/CustomerBookingSelfServiceImpl.java:27`

### Front usage

Это submit-step booking wizard.

### Front flow

1. выбрать машину;
2. выбрать услуги;
3. выбрать слот;
4. optional problem/intake notes;
5. вызвать `POST /bookings`;
6. перейти на booking success / order details page.

---

## 5. Booking update API

## 5.1. Update booking

### Endpoint

`PUT /api/customers/me/bookings/{orderId}`

### Request

```json
{
  "plannedVisitAt": "2026-05-21T10:00:00Z",
  "plannedSlotMinutes": 120,
  "problem": "Brake noise on low speed",
  "intakeNotes": "Changed request",
  "selectedServiceIds": [1, 2]
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingUpdateDTO.java:1`

### Response

`OrderResponseDTO`

### Backend behavior

Customer update разрешён только для статусов:

- `NEW`
- `WAITING_FOR_VISIT`

См.:

- `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:449`

### Front usage

Это основа для:

- `Reschedule booking`
- `Edit booking request`

### Front requirement

На UI показывать `Edit/Reschedule` только если заказ ещё не checked-in и находится в pre-visit stage.

---

## 6. Booking cancel API

## 6.1. Cancel booking

### Endpoint

`POST /api/customers/me/bookings/{orderId}/cancel`

### Request body

Пока пустой.

### Response

`OrderResponseDTO`

Ожидаемый статус:

- `CANCELLED_BY_CUSTOMER`

### Front usage

Это отдельная cancel action на details/list card.

### Front requirement

- показать confirm modal;
- после успеха обновить order details/list;
- статус отразить как `Cancelled by customer`.

---

## 7. Customer order details API

## 7.1. Get one order

### Endpoint

`GET /api/customers/me/orders/{orderId}`

### Purpose

Открыть details screen заказа в self-service namespace.

### Response

`OrderResponseDTO`

### Front usage

Использовать как primary source для:

- order details page;
- reschedule / cancel controls;
- approvals block;
- documents section по заказу.

---

## 8. Files / documents API

## Important architecture note

Файлы физически не хранятся в `autoshop-core`.

`core` теперь предоставляет **customer-safe facade** поверх внешнего `autoshop-files` API.

То есть фронт работает через `core`, а не ходит напрямую в files-service за customer documents list.

---

## 8.1. Get current customer documents

### Endpoint

`GET /api/customers/me/documents`

### Purpose

Список документов текущего клиента.

### Response

`CustomerFileMetadataDTO[]`

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerFileMetadataDTO.java:1`

Пример:

```json
[
  {
    "fileId": "f-001",
    "filename": "passport.pdf",
    "category": "CUSTOMER_DOCUMENT",
    "ownerType": "CUSTOMER",
    "ownerId": "45",
    "contentType": "application/pdf",
    "sizeBytes": 54321,
    "status": "AVAILABLE",
    "createdAt": "2026-05-15T10:00:00Z"
  }
]
```

### Front usage

Использовать для:

- profile/account documents section;
- list/table/cards of customer files.

---

## 8.2. Get vehicle documents

### Endpoint

`GET /api/customers/me/vehicles/{vehicleId}/documents`

### Purpose

Документы/файлы машины клиента.

### Front usage

Использовать в:

- vehicle details screen;
- vehicle documents tab/section.

---

## 8.3. Get order documents

### Endpoint

`GET /api/customers/me/orders/{orderId}/documents`

### Purpose

Документы/файлы заказа клиента.

### Front usage

Использовать в:

- order details screen;
- estimate/report/documents section.

---

## 8.4. Get presigned download URL

### Endpoint

`POST /api/customers/me/files/{fileId}/presigned-download-url`

### Response

```json
{
  "url": "https://files.example/presigned/..."
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerFileDownloadUrlResponseDTO.java:1`

### Front usage

Flow:

1. user clicks file row/card;
2. Front calls presigned-download endpoint;
3. Front opens returned URL in new tab or downloads file.

### Important

На фронте не надо строить files download URL вручную.

---

## 8.5. Что по files ещё НЕ реализовано

Пока backend реализует только customer-safe read/download facade:

- list files
- validate ownership
- request presigned download URL

Пока **не реализовано** в этом пакете:

- customer upload file
- customer delete file
- customer edit file metadata

То есть для полного documents UX фронту пока можно делать:

- preview/list/download

но нельзя ещё делать полноценный upload flow через новые customer endpoints.

---

## 9. Approvals alias API

## 9.1. Get order approvals

### Endpoint

`GET /api/customers/me/orders/{orderId}/approvals`

### Purpose

Self-service alias для approvals list без знания старого `/api/orders/{id}/approvals` namespace.

### Response

`OrderApprovalRequestResponseDTO[]`

### Front usage

Использовать в order details page.

---

## 9.2. Approve request

### Endpoint

`POST /api/customers/me/orders/{orderId}/approvals/{requestId}/approve`

### Request

```json
{
  "decisionToken": "token-123",
  "comment": "Approved"
}
```

DTO:

- `src/main/java/com/vladko/autoshopcore/order/approval/dto/OrderApprovalDecisionCreateDTO.java:1`

### Front usage

- approve button in approvals card/modal;
- decision token должен генерироваться на фронте как idempotency token.

---

## 9.3. Reject request

### Endpoint

`POST /api/customers/me/orders/{orderId}/approvals/{requestId}/reject`

### Request

```json
{
  "decisionToken": "token-456",
  "comment": "Too expensive"
}
```

### Front usage

- reject action in approval review flow.

---

## Что именно должен сделать Front

## Phase A — Vehicles management

### Нужно сделать

1. Новый API module:
   - `clientVehiclesApi`
2. Экран `My Vehicles` должен уметь:
   - list
   - create
   - update
   - delete
3. Vehicle details screen:
   - load by vehicleId
   - show documents section
   - optionally show orders history composed via existing orders API

### Endpoint mapping

- list → `GET /api/customers/me/vehicles`
- details → `GET /api/customers/me/vehicles/{vehicleId}`
- create → `POST /api/customers/me/vehicles`
- update → `PUT /api/customers/me/vehicles/{vehicleId}`
- delete → `DELETE /api/customers/me/vehicles/{vehicleId}`

### UI goal

Закрыть полный vehicle self-service lifecycle.

---

## Phase B — Booking wizard

### Нужно сделать

1. Новый API module:
   - `clientBookingApi`
2. Wizard steps:
   - select vehicle
   - select services
   - lookup slots
   - select slot
   - enter problem/notes
   - submit booking
3. После submit:
   - показать success state;
   - перейти в order details.

### Endpoint mapping

- services → `GET /api/customers/me/booking/services`
- slots → `GET /api/customers/me/booking/slots`
- create → `POST /api/customers/me/bookings`

### UI goal

Полный self-service booking creation без участия staff UI.

---

## Phase C — Booking management

### Нужно сделать

На order details / bookings list:

- reschedule booking
- edit selected services/problem/notes
- cancel booking

### Endpoint mapping

- details → `GET /api/customers/me/orders/{orderId}`
- update → `PUT /api/customers/me/bookings/{orderId}`
- cancel → `POST /api/customers/me/bookings/{orderId}/cancel`

### UI goal

Клиент сам управляет своей записью до check-in.

---

## Phase D — Approvals flow cleanup

### Нужно сделать

Переключить Front на self-service alias routes, чтобы не использовать legacy order namespace в customer UI.

### Endpoint mapping

- approvals list → `GET /api/customers/me/orders/{orderId}/approvals`
- approve → `POST /api/customers/me/orders/{orderId}/approvals/{requestId}/approve`
- reject → `POST /api/customers/me/orders/{orderId}/approvals/{requestId}/reject`

### UI goal

Чистый customer order journey в одном namespace.

---

## Phase E — Documents read/download

### Нужно сделать

1. Новый API module:
   - `clientDocumentsApi`
2. Documents sections:
   - profile documents
   - vehicle documents
   - order documents
3. Download action:
   - request presigned URL
   - open/download file

### Endpoint mapping

- customer docs → `GET /api/customers/me/documents`
- vehicle docs → `GET /api/customers/me/vehicles/{vehicleId}/documents`
- order docs → `GET /api/customers/me/orders/{orderId}/documents`
- download URL → `POST /api/customers/me/files/{fileId}/presigned-download-url`

### UI goal

Закрыть documents preview/list/download chain.

---

## Что Front ещё не сможет сделать полностью

### 1. File upload

Пока customer upload endpoints нет.

Значит пока можно закрыть:

- list
- preview
- download

Но нельзя закрыть:

- upload customer file
- upload vehicle file
- upload order attachment

### 2. File delete

Пока нет customer-safe delete facade.

### 3. Full slot reservation semantics

Сейчас slot lookup — это availability preview.

Это значит:

- слот подбирается красиво;
- но окончательная реальная запись всё равно происходит на `POST /bookings`.

Frontend должен учитывать race-condition сценарий:

- slot lookup показал свободно;
- к моменту submit booking слот мог стать занят.

Нужен нормальный user-facing error handling на submit booking.

---

## Recommended frontend module split

### 1. `clientVehiclesApi.ts`

Методы:

- `getVehicles()`
- `getVehicle(vehicleId)`
- `createVehicle(payload)`
- `updateVehicle(vehicleId, payload)`
- `deleteVehicle(vehicleId)`

### 2. `clientBookingApi.ts`

Методы:

- `getBookingServices()`
- `lookupSlots(params)`
- `createBooking(payload)`
- `updateBooking(orderId, payload)`
- `cancelBooking(orderId)`
- `getOrder(orderId)`

### 3. `clientApprovalsApi.ts`

Методы:

- `getOrderApprovals(orderId)`
- `approve(orderId, requestId, payload)`
- `reject(orderId, requestId, payload)`

### 4. `clientDocumentsApi.ts`

Методы:

- `getMyDocuments()`
- `getVehicleDocuments(vehicleId)`
- `getOrderDocuments(orderId)`
- `getPresignedDownloadUrl(fileId)`

---

## End-to-end chain that Front can now close

После этого backend пакета frontend уже может закрыть такую цепочку полностью:

1. Клиент логинится.
2. Клиент открывает `My Vehicles`.
3. Клиент добавляет машину.
4. Клиент запускает booking wizard.
5. Клиент выбирает машину.
6. Клиент выбирает услуги.
7. Front получает доступные слоты.
8. Клиент выбирает слот.
9. Front создаёт booking.
10. Front открывает order details.
11. Клиент может:
    - изменить booking;
    - отменить booking;
    - посмотреть approvals;
    - approve/reject;
    - посмотреть документы заказа;
    - скачать документ.
12. Клиент может открыть документы машины и свои документы.

Это и есть основной self-service chain, которую теперь можно закрыть на Front почти полностью.

---

## Что рекомендовано сделать следующим backend пакетом

Чтобы цепочка была 100% завершённой, следующим небольшим пакетом стоит добавить:

1. customer file upload facade;
2. customer file delete facade;
3. optional customer order timeline alias;
4. optional richer booking slot heuristics;
5. optional booking conflict-specific error contract для frontend UX.

---

## Final frontend target

Frontend should aim to deliver these finished user journeys:

### Journey 1 — Vehicle management

- list vehicles
- create vehicle
- edit vehicle
- delete vehicle

### Journey 2 — Booking

- choose vehicle
- choose services
- lookup slots
- create booking
- edit booking
- cancel booking

### Journey 3 — Order journey

- open order details
- review approvals
- approve / reject
- read/download order documents

### Journey 4 — Documents

- read customer documents
- read vehicle documents
- read order documents
- open presigned download URL

Когда Front закроет эти 4 journeys, customer self-service будет выглядеть цельно и профессионально даже без upload phase.
