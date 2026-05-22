# Deep Interview Transcript Summary — CRM Core Full CRM

- Profile: standard
- Context type: brownfield
- Final ambiguity: 0.16
- Threshold: 0.20
- Pressure pass: completed
- Context snapshot: `.omx/context/crm-core-full-crm-20260513T144204Z.md`

## Summary of clarified answers

### Intent
Core must become the system of record for the full customer-service process in the auto service CRM: from appointment booking to handover of the vehicle and closed order history.

### Desired outcome
- Client can create an order online or through reception/manager.
- Order is created immediately at booking time.
- Order contains vehicle, initial problem, standard service selection, and booked date/time.
- Staff work against one shared order with role-specific views.
- Order moves through CRM-aware statuses until completion/cancellation.
- Loyalty rules can be configured or disabled by admin.

### Scope
Phase 1 should include the CRM core flow end-to-end, not just a narrow MVP:
- booking
- visit planning
- reception/check-in
- mechanic execution flow
- customer approvals for extra works
- part approval + procurement linkage
- ready-for-pickup and handover
- customer/history visibility
- admin loyalty configuration

### Non-goals
Out of scope for the first CRM-complete release:
- deep external integrations
- financial exports / accounting document pipelines
- advanced BI/analytics
- omnichannel communication center
- advanced warehouse automation beyond current operational needs
- advanced procurement orchestration beyond existing ordering flow

### Decision boundaries
- `Order` is the central entity from booking onward.
- One order equals one invoice.
- Additional issues discovered during work remain inside the same order.
- No-show cancels/closes the order as cancelled, not completed.
- Extra parts always require client approval; after approval the manager procures if not in stock.
- Standard services may be preconfigured by admin and selected during booking.
- Loyalty UI visibility can depend on whether loyalty is enabled; backend still needs feature-state awareness.

### Pressure-pass resolution
Question revisited: whether booking and repair should be split into separate entities/work orders.
Resolution: keep a single `Order` throughout. New work discovered during inspection or repair does not create a new work order; it triggers approval states within the same order.

## Required end-to-end scenarios
1. Online booking -> planned mechanic slot -> vehicle check-in -> work -> extra work approval -> completion -> handover.
2. No-show -> booking slot released -> order cancelled -> customer sees cancelled state.
3. Customer refuses extra work -> only originally agreed work is completed -> handover.
4. Part not in stock -> mechanic requests part -> customer approves -> manager procures -> stock receipt -> work resumes.
5. Front-desk booking / immediate drop-off -> order created by receptionist -> vehicle immediately checked in -> work starts.
6. Standard maintenance flow -> standard service selected at creation time, with or without approval requirements.
7. Loyalty disabled -> order flow still works cleanly without loyalty actions visible in UI.
