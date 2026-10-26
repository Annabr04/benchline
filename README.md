# Benchline

Fill-line workbench for pharmacy technicians. The order flow is a state machine in a pure Kotlin module, so the rules can be tested without an emulator.

I built this from the bench. I work as a pharmacy technician and I am finishing a Computer Science degree at SNHU. The failures I kept seeing were operational: unclear priority, look-alike drugs, and shift handoffs that lived in someone's memory.

**No real patients.** Training initials, a synthetic formulary, no protected health information.

## What to run

Domain tests do not need Android SDK or a device.

```bash
# JDK 17. Android Studio's embedded JDK is enough.
gradle :domain:test
```

GitHub Actions runs that command on every push. See [`.github/workflows/domain-tests.yml`](.github/workflows/domain-tests.yml).

Open the folder in current Android Studio, let it install the Gradle 9.6 wrapper if it asks, then run the `app` configuration. The interactive model of these same rules is the Benchline preview that ships with the design notes.

## Resume bullet

Built Benchline, an Android fill-line workbench in Kotlin and Jetpack Compose. Modeled prescription flow as a state machine in a pure JVM module, with NDC normalization, look-alike drug guards, and pharmacist-only release. Unit-tested on GitHub Actions without an emulator.

## Layout

| Module | Responsibility |
| --- | --- |
| `domain` | Orders, NDC parsing, look-alike guards, pharmacist-only release, handoff text. No Android imports. |
| `app` | Jetpack Compose UI and a view model. Displays domain errors. Does not re-decide policy. |

## SDLC

| Phase | Where it lives |
| --- | --- |
| Requirements | [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) |
| Design | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| Verification | [`docs/TEST-PLAN.md`](docs/TEST-PLAN.md) and `domain/src/test` |
| Release | This README and the GitHub Actions workflow |

## Rules worth knowing before you read the code

- A 10-digit NDC is ambiguous until it is hyphenated. `4-4-2`, `5-3-2`, and `5-4-1` pad different segments to make the 11-digit billing form.
- A 12-digit barcode is accepted only when the GS1 check digit matches. The first 11 digits are the NDC.
- Look-alike pairs (tall-man lettering) cannot be verified until someone acknowledges the comparison.
- A technician can move an order up to pharmacist check. Only a pharmacist can release it. That check is in `Workflow.advance`, not only in the button.
- An exception remembers the status it interrupted. Resolving it returns there.

## Stack

- Kotlin 2.2, JDK 17
- Android Gradle Plugin 9.4, Gradle 9.6, `compileSdk` 37
- Jetpack Compose BOM 2026.09, Material 3
- Classic Android DSL, with `android.newDsl=false` and `android.builtInKotlin=false`, so the build files stay readable on AGP 9

The Gradle wrapper jar is not committed. Android Studio creates it on open. CI uses `gradle/actions/setup-gradle` and does not need the jar.

## How I would talk about this

1. The state machine is not in the composable, because a UI check can be bypassed and cannot run in CI without an emulator.
2. Raw 10-digit NDCs are rejected because padding the wrong segment changes the billing code.
3. PHI stays out by construction: initials, synthetic Rx numbers, a fake formulary. A production pilot needs access control and an audit log before any real identifier is stored.
4. Exceptions store the previous status. That is the difference between a label and a workflow.
5. Next automation would be a camera barcode feeding `Ndc.normalize`, then one UI test that a technician cannot press Release. Not pixel tests first.
