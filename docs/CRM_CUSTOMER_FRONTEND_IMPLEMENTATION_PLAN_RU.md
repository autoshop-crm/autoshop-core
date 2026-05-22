# CRM Customer Frontend — подробный технический план реализации

Дата: 2026-05-14  
Статус: draft / implementation-ready  
Проект: `FrontClient`  
Основа: концепт клиентской части и уже реализованный staff/frontend reference `../autoshop-web-spec`

---

## 1. Цель документа

Этот документ фиксирует **подробный phased implementation plan** для разработки клиентской части CRM с нуля в отдельном проекте `FrontClient`.

Задача плана:

- разложить разработку на понятные фазы;
- определить архитектурный фундамент проекта;
- зафиксировать, что можно переиспользовать из staff reference;
- определить приоритеты экранов и фич;
- снизить риск того, что клиентский кабинет превратится в копию staff CRM.

---

## 2. Базовая стратегия

Клиентский фронт нужно строить **как отдельный проект**, но не как отдельную доменную систему.

Это означает:

- UI/UX, routing tree, layout и presentation layer у клиента будут отдельными;
- source-of-truth по домену остаётся тем же, что и в staff/reference проекте;
- API-клиенты, часть типов, часть мапперов, theme foundation и базовые UI patterns можно переиспользовать;
- основная новая работа — это **client-safe presentation layer** и **mobile-first product shell**.

### Главная продуктовая идея

Клиентский кабинет — это не внутренний CRM-интерфейс, а:

- прозрачный сервисный кабинет владельца автомобиля;
- место, где клиент быстро понимает:
  - что происходит с машиной;
  - что делать дальше;
  - сколько это стоит;
  - когда будет готово;
  - где нужны его действия.

---

## 3. Что берём из reference-проекта

Reference: `../autoshop-web-spec`

### 3.1. Что можно переиспользовать почти напрямую

1. Общий стек:
   - React
   - TypeScript
   - Vite
   - MUI
   - axios
   - react-router-dom

2. Архитектурные паттерны:
   - auth bootstrap pattern
   - API client pattern
   - error/loading handling patterns
   - theme setup

3. Доменную основу:
   - orders
   - vehicles
   - customers
   - approvals
   - files
   - loyalty

4. UI building blocks:
   - `EmptyState`
   - `AppAlert`
   - `SectionCard`
   - `StatusChip`
   - `DetailGrid` как вспомогательный паттерн, но не как основной способ рендера клиентских критичных экранов

### 3.2. Что нельзя переносить как есть

Нельзя просто копировать:

- staff routing;
- staff dashboard;
- staff sidebar;
- order details как workspace для сотрудников;
- role-matrix oriented access patterns в UI;
- internal operational actions.

### 3.3. Что нужно адаптировать

Нужно адаптировать:

- статусы заказа;
- структуру order details;
- approvals UX;
- navigation model;
- information hierarchy;
- тексты кнопок, карточек и статусных блоков.

---

## 4. Подтверждённые опоры из reference

Ниже — ключевые места reference-проекта, на которые стоит опираться при реализации.

### Routing и auth bootstrap

- `../autoshop-web-spec/src/app/App.tsx:72`
- `../autoshop-web-spec/src/app/App.tsx:38`
- `../autoshop-web-spec/src/auth/storage.ts:1`
- `../autoshop-web-spec/src/api/authApi.ts:1`

### Layout и навигация staff-контура

- `../autoshop-web-spec/src/layouts/AppLayout.tsx:34`

Это reference для разделения логики, но не для прямого копирования UX.

### Theme и base UI

- `../autoshop-web-spec/src/styles/theme.ts:1`
- `../autoshop-web-spec/src/components/EmptyState.tsx:1`
- `../autoshop-web-spec/src/components/AppAlert.tsx:1`
- `../autoshop-web-spec/src/components/SectionCard.tsx:1`
- `../autoshop-web-spec/src/components/StatusChip.tsx:1`
- `../autoshop-web-spec/src/components/DetailGrid.tsx:1`

### Orders / approvals / files / loyalty API

- `../autoshop-web-spec/src/api/ordersApi.ts:1`
- `../autoshop-web-spec/src/api/orderApprovalApi.ts:4`
- `../autoshop-web-spec/src/api/filesApi.ts:6`
- `../autoshop-web-spec/src/api/loyaltyApi.ts:4`
- `../autoshop-web-spec/src/api/vehiclesApi.ts:30`

### Staff order detail как anti-reference

- `../autoshop-web-spec/src/pages/orders/OrderDetailsPage.tsx:1`
- `../autoshop-web-spec/src/pages/orders/components/OrderDetailsView.tsx:1`
- `../autoshop-web-spec/src/pages/orders/components/ApprovalsSection.tsx:1`
- `../autoshop-web-spec/src/pages/orders/components/TimelineSection.tsx:1`
- `../autoshop-web-spec/src/domain/crm/orderDetailPolicy.ts:55`

Это полезно как источник доменной структуры, но клиентский UI должен быть переосмыслен.

---

## 5. Целевая архитектура проекта `FrontClient`

Рекомендуемая структура:

```text
FrontClient/
  src/
    app/
      providers/
      router/
      store/
    layouts/
      client/
    pages/
      auth/
      dashboard/
      vehicles/
      orders/
      approvals/
      loyalty/
      profile/
      booking/
      documents/
    features/
      client-auth/
      client-dashboard/
      client-orders/
      client-vehicles/
      client-approvals/
      client-files/
      client-loyalty/
      client-booking/
    domain/
      client/
        mappers/
        policies/
        dictionaries/
        view-models/
    api/
    components/
    hooks/
    utils/
    styles/
    types/
```

### Архитектурные правила

1. `api/` отвечает только за HTTP и транспортные DTO.
2. `domain/client/` отвечает за client-safe mapping layer.
3. `features/` собирают поведение конкретной бизнес-области.
4. `pages/` собирают экраны из feature-блоков.
5. `layouts/client/` отвечает за shell и responsive navigation.
6. UI не должен зависеть напрямую от raw backend semantics там, где нужна клиентская интерпретация.

---

## 6. Ключевые технические решения

## 6.1. Разделение слоёв

Нужно явно разделить:

- **transport layer** — сырые ответы backend;
- **domain layer** — промежуточные модели и маппинг;
- **presentation layer** — client-safe view models;
- **UI layer** — страницы, блоки, CTA, карточки.

## 6.2. Client-safe mapping layer

Это обязательный слой.

Нужны как минимум:

- `ClientOrderViewModel`
- `ClientApprovalViewModel`
- `ClientVehicleViewModel`
- `ClientDashboardViewModel`
- `ClientLoyaltyViewModel`

### Пример ответственности `ClientOrderViewModel`

Должен собирать:

- `title`
- `statusLabel`
- `statusDescription`
- `nextAction`
- `scheduledAt`
- `vehicleSummary`
- `priceSummary`
- `approvalState`
- `timelinePreview`
- `documentsSummary`
- `isAttentionRequired`

## 6.3. Status dictionary

Нужно создать отдельный словарь:

- `backendStatus -> client label`
- `backendStatus -> client description`
- `backendStatus -> CTA`
- `backendStatus -> visual severity`

Пример:

- не `WAITING_FOR_OWNER_APPROVAL`
- а `Нужно ваше подтверждение дополнительных работ`

## 6.4. Navigation model

### Desktop

Лёгкая навигация:

- Главная
- Мои автомобили
- Мои заказы
- Согласования
- Документы
- Лояльность
- Профиль

### Mobile

Bottom navigation:

- Главная
- Заказы
- Авто
- Согласования
- Профиль

## 6.5. UI principles

Обязательные принципы:

1. mobile-first;
2. trust before density;
3. important-first hierarchy;
4. no backend language;
5. one-screen clarity;
6. sticky primary actions на мобильных критичных экранах.

---

## 7. Фазы разработки

## Phase 0 — Discovery и foundation

### Цель

Подготовить проектную основу до начала активной экранной разработки.

### Задачи

1. Создать новый проектный каркас `FrontClient`.
2. Определить package stack:
   - `react`
   - `typescript`
   - `vite`
   - `@mui/material`
   - `@emotion/react`
   - `@emotion/styled`
   - `axios`
   - `react-router-dom`
   - `zod`
3. Зафиксировать route map клиентского приложения.
4. Зафиксировать screen map MVP.
5. Зафиксировать navigation contract desktop/mobile.
6. Составить список backend endpoints, которые уже можно использовать.
7. Выявить missing backend contracts для booking, notifications, loyalty history, client registration/recovery.
8. Подготовить базовый copy deck для статусов и CTA.
9. Зафиксировать `ClientOrderViewModel` и related view-model contracts.

### Артефакты фазы

- `README.md`
- `src/app/router/routeMap.ts`
- `src/domain/client/view-models/*`
- `src/domain/client/dictionaries/clientOrderStatusDictionary.ts`
- `docs/api-gap-analysis.md`

### Definition of Done

- понятен список экранов MVP;
- понятны backend зависимости;
- утверждён client-safe mapping contract;
- утверждена навигация.

---

## Phase 1 — App shell, providers и identity

### Цель

Собрать базовую рабочую оболочку приложения.

### Задачи

1. Настроить `ThemeProvider`.
2. Перенести и адаптировать theme foundation из reference.
3. Настроить `BrowserRouter`.
4. Создать `ClientLayout`.
5. Создать `ClientTopBar`.
6. Создать `ClientBottomNavigation`.
7. Реализовать базовые app routes.
8. Реализовать auth bootstrap.
9. Реализовать `PublicRoute` / `ProtectedRoute`.
10. Реализовать базовый login screen.
11. Подготовить заглушки для register/recovery, даже если backend ещё не готов.
12. Реализовать logout flow.

### Экраны фазы

- Login
- App shell
- Placeholder pages для основных разделов

### Technical notes

- auth bootstrap можно строить по паттерну из `../autoshop-web-spec/src/app/App.tsx:38`
- storage pattern можно адаптировать из `../autoshop-web-spec/src/auth/storage.ts:1`
- UX нужно делать отдельным от staff login

### Definition of Done

- клиент может войти в приложение;
- клиент попадает в client shell;
- клиентский layout работает на desktop и mobile;
- основные routes существуют и защищены guard-ами.

---

## Phase 2 — Client domain mapping layer

### Цель

Изолировать клиентский UI от сырого staff/backend языка.

### Задачи

1. Создать client dictionaries:
   - order statuses
   - approval statuses
   - next actions
   - severity mapping
2. Реализовать `mapOrderToClientViewModel()`.
3. Реализовать `mapApprovalToClientViewModel()`.
4. Реализовать `mapVehicleToClientViewModel()`.
5. Реализовать `mapLoyaltyToClientViewModel()`.
6. Реализовать formatters:
   - money
   - date/time
   - timeline labels
   - progress labels
7. Подготовить fake fixtures и mock adapters для экранной сборки.

### Почему эта фаза критична

Без неё разработка экранов быстро превратится в рендер raw DTO и копирование staff semantics.

### Definition of Done

- UI экраны могут рендериться через client-safe модели;
- внутренние enum-значения не торчат в интерфейсе;
- статусы и CTA централизованы.

---

## Phase 3 — Dashboard MVP

### Цель

Сделать персональный главный экран клиента.

### Что должен показывать dashboard

1. Активный заказ.
2. Ближайшую запись.
3. Pending approval.
4. Краткий статус по автомобилям.
5. Быстрые действия:
   - открыть заказ;
   - открыть согласование;
   - записаться на сервис;
   - открыть мои автомобили.

### Задачи

1. Создать `ClientDashboardPage`.
2. Сделать hero/state block текущего активного кейса.
3. Сделать карточку `Нужно ваше действие`.
4. Сделать список quick actions.
5. Сделать пустые состояния:
   - нет машин;
   - нет активных заказов;
   - нет согласований.
6. Добавить loading/error UX.

### Definition of Done

- клиент открывает приложение и сразу понимает, что происходит сейчас;
- dashboard не выглядит как operational staff board.

---

## Phase 4 — Orders MVP

### Цель

Сделать клиентский контур заказов.

### Подфаза 4.1 — Orders list

#### Задачи

1. Реализовать `My Orders` page.
2. Добавить active/archive segmentation.
3. Добавить order cards вместо heavy tables.
4. Добавить фильтрацию минимум по:
   - активные;
   - завершённые;
   - требуют действия.
5. Добавить CTA перехода в details.

#### Definition of Done

- клиент видит свои заказы и быстро различает активные и архивные.

### Подфаза 4.2 — Client Order Journey Page

#### Это главный экран проекта

Начинать продуктовую ценность нужно именно с него.

#### Экран должен отвечать на 4 вопроса

1. Что происходит?
2. Что мне делать?
3. Сколько это стоит?
4. Когда будет готово?

#### Блоки экрана

1. Order hero
   - номер заказа
   - машина
   - статус
   - краткое описание
   - next step

2. Progress block
   - где сейчас находится заказ
   - какой следующий шаг
   - есть ли задержка / ожидание клиента

3. Price summary
   - предварительная оценка
   - текущий итог
   - скидка
   - баллы / выгода

4. Approvals preview
   - есть ли pending approvals
   - CTA открыть согласование

5. Files/documents preview
   - фото
   - документы
   - сметы
   - акты

6. Timeline preview
   - последние важные события

7. Help / support block
   - что делать, если есть вопрос

#### Что нельзя показывать

- внутренние employee assignments;
- raw ids сотрудников;
- внутренние причины transitions;
- закупочные подробности;
- warehouse/internal operational details.

#### Definition of Done

- этот экран сам по себе уже снижает необходимость звонка в сервис;
- pending approval визуально приоритетнее второстепенных блоков;
- UI mobile-first и понятен без знания backend терминов.

---

## Phase 5 — Approvals MVP

### Цель

Сделать самый денежный и критичный пользовательский сценарий.

### Подфаза 5.1 — Approvals inbox

#### Задачи

1. Создать список открытых согласований.
2. Создать список исторических решений.
3. Приоритизировать pending approvals над resolved.
4. Добавить заметные CTA.

### Подфаза 5.2 — Approval decision page

#### На экране должно быть видно

- что нашли;
- зачем рекомендуем;
- сколько стоят работы;
- сколько стоят детали;
- итоговое изменение суммы;
- влияет ли это на срок;
- что произойдёт после решения;
- approve / reject;
- комментарий клиента.

#### Задачи

1. Реализовать decision screen.
2. Сделать sticky action bar на mobile.
3. Сделать optimistic UX после решения.
4. Сделать error recovery.
5. Сделать success state.
6. Сделать monetary delta максимально заметным.

### Technical basis

Основываться на:

- `../autoshop-web-spec/src/api/orderApprovalApi.ts:4`

### Definition of Done

- клиент может быстро принять решение с телефона;
- денежный эффект и влияние на заказ понятны без расшифровки.

---

## Phase 6 — Vehicles MVP

### Цель

Дать клиенту понятный доступ к его автомобилям.

### Экраны

1. My Vehicles list
2. Vehicle Details

### Задачи

1. Реализовать список автомобилей.
2. Реализовать карточку автомобиля.
3. Показать:
   - марку
   - модель
   - VIN
   - номер
   - документы/фото
   - историю заказов по авто
4. Добавить CTA `Записать этот автомобиль`.
5. Подготовить поддержку нескольких автомобилей у одного аккаунта.

### Technical basis

- `../autoshop-web-spec/src/api/vehiclesApi.ts:30`

### Definition of Done

- клиент может быстро переключаться между своими авто и их историей.

---

## Phase 7 — Files и documents

### Цель

Усилить доверие через прозрачные материалы по заказу и автомобилю.

### Задачи

1. Реализовать files/documents section в заказе.
2. Реализовать files/documents section в авто.
3. Сделать preview grouping:
   - фото
   - документы
   - акты
   - сметы
4. Реализовать download/open flow.
5. Добавить клиентскую загрузку pre-visit файлов.
6. Добавить ограничения и UX для upload:
   - размер;
   - тип;
   - progress;
   - retry.

### Technical basis

- `../autoshop-web-spec/src/api/filesApi.ts:6`

### Definition of Done

- клиент может посмотреть и скачать документы;
- клиент может загрузить нужные материалы до визита.

---

## Phase 8 — Loyalty

### Цель

Показать лояльность как пользовательскую выгоду, а не бухгалтерский модуль.

### Задачи

1. Реализовать loyalty overview page.
2. Показать текущий баланс.
3. Показать выгоду по текущему заказу.
4. Показать списания/начисления по возможности backend.
5. Показать связь `заказ -> выгода клиента`.
6. Добавить explanatory copy.

### Technical basis

- `../autoshop-web-spec/src/api/loyaltyApi.ts:4`

### Definition of Done

- клиент понимает, сколько у него бонусов и как они влияют на стоимость обслуживания.

---

## Phase 9 — Booking MVP

### Цель

Дать клиенту возможность создать новую запись без звонка.

### User flow

1. Вход / регистрация
2. Выбор автомобиля
3. Описание проблемы
4. Выбор даты и времени
5. Добавление файлов
6. Подтверждение записи
7. Экран успеха с номером заявки

### Задачи

1. Реализовать мастер записи.
2. Сделать пошаговый flow.
3. Добавить slot-based time picker.
4. Поддержать привязку к существующему авто.
5. Поддержать добавление нового авто, если это входит в scope.
6. Поддержать описание проблемы.
7. Поддержать прикрепление фото/документов.
8. Реализовать success screen.
9. Подготовить основу для reschedule/cancel, если backend допускает.

### Риски

- booking нельзя начинать до стабилизации shell и order journey;
- без согласованного backend contract есть риск сделать UI в отрыве от реальных payload'ов.

### Definition of Done

- новый или действующий клиент может оформить запись без звонка и без лишних полей.

---

## Phase 10 — Profile, account, settings

### Цель

Закрыть базовые пользовательские account-сценарии.

### Задачи

1. Профиль клиента.
2. Контакты.
3. Подтверждённые каналы связи.
4. Смена пароля.
5. Восстановление доступа.
6. Настройки уведомлений, если backend есть.

### Definition of Done

- клиент может управлять своим аккаунтом без обращения в поддержку.

---

## Phase 11 — Notifications и service maturity

### Цель

Усилить сервисную зрелость продукта.

### Возможные задачи

1. Notifications center.
2. Event cards на dashboard.
3. Напоминания о записи.
4. Напоминания о сезонных работах.
5. Recommendations / repeat visit CTA.
6. Reschedule / cancel booking.

### Definition of Done

- приложение не только показывает текущее состояние, но и возвращает клиента обратно в сервисный цикл.

---

## 8. Рекомендуемый порядок спринтов

### Sprint 1

- Phase 0
- Phase 1

### Sprint 2

- Phase 2
- Dashboard MVP

### Sprint 3

- Orders list
- Client Order Journey

### Sprint 4

- Approvals inbox
- Approval decision page

### Sprint 5

- Vehicles
- Files
- Loyalty

### Sprint 6

- Booking MVP

### Sprint 7+

- Profile maturity
- Notifications
- Retention features

---

## 9. Приоритеты разработки

### P0 — начинать отсюда

1. App shell
2. Auth
3. Client-safe mapping layer
4. Dashboard
5. Client Order Journey
6. Approvals

### P1

1. Orders list
2. Vehicles
3. Files
4. Loyalty

### P2

1. Booking
2. Profile/settings

### P3

1. Notifications
2. Recommendations
3. Retention layer
4. Multi-car family scenarios

---

## 10. Главные продуктовые и технические риски

## Риск 1 — скопировать staff CRM

Это главная ошибка.

Признаки проблемы:

- слишком много таблиц;
- operational language в UI;
- внутренние статусы наружу;
- employee/workspace actions на клиентских страницах.

## Риск 2 — не сделать mapping layer

Если не создать presentation layer, raw backend semantics быстро проникнут в каждый экран.

## Риск 3 — недооценить approval UX

Approval flow — самый денежный экран всего клиентского проекта.

## Риск 4 — сделать desktop-first

Клиентский кабинет почти наверняка будет mobile-first по реальному использованию.

## Риск 5 — слишком рано уйти в booking

Booking важен, но сначала нужно построить shell, order journey и approvals.

---

## 11. Definition of Success для MVP

MVP можно считать успешным, если клиент без звонка способен:

1. войти в кабинет;
2. увидеть свой активный заказ;
3. понять текущий статус простым языком;
4. увидеть следующий шаг;
5. открыть и решить согласование;
6. увидеть стоимость и документы;
7. открыть свои автомобили и историю заказов.

Если эти 7 пунктов работают стабильно, MVP уже даёт реальную сервисную ценность.

---

## 12. Практический стартовый backlog

### Блок A — setup

1. Создать `FrontClient` app scaffold.
2. Подключить MUI, router, axios.
3. Настроить theme.
4. Настроить app providers.
5. Настроить route tree.

### Блок B — auth

6. Реализовать auth storage.
7. Реализовать auth API client.
8. Реализовать bootstrap current user.
9. Реализовать login page.
10. Реализовать protected routes.

### Блок C — domain client layer

11. Описать `ClientOrderViewModel`.
12. Описать `ClientApprovalViewModel`.
13. Описать `ClientVehicleViewModel`.
14. Сделать status dictionary.
15. Сделать order mapper.
16. Сделать approval mapper.
17. Сделать vehicle mapper.

### Блок D — MVP screens

18. Dashboard.
19. Orders list.
20. Order journey page.
21. Approvals inbox.
22. Approval decision page.
23. Vehicles list.
24. Vehicle details.

### Блок E — supporting capabilities

25. Files UI.
26. Loyalty UI.
27. Profile UI.
28. Booking flow MVP.

### Блок F — quality

29. Empty states.
30. Loading states.
31. Error states.
32. Mobile QA.
33. Проверка на реальных backend payload'ах.
34. Copy review клиентских статусов и CTA.

---

## 13. Что делать первым делом после этого документа

Рекомендуемый ближайший порядок:

1. Создать `FrontClient` scaffold.
2. Поднять app shell.
3. Поднять auth flow.
4. Сделать `ClientOrderViewModel` и status dictionary.
5. Сразу после этого реализовать `Client Order Journey Page` как главный экран проекта.
6. Затем — dashboard и approvals.

---

## 14. Итоговая рекомендация

Если выбирать один главный архитектурный принцип для всего проекта, он такой:

> Клиентский фронт должен переиспользовать доменную зрелость staff/reference проекта, но никогда не копировать его UI-семантику напрямую.

Если выбирать один главный экран для старта, это:

- `Client Order Journey Page`

Именно он соединяет:

- статус;
- деньги;
- доверие;
- файлы;
- согласования;
- следующие действия;
- удержание клиента.

