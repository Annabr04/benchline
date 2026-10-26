# Test plan

## Automated now

`domain/src/test/kotlin/dev/annabrackat/benchline/DomainTest.kt`, run with `gradle :domain:test` and on GitHub Actions.

| Case | Expectation |
| --- | --- |
| 4-4-2, 5-3-2, 5-4-1 labels | Pad the correct segment |
| 10 digits, no hyphens | Rejected |
| GS1 sample `036000291452` | Check digit accepted; a flipped digit rejected |
| Barcode built from a formulary NDC | Normalizes back to that NDC |
| Received | One step lands on data entry |
| Data entry without verification | Blocked with code `ndc` |
| Look-alike without acknowledgement | Blocked with code `lasa` |
| Look-alike with acknowledgement | Verified |
| Short count | Blocked with code `count` |
| Technician at pharmacist check | Blocked with code `role` |
| Pharmacist at pharmacist check | Status becomes ready |
| Flag then resolve | Previous status restored |
| Count before fill | Rejected |
| Seeded handoff | Open STAT kept, exceptions listed |

## Not automated, on purpose

Compose screens are reviewed by running the app. An Espresso test that a technician cannot release would be the first UI test, because it locks a rule. Screenshot tests would not.

## Manual script

1. Open the queue. Confirm STAT prednisone is present and the role reads Tech.
2. Select it and tap Release. Expect the pharmacist-only error.
3. Switch to RPh and release. The order leaves the open count.
4. Open Verify, choose the hydrOXYzine waiter, submit its label NDC without the acknowledgement. Expect the look-alike error.
5. Acknowledge, verify, then move the order forward from the queue.
6. Open Handoff. The spoken brief should mention the remaining open work and say it is not a real patient record.
