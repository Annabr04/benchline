# Requirements

Benchline is a training workbench, not a medical device and not a system of record.

## Users

- **Technician.** Moves an order from received through fill, verifies the NDC, counts the product, and flags exceptions.
- **Pharmacist.** Does everything a technician can do, and is the only role that can release an order from pharmacist check.

The role is an explicit switch in this sample so the authorization rule is visible. A production app would take it from sign-in.

## Stories

1. As a technician, I see the line ordered so STAT and waiters are not buried under routine work.
2. As a technician, I cannot send an order to fill until the NDC matches the product on the order.
3. As a technician, I cannot verify a look-alike drug until I acknowledge that I compared it with its pair.
4. As a technician, I cannot send an order to the pharmacist until the counted quantity matches the prescribed quantity.
5. As a technician, I am blocked if I try to release an order.
6. As a pharmacist, I can release an order that has reached pharmacist check.
7. As either role, I can flag an insurance reject, an out-of-stock, a clarification, or a LASA hold, and resolving it returns the order to the status it left.
8. As either role, I can read a shift handoff that lists open STAT orders, waiters, exceptions, and look-alike drugs still on the line.

## Out of scope

- Real patient identifiers, claims, inventory, or a live drug database.
- Camera barcode scanning. The normalizer already accepts a 12-digit barcode so a camera can call it later.
- Persistence across process death. The sample keeps state in the view model.

## Acceptance

A behavior is done when a domain test fails if the rule is removed, and the screen shows the domain's error instead of inventing its own.
