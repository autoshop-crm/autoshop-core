# CRM Customer Self-Service Implementation Plan

## Цель

Перевести customer API из режима **только просмотра** в полноценный **self-service контур** без раскрытия внутренних CRM-механик.

Клиент в своём кабинете должен уметь:

- управлять своими автомобилями;
- создавать запись на приём;
- выбирать услуги из стандартного каталога;
- переносить или отменять свою запись в разрешённых статусах;
- просматривать свои заказы и детали заказа;
- подтверждать или отклонять согласования по заказу.

Клиент **не должен**:

- управлять назначением сотрудников;
- видеть внутренние CRM-статусы и служебные переходы;
- влиять на estimate/parts/procurement/workflow вне customer-safe сценариев;
- передавать `customerId`, `employeeId` и другие внутренние идентификаторы, которые должны определяться сервером.

---

## Primary Locations

### Текущий self-service слой

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerSelfServiceController.java:17`
- `src/main/java/com/vladko/autoshopcore/client/service/CustomerSelfServiceImpl.java:30`

Сейчас этот слой покрывает:

- `GET /api/customers/me`
- `PUT /api/customers/me`
- `GET /api/customers/me/orders`
- `GET /api/customers/me/vehicles`
- `GET /api/customers/me/loyalty`
- `GET /api/customers/me/dashboard`

### Идентификация текущего клиента

- `src/main/java/com/vladko/autoshopcore/customerauth/service/CustomerIdentityLinkService.java:23`
- `src/main/java/com/vladko/autoshopcore/client/service/CustomerSelfServiceImpl.java:112`

Это основной building block для всех новых customer-safe операций.

### Staff vehicle CRUD, который уже можно переиспользовать

- `src/main/java/com/vladko/autoshopcore/vehicle/controller/VehicleController.java:16`
- `src/main/java/com/vladko/autoshopcore/vehicle/service/VehicleServiceImpl.java:31`

Уже есть готовые операции:

- create vehicle;
- get vehicle;
- list customer vehicles;
- update vehicle;
- delete vehicle;
- catalog link/unlink.

### Staff order / booking logic, который уже можно переиспользовать

- `src/main/java/com/vladko/autoshopcore/order/controller/OrderController.java:22`
- `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:345`
- `src/main/java/com/vladko/autoshopcore/order/dto/OrderCreateDTO.java:15`
- `src/main/java/com/vladko/autoshopcore/order/dto/OrderUpdateDTO.java:13`

Уже реализовано:

- создание заказа/booking;
- проверка согласованности `customer ↔ vehicle`;
- выбор `selectedServiceIds`;
- автосборка service lines;
- валидация availability для сотрудника;
- перевод booking в `WAITING_FOR_VISIT`;
- отмена/переходы статусов.

### Каталог услуг

- `src/main/java/com/vladko/autoshopcore/servicecatalog/controller/ServiceCatalogController.java:17`
- `src/main/java/com/vladko/autoshopcore/servicecatalog/service/ServiceCatalogServiceImpl.java:73`
- `src/main/java/com/vladko/autoshopcore/servicecatalog/dto/ServiceCatalogItemResponseDTO.java:10`

Уже существует активный каталог с ценой, категорией и длительностью.

### Approval flow

- `src/main/java/com/vladko/autoshopcore/order/approval/controller/OrderApprovalController.java:20`
- `src/main/java/com/vladko/autoshopcore/order/approval/service/OrderApprovalServiceImpl.java:128`
- `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java:39`

Это одна из самых готовых customer-фич уже сейчас.

### Security

- `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:47`
- `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java:17`

---

## Related Files

### Vehicles

- `src/main/java/com/vladko/autoshopcore/vehicle/dto/VehicleCreateDTO.java:12`
- `src/main/java/com/vladko/autoshopcore/vehicle/dto/VehicleUpdateDTO.java:11`
- `src/main/java/com/vladko/autoshopcore/vehicle/dto/VehicleResponseDTO.java:10`
- `src/main/java/com/vladko/autoshopcore/vehicle/repository/VehicleRepository.java`

### Orders / booking

- `src/main/java/com/vladko/autoshopcore/order/dto/OrderCreateDTO.java:15`
- `src/main/java/com/vladko/autoshopcore/order/dto/OrderUpdateDTO.java:13`
- `src/main/java/com/vladko/autoshopcore/order/dto/OrderResponseDTO.java:14`
- `src/main/java/com/vladko/autoshopcore/order/dto/OrderServiceLineDTO.java:8`
- `src/main/java/com/vladko/autoshopcore/order/entity/OrderStatus.java:3`
- `src/main/java/com/vladko/autoshopcore/order/repository/OrderRepository.java:41`

### Employee availability

- `src/main/java/com/vladko/autoshopcore/employee/controller/EmployeeController.java:35`
- `src/main/java/com/vladko/autoshopcore/employee/service/EmployeeServiceImpl.java:80`
- `docs/employee-availability-api-contract.md`
- `docs/employee-availability-search-now-plan.md`

### Customer docs / roadmap

- `docs/CRM_CUSTOMER_API_DOCUMENTATION_RU.md:225`
- `docs/CRM_CUSTOMER_API_DOCUMENTATION_RU.md:724`
- `docs/CRM_CUSTOMER_API_DOCUMENTATION_RU.md:1038`
- `docs/CRM_CUSTOMER_FRONTEND_AUTH_UPDATE_RU.md:387`
- `docs/CRM_CUSTOMER_FRONTEND_AUTH_UPDATE_RU.md:413`
- `docs/CRM_CUSTOMER_FRONTEND_AUTH_UPDATE_RU.md:447`
- `docs/CRM_CUSTOMER_FRONTEND_AUTH_UPDATE_RU.md:502`

---

## Usage Patterns

### Как проект устроен сейчас

1. Customer self-service уже вынесен в отдельный namespace `/api/customers/me/*`.
2. Staff CRUD живёт в общих контроллерах `/api/vehicles`, `/api/orders`, `/api/service-catalog`.
3. Внутренние domain-сервисы уже содержат большую часть нужной бизнес-логики.
4. Основной пробел — не отсутствие domain-логики, а отсутствие **customer-safe facade contracts**.

### Как правильно расширять проект дальше

Новые customer-возможности нужно добавлять не через “дать роль CUSTOMER на staff endpoints”, а через:

- отдельные self-service controller routes;
- отдельные customer DTO;
- ownership validation на backend;
- server-side derivation of `customerId`;
- исключение `employeeId` и внутренних полей из customer contracts.

---

## Key Insights

### Insight 1 — core logic уже существует

Проект уже умеет:

- создавать машину;
- редактировать машину;
- удалять машину;
- создавать booking;
- выбирать стандартные услуги;
- собирать service lines;
- валидировать booking availability;
- отменять booking;
- обрабатывать customer approvals.

То есть задача не требует большого rewrite domain-модели.

### Insight 2 — текущие публичные DTO небезопасны для customer API напрямую

Например:

- `OrderCreateDTO` содержит `customerId` и `employeeId`: `src/main/java/com/vladko/autoshopcore/order/dto/OrderCreateDTO.java:18`
- `VehicleCreateDTO` содержит `customerId`: `src/main/java/com/vladko/autoshopcore/vehicle/dto/VehicleCreateDTO.java:14`

Для customer API это неприемлемо.

### Insight 3 — ownership model уже частично есть

Для заказов ownership проверяется через `requireCustomerAccess(order)`:

- `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java:39`

Для машин такого customer-safe guard пока нет, но паттерн уже очевиден.

### Insight 4 — documents/files не стоит смешивать с booking MVP

Docs прямо фиксируют, что customer files/documents контрактов пока нет:

- `docs/CRM_CUSTOMER_API_DOCUMENTATION_RU.md:1041`

Поэтому vehicle CRUD + booking MVP + approvals должны идти отдельно от photo/documents пакета.

---

## Текущее состояние по capability matrix

| Capability | Domain logic | Customer-safe API | Security ownership | Ready for UI |
|---|---:|---:|---:|---:|
| Profile read/update | Да | Да | Да | Да |
| Orders list | Да | Да | Да | Да |
| Order details | Да | Частично | Частично | Ограниченно |
| Approvals list | Да | Да | Да | Да |
| Approve / reject | Да | Да | Да | Да |
| Vehicles list | Да | Да | Да | Да |
| Vehicle details | Да | Нет | Нет | Нет |
| Vehicle create | Да | Нет | Нет | Нет |
| Vehicle update | Да | Нет | Нет | Нет |
| Vehicle delete | Да | Нет | Нет | Нет |
| Active service catalog | Да | Нет | Нет | Нет |
| Booking create | Да | Нет | Нет | Нет |
| Booking reschedule | Да | Нет | Нет | Нет |
| Booking cancel by customer | Частично | Нет | Нет | Нет |
| Booking slot lookup | Частично | Нет | Нет | Нет |
| Documents / photos | Частично infra | Нет | Нет | Нет |

---

## Целевая архитектура

### Принцип

Построить **customer-safe application facade** поверх существующих domain-сервисов.

### Рекомендуемые новые сервисы

- `CustomerVehicleSelfService`
- `CustomerBookingSelfService`
- `CustomerOrderSelfService`
- при необходимости `CustomerServiceCatalogFacade`

### Почему не стоит раздувать `CustomerSelfServiceImpl`

- Сейчас это в первую очередь profile/dashboard/overview слой: `src/main/java/com/vladko/autoshopcore/client/service/CustomerSelfServiceImpl.java:30`
- Если туда сложить vehicles + booking + order details + services + documents, класс быстро станет монолитом.

### Рекомендуемый package layout

Вариант 1:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerVehicleSelfServiceController.java`
- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerBookingSelfServiceController.java`
- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerOrderSelfServiceController.java`
- `src/main/java/com/vladko/autoshopcore/client/service/CustomerVehicleSelfService.java`
- `src/main/java/com/vladko/autoshopcore/client/service/CustomerBookingSelfService.java`
- `src/main/java/com/vladko/autoshopcore/client/service/CustomerOrderSelfService.java`
- `src/main/java/com/vladko/autoshopcore/client/dto/...`

Вариант 2:

- выделить `client/vehicle`, `client/booking`, `client/order` подпакеты.

Для роста проекта предпочтительнее вариант 2.

---

## Целевые customer API contracts

## 1. Vehicles

### 1.1. List vehicles

`GET /api/customers/me/vehicles`

Уже существует:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerSelfServiceController.java:39`

### 1.2. Get vehicle details

`GET /api/customers/me/vehicles/{vehicleId}`

#### Назначение

- отдельный экран автомобиля;
- детальная история по авто;
- подготовка к booking create/edit.

### 1.3. Create vehicle

`POST /api/customers/me/vehicles`

#### Request DTO

`CustomerVehicleCreateDTO`

```json
{
  "brand": "BMW",
  "model": "X5",
  "vin": "WBA12345678901234",
  "licensePlate": "A123AA77"
}
```

#### Важно

- без `customerId`;
- `customerId` определяется из authenticated customer.

### 1.4. Update vehicle

`PUT /api/customers/me/vehicles/{vehicleId}`

#### Request DTO

`CustomerVehicleUpdateDTO`

```json
{
  "brand": "BMW",
  "model": "X5 LCI",
  "licensePlate": "A123AA77"
}
```

### 1.5. Delete vehicle

`DELETE /api/customers/me/vehicles/{vehicleId}`

#### Business rule to define

Нужно заранее зафиксировать правило:

- либо удаление разрешено только если нет активных заказов;
- либо физическое удаление запрещено и делается soft-disable;
- либо удаление возможно только если нет вообще ни одного заказа.

**Рекомендация:**

Для MVP разрешать удаление **только если по машине нет активных заказов в booking/repair flow**.

---

## 2. Service catalog for booking

### 2.1. List customer-available services

`GET /api/customers/me/booking/services`

#### Response

Новый customer-safe DTO:

- `id`
- `name`
- `description`
- `basePrice`
- `categoryId`
- `categoryName`
- `defaultDurationMinutes`

Можно переиспользовать структуру из:

- `src/main/java/com/vladko/autoshopcore/servicecatalog/dto/ServiceCatalogItemResponseDTO.java:10`

но лучше завести отдельный customer response DTO, чтобы не привязать внешний контракт к staff-admin API навсегда.

### 2.2. Optional categories endpoint

`GET /api/customers/me/booking/service-categories`

Если фронту нужен grouped selector.

---

## 3. Booking slot lookup

### 3.1. Search available slots

`GET /api/customers/me/booking/slots?vehicleId=...&serviceIds=1,2,3&dateFrom=...&days=...`

### Почему нужен отдельный endpoint

Сейчас есть staff-oriented availability search:

- `src/main/java/com/vladko/autoshopcore/employee/controller/EmployeeController.java:35`

Но клиенту не нужно видеть список сотрудников.

### Customer response должен возвращать

- дату/время слота;
- длительность;
- признак доступности;
- optional reason if unavailable.

Например:

```json
[
  {
    "startAt": "2026-05-20T09:00:00Z",
    "slotMinutes": 90,
    "available": true
  }
]
```

### Внутренняя логика

1. Проверить ownership машины.
2. Загрузить активные выбранные services.
3. Вычислить рекомендуемую длительность по `defaultDurationMinutes`.
4. Использовать employee availability internals.
5. Вернуть клиенту агрегированные слоты без раскрытия механиков.

### Open decision

Нужно решить отдельно:

- выбирает ли backend “любой доступный слот без привязки к сотруднику”;
- или клиент видит только временные окна;
- или клиент выбирает preferred time, а backend уже потом назначает staff.

**Рекомендация для MVP:**

Клиент выбирает только время. Сотрудник назначается позже staff-side или автоматически по внутренним правилам.

---

## 4. Booking create

### 4.1. Create booking

`POST /api/customers/me/bookings`

### Request DTO

`CustomerBookingCreateDTO`

```json
{
  "vehicleId": 14,
  "plannedVisitAt": "2026-05-20T09:00:00Z",
  "plannedSlotMinutes": 90,
  "selectedServiceIds": [1, 4, 8],
  "problem": "Стук в подвеске на малой скорости",
  "intakeNotes": "Нужна машина вечером",
  "plannedDropOff": true
}
```

### Важно

DTO не должен содержать:

- `customerId`
- `employeeId`
- `bookingChannel`
- staff-only approval/financial поля

### Backend mapping

На backend формируется `OrderCreateDTO`:

- `customerId = currentCustomer.id`
- `vehicleId = request.vehicleId`
- `employeeId = null`
- `bookingChannel = SELF_SERVICE` или новое customer-safe значение
- `selectedServiceIds = request.selectedServiceIds`
- `problem = request.problem`
- `plannedVisitAt = request.plannedVisitAt`
- `plannedSlotMinutes = request.plannedSlotMinutes`
- `intakeNotes = request.intakeNotes`

### Domain reuse points

- `validateCustomerVehicleConsistency(...)`: `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:470`
- `replaceServiceLines(...)`: `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:408`
- `defaultProblem(...)`: `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:397`
- `initialStatus WAITING_FOR_VISIT`: `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:353`

### Архитектурная рекомендация

Не вызывать напрямую staff-public `orderService.create(dto)` из customer controller.

Лучше:

- выделить reusable internal create method в order application layer;
- либо создать `CustomerBookingSelfService`, который вызывает безопасный internal facade.

---

## 5. Booking update / reschedule

### 5.1. Update booking

`PUT /api/customers/me/bookings/{orderId}`

### Разрешённые поля

- `plannedVisitAt`
- `plannedSlotMinutes`
- `selectedServiceIds`
- `problem`
- `intakeNotes`

### DTO

`CustomerBookingUpdateDTO`

### Разрешённые статусы для изменения

Рекомендуемо:

- `WAITING_FOR_VISIT`
- возможно `NEW`

### Запрещённые кейсы

- после check-in;
- после ухода в diagnosis/repair;
- после terminal status;
- если заказ уже waiting for part / approval flow в неподходящем состоянии.

### Reuse points

- order update logic: `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:118`
- planned visit mutability checks: `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:437`

---

## 6. Booking cancel

### 6.1. Cancel booking by customer

`POST /api/customers/me/bookings/{orderId}/cancel`

### Ожидаемое поведение

- только owner заказа;
- только в pre-check-in booking statuses;
- status → `CANCELLED_BY_CUSTOMER`.

### Почему лучше отдельный endpoint

- безопаснее, чем разрешать клиенту менять status через общий update;
- проще держать бизнес-ограничения;
- лучше читается фронтом.

---

## 7. Customer order details

### 7.1. Get one order

`GET /api/customers/me/orders/{orderId}`

### Зачем нужен

Сейчас у клиента есть list, approvals и dashboard. Но для нормального order journey нужен отдельный details endpoint без знания staff routes.

### Внутри

- найти order;
- проверить ownership;
- отдать `OrderResponseDTO`.

### Расширение на будущее

Позже сюда можно добавить aggregate details payload:

- `order`
- `approvals`
- `timelinePreview`
- `documentsSummary`
- `loyaltySummary`

Но для MVP лучше начать с простого order details.

---

## 8. Customer approvals

### Что уже есть

Из security видно, что customer уже может:

- читать approvals;
- approve;
- reject.

См.:

- `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:85`
- `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:87`
- `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:88`

### Рекомендация

Для чистоты self-service API желательно добавить alias routes:

- `GET /api/customers/me/orders/{orderId}/approvals`
- `POST /api/customers/me/orders/{orderId}/approvals/{requestId}/approve`
- `POST /api/customers/me/orders/{orderId}/approvals/{requestId}/reject`

Внутри можно переиспользовать существующий `OrderApprovalService`.

---

## 9. Security plan

## 9.1. Что нельзя делать

Нельзя просто открыть клиенту старые staff endpoints:

- `/api/vehicles/**`
- `/api/orders`
- `/api/service-catalog/**`

Потому что тогда CUSTOMER получит лишнюю власть и сможет влиять на внутренние поля.

## 9.2. Что нужно сделать

### Добавить новые matcher rules

В `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java:60` добавить customer-safe разрешения для:

- `POST /api/customers/me/vehicles`
- `GET /api/customers/me/vehicles/{id}`
- `PUT /api/customers/me/vehicles/{id}`
- `DELETE /api/customers/me/vehicles/{id}`
- `GET /api/customers/me/booking/services`
- `GET /api/customers/me/booking/slots`
- `POST /api/customers/me/bookings`
- `PUT /api/customers/me/bookings/{id}`
- `POST /api/customers/me/bookings/{id}/cancel`
- `GET /api/customers/me/orders/{id}`
- optional approvals aliases.

### Добавить ownership helpers

Рекомендуется добавить в security/application layer:

- `requireCurrentCustomerVehicleAccess(vehicle)`
- `requireCurrentCustomerVehicleAccess(vehicleId)`
- `requireCurrentCustomerBookingEditable(order)`
- `requireCurrentCustomerBookingCancelable(order)`

### Где размещать ownership checks

Рекомендация:

- базовый `currentCustomer()` resolution оставить в self-service application services;
- order ownership reuse можно делать через `CoreSecurityService.requireCustomerAccess(order)`;
- vehicle ownership лучше вынести в отдельный helper/service.

---

## 10. DTO plan

## 10.1. New request DTOs

Создать:

- `CustomerVehicleCreateDTO`
- `CustomerVehicleUpdateDTO`
- `CustomerBookingCreateDTO`
- `CustomerBookingUpdateDTO`
- `CustomerBookingCancelDTO` (если нужен comment/reason)
- `CustomerBookingSlotsQueryDTO` или query params parser DTO

## 10.2. New response DTOs

Создать:

- `CustomerVehicleDetailsDTO` либо переиспользовать `VehicleResponseDTO`
- `CustomerBookingSlotResponseDTO`
- `CustomerBookingServiceCatalogItemDTO`
- optional `CustomerOrderDetailsDTO`

## 10.3. DTO rule

Для customer API DTO должны быть **narrower than staff DTO**.

То есть внешние customer contracts не должны напрямую зависеть от staff admin contracts, если там есть риск утечки внутренней модели.

---

## 11. Service-by-service implementation plan

## Phase 1 — Vehicle self-service CRUD

### Scope

- `POST /api/customers/me/vehicles`
- `GET /api/customers/me/vehicles/{vehicleId}`
- `PUT /api/customers/me/vehicles/{vehicleId}`
- `DELETE /api/customers/me/vehicles/{vehicleId}`

### Backend tasks

1. Добавить controller.
2. Добавить service facade.
3. Добавить ownership validation.
4. Смаппить customer DTO → existing `VehicleCreateDTO` / `VehicleUpdateDTO`.
5. Добавить security rules.
6. Написать integration tests.

### Acceptance criteria

- клиент может создать свою машину;
- клиент видит только свои машины;
- клиент не может изменить/удалить чужую машину;
- catalog-link endpoints клиенту недоступны.

---

## Phase 2 — Customer booking service catalog

### Scope

- `GET /api/customers/me/booking/services`
- optional `GET /api/customers/me/booking/service-categories`

### Backend tasks

1. Добавить customer-safe catalog controller.
2. Использовать `ServiceCatalogService.getServices(activeOnly=true, ...)`.
3. Отдавать только active services.
4. Не открывать admin CRUD.
5. Добавить tests.

### Acceptance criteria

- клиент видит только активные услуги;
- фронт может построить picker услуг;
- API не раскрывает admin write surface.

---

## Phase 3 — Booking create MVP

### Scope

- `POST /api/customers/me/bookings`

### Backend tasks

1. Добавить `CustomerBookingCreateDTO`.
2. Реализовать mapping в internal order create command.
3. Подставлять `customerId` из auth context.
4. Всегда игнорировать employee assignment.
5. Проверять ownership vehicle.
6. Проверять active service IDs.
7. Возвращать `OrderResponseDTO` или customer-specific booking response.
8. Добавить tests.

### Acceptance criteria

- клиент может создать booking на свою машину;
- клиент не может создать booking на чужую машину;
- клиент может выбрать стандартные услуги;
- order создаётся в корректном customer booking status.

---

## Phase 4 — Booking reschedule / edit

### Scope

- `PUT /api/customers/me/bookings/{orderId}`

### Backend tasks

1. Ограничить editable statuses.
2. Разрешить update только owner’у.
3. Поддержать reschedule и update selected services.
4. Повторно валидировать booking slot.
5. Добавить tests.

### Acceptance criteria

- клиент может перенести запись до check-in;
- клиент не может редактировать заказ после перехода в ремонтный workflow;
- services/problem/intakeNotes корректно обновляются.

---

## Phase 5 — Booking cancel

### Scope

- `POST /api/customers/me/bookings/{orderId}/cancel`

### Backend tasks

1. Явный cancel endpoint.
2. Ownership check.
3. Status guard.
4. Transition to `CANCELLED_BY_CUSTOMER`.
5. Timeline / notifications if required.
6. Tests.

### Acceptance criteria

- клиент может отменить только свой booking;
- нельзя отменить после check-in;
- статус выставляется корректно.

---

## Phase 6 — Customer order details

### Scope

- `GET /api/customers/me/orders/{orderId}`

### Backend tasks

1. Добавить details endpoint.
2. Reuse existing order mapping.
3. Ownership check.
4. Optional future extension path for aggregate details.

### Acceptance criteria

- клиент может открыть детальную карточку только своего заказа.

---

## Phase 7 — Slot lookup refinement

### Scope

- `GET /api/customers/me/booking/slots`

### Backend tasks

1. Определить duration strategy по selected services.
2. Спрятать employee-level details.
3. Стабилизировать response contract для фронта.
4. Добавить tests на availability edge cases.

### Acceptance criteria

- фронт может строить календарь/слоты без знания о механиках.

---

## Phase 8 — Documents/files later

### Scope

Не включать в booking MVP.

### Почему

Docs фиксируют отдельный gap:

- `docs/CRM_CUSTOMER_API_DOCUMENTATION_RU.md:1041`

### Что делать потом

Отдельным пакетом:

- vehicle photos/documents;
- order attachments;
- customer-safe upload/list/delete permissions.

---

## 12. Изменения по файлам

## Новые контроллеры

Рекомендуемые файлы:

- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerVehicleSelfServiceController.java`
- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerBookingSelfServiceController.java`
- `src/main/java/com/vladko/autoshopcore/client/controller/CustomerOrderSelfServiceController.java`
- optional `src/main/java/com/vladko/autoshopcore/client/controller/CustomerBookingCatalogController.java`

## Новые сервисы

- `src/main/java/com/vladko/autoshopcore/client/service/CustomerVehicleSelfService.java`
- `src/main/java/com/vladko/autoshopcore/client/service/CustomerBookingSelfService.java`
- `src/main/java/com/vladko/autoshopcore/client/service/CustomerOrderSelfService.java`

## Новые DTO

- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerVehicleCreateDTO.java`
- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerVehicleUpdateDTO.java`
- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingCreateDTO.java`
- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingUpdateDTO.java`
- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingSlotResponseDTO.java`
- `src/main/java/com/vladko/autoshopcore/client/dto/CustomerBookingServiceCatalogItemDTO.java`

## Изменяемые существующие файлы

- `src/main/java/com/vladko/autoshopcore/configuration/SecurityConfiguration.java`
- `src/main/java/com/vladko/autoshopcore/security/CoreSecurityService.java`
- `src/main/java/com/vladko/autoshopcore/security/CoreSecurityServiceImpl.java`
- возможно `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java`
- возможно `src/main/java/com/vladko/autoshopcore/vehicle/service/VehicleServiceImpl.java`

---

## 13. Тестовый план

## Unit tests

Покрыть:

- current customer → vehicle create mapping;
- current customer → booking create mapping;
- ownership validation for vehicles;
- forbidden field injection resistance;
- booking status guards.

## Integration tests

Сценарии:

1. customer creates own vehicle;
2. customer cannot update чужую машину;
3. customer cannot delete чужую машину;
4. customer gets own vehicle details;
5. customer gets only active booking services;
6. customer creates booking for own vehicle;
7. customer cannot create booking for чужую vehicle;
8. customer reschedules own booking in allowed status;
9. customer cannot reschedule after check-in;
10. customer cancels own booking;
11. customer cannot cancel чужой booking.

## Security tests

Проверить:

- CUSTOMER не может `POST /api/vehicles`
- CUSTOMER может `POST /api/customers/me/vehicles`
- CUSTOMER не может подменить `customerId`
- CUSTOMER не может назначить `employeeId`
- CUSTOMER не может идти в admin catalog write routes

---

## 14. Порядок реализации

### Recommended order

1. Vehicle self-service CRUD
2. Customer booking catalog read
3. Booking create MVP
4. Booking reschedule
5. Booking cancel
6. Customer order details
7. Slot lookup refinement
8. Documents/files package later

### Почему такой порядок

- booking без управления машинами неудобен;
- booking без service catalog не имеет UX-смысла;
- details endpoint нужен после появления новых customer-generated bookings;
- documents/photos не должны тормозить запуск MVP.

---

## 15. MVP acceptance definition

Self-service MVP считается готовым, когда клиент может:

1. добавить свою машину;
2. отредактировать свою машину;
3. удалить свою машину при соблюдении бизнес-ограничений;
4. выбрать машину из списка;
5. увидеть список доступных услуг;
6. создать запись на приём;
7. перенести запись;
8. отменить запись;
9. открыть детали своего заказа;
10. approve/reject согласование по заказу.

При этом клиент не должен иметь возможности:

- передавать `customerId`;
- выбирать или менять `employeeId`;
- менять estimate/parts/internal statuses;
- модифицировать чужие машины и чужие заказы.

---

## 16. Final recommendation

Лучший путь реализации — это **self-service facade architecture**:

- reuse существующих domain-сервисов;
- не открывать старые staff endpoints;
- строить новые customer-safe routes на `/api/customers/me/*`;
- держать ownership и mutability rules на backend;
- выпускать MVP в 2 волны:
  - Wave 1: vehicles + catalog + booking create/cancel/update;
  - Wave 2: slot refinement + documents/photos.

Это даст клиенту реальные сценарии self-service без риска сломать внутреннюю CRM-модель.
