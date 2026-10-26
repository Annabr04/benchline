# Architecture

```
app (Compose, ViewModel)
        │  calls
        ▼
domain (pure Kotlin / JVM)
   Ndc · Workflow · Handoff · Formulary
```

The app is allowed to choose which button to show. It is not allowed to decide whether the transition is legal. `Workflow` returns `DomainResult.Ok` or `DomainResult.Err`. The view model stores the error string. The composable renders it.

## Why a second module

`domain` does not depend on Android. `gradle :domain:test` runs on a plain JDK in CI. That is the point of the split: the risky rules are the cheapest thing to test.

`SeedData` lives in the domain main source so the app and the tests share one fixture. It is data, not policy. `Workflow` does not import it.

## State machine

```
received → data_entry → in_fill → pharmacist_check → ready
                \          \              \
                 +----------+--------------+→ exception
                                              (remembers the status it left)
```

Guards:

| From | To | Guard |
| --- | --- | --- |
| data_entry | in_fill | NDC verified, and look-alike acknowledgement when the drug has a pair |
| in_fill | pharmacist_check | counted quantity equals prescribed quantity |
| pharmacist_check | ready | role is pharmacist |
| anything active | exception | a reason is required |
| exception | previous status | the saved status is restored and the reason is cleared |

Ready orders leave the line. Returning a released order to stock is a different workflow and is intentionally absent.

## NDC

US label NDCs are 10 digits in one of three shapes. Billing and many barcodes want 11 digits in 5-4-2 form. The short segment gets a leading zero:

| Label | 11-digit |
| --- | --- |
| 4-4-2 | pad the labeler |
| 5-3-2 | pad the product |
| 5-4-1 | pad the package |

A raw 10-digit string is rejected. Without hyphens the short segment is unknown, and guessing would silently bill the wrong code.

A 12-digit value is treated as a GTIN-12: the check digit is verified, then the first 11 digits are the NDC.

## What this is not

There is no Hilt, Room, or CameraX. Adding them before the rules were tested would be ceremony. The next production steps, in order, are sign-in for the role, an audit log of transitions, and a camera that calls `Ndc.normalize`.
