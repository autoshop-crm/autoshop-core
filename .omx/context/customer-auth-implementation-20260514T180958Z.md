task statement: Реализовать customer auth / self-service foundation по плану CRM_CUSTOMER_AUTH_IMPLEMENTATION_PLAN_RU.md внутри текущего репо
desired outcome: working backend foundation with customer linkage, self-service endpoints, auth facade contracts where possible, tests green
known facts/evidence:
- внешний auth-service уже используется только для validateAccessToken
- customer access сейчас завязан на local customer id
- customer/profile/vehicle/loyalty public API для customer отсутствуют
- полный auth-service код в текущем репо отсутствует
constraints:
- работать аккуратно и совместимо с текущим проектом
- не ломать staff flows
- реализовать только то, что возможно внутри данного репо без придумывания внешних сервисов
unknowns/open questions:
- доступны ли login/register endpoints у внешнего auth-service
- какой точный контракт refresh/logout/recovery у auth-service
likely codebase touchpoints:
- src/main/java/com/vladko/autoshopcore/security
- src/main/java/com/vladko/autoshopcore/client
- src/main/java/com/vladko/autoshopcore/order
- src/main/resources/db/changelog
- src/test/java/com/vladko/autoshopcore
