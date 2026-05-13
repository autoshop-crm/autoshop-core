# Deep Interview Spec — CRM Core Full CRM

## Metadata
- Profile: standard
- Rounds: 7
- Final ambiguity: 0.16
- Threshold: 0.20
- Context type: brownfield
- Context snapshot: `.omx/context/crm-core-full-crm-20260513T144204Z.md`

## Clarity Breakdown
| Dimension | Score | Notes |
|---|---:|---|
| Intent | 0.88 | Full CRM lifecycle in Core is explicit |
| Outcome | 0.93 | Clear target flow from booking to handover |
| Scope | 0.90 | CRM core bounded; several backoffice extras excluded |
| Constraints | 0.70 | Brownfield, order-centric, one-order-one-invoice, role-aware |
| Success | 0.88 | End-to-end scenarios are concrete |
| Context | 0.86 | Current Core limitations are evidence-backed |

## Intent
Transform `autoshop-core` from a repair-order backend into a CRM-capable operational backend where booking, service intake, execution, approvals, procurement waits, readiness, handover, and order history are all represented coherently in one domain flow.

## Desired Outcome
- Customer can create an order online or via receptionist/manager.
- Order exists immediately at booking time.
- Order captures booked slot, customer, vehicle, requested standard services, optional problem description.
- Reception can plan and view expected arrivals.
- Mechanic can execute work and request approvals.
- Customer can approve or reject extra work and parts through the order view.
- Manager can see orders waiting for parts and procure missing items.
- Admin can configure standard inspection/service items and loyalty behavior, including disabling loyalty.
- Customer and staff have order history by customer and vehicle.

## In Scope
- Order-as-booking domain model
- CRM-aware order statuses and transitions
- Booking datetime and slot planning
- Vehicle check-in / handover timestamps
- Customer approval loop for extra works
- Customer approval loop for non-stock parts
- Waiting-for-parts operational flow
- Role-aware views and permissions for receptionist, manager, mechanic, admin, customer-facing consumers
- Admin-managed standard services / inspection catalog
- Loyalty enable/disable and rules configuration sufficient for operational use
- Order history and search by customer / vehicle / status
- Customer-visible order timeline/status

## Out of Scope / Non-goals
- Deep third-party integrations
- Accounting exports and document pipelines
- Advanced BI / marketing analytics
- Omnichannel communications center
- Sophisticated warehouse automation
- Sophisticated procurement orchestration beyond current workflow needs

## Decision Boundaries
- OMX may model new supporting entities, but `Order` remains the main lifecycle aggregate.
- One order equals one invoice.
- Additional discovered work stays in the same order.
- No-show ends with cancelled, not completed.
- Extra parts always require customer approval before procurement.
- Standard services are configurable by admin.
- Loyalty must be configurable and may be disabled entirely.

## Constraints
- Brownfield Spring backend in `autoshop-core`
- Current order flow is minimal and repair-centric
- Existing procurement and loyalty modules should be reused where possible
- Current security model is employee-role based and will need extension/refinement
- No direct implementation in this artifact; this is a source-of-truth requirements handoff

## Testable Acceptance Criteria
1. Creating an order at booking time stores customer, vehicle, booked slot, requested service(s), and initial issue text.
2. Reception can see a calendar/list of expected arrivals and assign or plan a mechanic.
3. Vehicle check-in records factual intake time and transitions the order from planned state to accepted/in-progress states.
4. Mechanic can add extra works to the same order and trigger customer approval.
5. Customer can approve or reject extra works; the order timeline and allowed next actions reflect the decision.
6. Mechanic can request a new part; if it is not in stock, customer approval is required before manager procurement.
7. Manager can filter orders waiting for parts and continue the flow after stock receipt.
8. No-show automatically releases the slot and moves the order to cancelled state visible to the customer.
9. Completed repair moves to ready-for-owner, then handover records key-return timestamp and closes the order.
10. Order history is available by customer and by vehicle.
11. Loyalty rules can be configured or disabled by admin without breaking the order flow.
12. When loyalty is disabled, loyalty-specific actions are suppressed while order completion still works.

## Assumptions Exposed + Resolutions
- Assumption: booking and repair may require separate entities. Resolution: keep a single `Order` lifecycle aggregate.
- Assumption: CRM scope might include many non-core backoffice systems. Resolution: exclude deep integrations, BI, and document/export concerns from first CRM-complete release.
- Assumption: extra discovered work could generate a second order. Resolution: keep same order, same invoice.

## Pressure-pass Findings
- Revisited earlier answer: whether `Order` should be created at booking or later.
- Finding: user explicitly wants booking itself to create `Order` so the entire lifecycle is visible in one place.
- Impact: status model, timestamps, scheduling, approvals, and customer views must be embedded into the `Order` lifecycle.

## Brownfield Evidence vs Inference
### Evidence
- `src/main/java/com/vladko/autoshopcore/order/entity/OrderStatus.java:3` defines only `NEW`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.
- `src/main/java/com/vladko/autoshopcore/order/entity/Order.java:20` has no booking/check-in/handover timeline fields.
- `src/main/java/com/vladko/autoshopcore/order/dto/OrderCreateDTO.java:12` only supports customer, vehicle, employee, and free-text problem.
- `src/main/java/com/vladko/autoshopcore/order/controller/OrderController.java:20` exposes create/get/update/assign/estimate/status endpoints only.
- `src/main/java/com/vladko/autoshopcore/order/service/OrderServiceImpl.java:235` enforces a minimal transition graph `NEW -> IN_PROGRESS|CANCELLED -> COMPLETED|CANCELLED`.
- `src/main/java/com/vladko/autoshopcore/entities/EmployeeType.java:3` already has `ADMIN`, `MANAGER`, `MECHANIC`, `RECEPTIONIST` roles.
- `src/main/java/com/vladko/autoshopcore/loyalty/controller/LoyaltyController.java:14` and related loyalty services/entities already exist.
- Parts/procurement modules already support requested parts, quoting, receipt, and procurement flows.

### Inference
- Because order creation is already central and procurement/loyalty exist, the most coherent CRM evolution is to extend `Order` rather than introduce a separate primary booking aggregate.
- Customer approval flows likely need a new approval/timeline submodel, because neither order nor procurement currently encode customer-facing approval states.

## Technical Context Findings
- Current Core is much closer to a repair-order backend than a CRM backend.
- Procurement capabilities can support the “waiting for part” scenario, but the customer approval gate is missing in the visible order lifecycle.
- Loyalty exists, but admin-facing enable/disable/configuration requirements need to be made explicit in the domain/API.

## Condensed Transcript
- Q1 intent: full CRM from booking to completion
- Q2 scope: nearly all CRM core is required in first release
- Q3 non-goals: exclude integrations/BI/export/backoffice depth; keep loyalty config
- Q4 order creation moment: create order at booking
- Q5 pressure pass: keep one order through extra work and no-show branches
- Q6/7 success criteria: happy path + no-show + decline extra work + missing part + receptionist booking + standard service + loyalty disabled
