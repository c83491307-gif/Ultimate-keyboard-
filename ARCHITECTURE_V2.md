# NovaKeys V2 Architecture

## Goal

Rebuild NovaKeys around a reliable Android Input Method Engine (IME), with Android 7.0 / API 24 as the minimum supported version. Reliability, offline operation, privacy, and testability take priority over preserving legacy implementation details.

This document is the implementation contract for the rewrite branch. It does not claim that planned features are already implemented.

## Decisions

### 1. Keep the Android IME service thin

`NovaInputMethodService` is an Android framework adapter only. It owns framework callbacks and connects platform objects to application controllers. It must not own keyboard layout rules, clipboard persistence, theme logic, or feature-specific state.

### 2. Separate state, actions, rendering, and platform access

- **Domain:** immutable keyboard state, editor context, key/action models, layout rules, sensitive-field policy.
- **Controller:** interprets user actions and coordinates domain operations.
- **Platform adapter:** wraps `InputConnection`, `EditorInfo`, lifecycle, insets, and Android-specific APIs.
- **UI:** renders state and forwards user actions. It does not perform database or framework work directly.
- **Feature modules/packages:** clipboard, emoji, snippets, drawing, typography, themes, and vault each own their own state and repository contracts.

### 3. Android 7 compatibility is a design constraint

- Keep `minSdk = 24`.
- Guard APIs introduced after API 24 at the platform boundary.
- Do not copy FlorisBoard's current application configuration wholesale: its current minSdk is higher than NovaKeys' required minimum.
- Keep modern Android integrations optional and degrade safely when unavailable.

### 4. Prefer a native View-based IME surface for V2

Two viable approaches were considered:

1. Compose-first IME surface, similar in spirit to FlorisBoard's Compose root and controller composition locals.
2. Native View-based IME surface with framework lifecycle adapter and explicit controllers.

Choose option 2 for the first stable V2 foundation. It reduces framework/lifecycle uncertainty in the most sensitive Android component, keeps the API-24 path straightforward, and allows the existing Compose-based app/settings screens to remain independent. Compose may still be used in the host application where it is already useful.

### 5. Offline-first and privacy boundaries

- Core typing must work without network access.
- Clipboard and local history must have explicit retention and sensitive-field rules.
- No network client is allowed in the core typing path.
- Optional network-backed features must be isolated and disabled unless explicitly configured.
- Password/sensitive fields must suppress clipboard capture, drawing, image suggestions, and nonessential UI.

## Target package map

```text
com.creazzy.novakeys
  app/
    MainActivity
    setup/
  ime/
    android/
      NovaInputMethodService
      AndroidInputConnection
      EditorInfoMapper
      ImeLifecycleBridge
    domain/
      KeyboardState
      EditorContext
      KeyboardAction
      KeyboardLayout
      SensitiveFieldPolicy
    controller/
      KeyboardController
      InputActionDispatcher
    ui/
      KeyboardRootView
      KeyRowView
      KeyView
      ToolbarView
  feature/
    clipboard/
      ClipboardRepository
      ClipboardController
      ClipboardUi
    emoji/
    snippets/
    themes/
    drawing/
    typography/
  data/
    clipboard/
    vault/
    preferences/
  security/
    DatabaseKeyManager
    SensitiveDataPolicy
  testing/
    ime-smoke-host/
```

Packages are boundaries first; they do not need to become separate Gradle modules until build/test evidence justifies that cost.

## Main usage contract

The UI sends intent, not Android framework calls:

```kotlin
controller.dispatch(KeyboardAction.InsertText("a"))
controller.dispatch(KeyboardAction.DeleteBackward)
controller.dispatch(KeyboardAction.SwitchLanguage("ar"))
controller.dispatch(KeyboardAction.OpenFeature(KeyboardFeature.Clipboard))
```

The controller owns state transitions and delegates text operations to an injected editor adapter. The Android adapter is the only component that directly calls `InputConnection`.

## Ownership

| Concern | Single owner |
|---|---|
| Keyboard mode, language, shift, numeric state | KeyboardController |
| Editor type and sensitivity | EditorInfoMapper + SensitiveFieldPolicy |
| Text commit/delete/selection | AndroidInputConnection |
| Key layout and rendering | IME UI |
| Clipboard persistence and retention | ClipboardRepository |
| Theme resolution | ThemeController |
| Vault encryption/key recovery | Security/data layer |
| Android service lifecycle | NovaInputMethodService |

## Runtime flow

1. Android creates `NovaInputMethodService`.
2. Service constructs the platform adapter and controller; optional features initialize lazily.
3. `onStartInput` maps `EditorInfo` into an immutable `EditorContext`.
4. Sensitive-field policy disables disallowed features before rendering.
5. The UI renders the current `KeyboardState`.
6. A key gesture becomes a `KeyboardAction`.
7. The controller validates and handles the action.
8. Text operations pass through the editor adapter to `InputConnection`.
9. On finish/destroy, observers and callbacks are closed deterministically.

## Failure policy

- Failure in optional clipboard/image sync must never prevent the keyboard from appearing.
- Failure to load an optional feature must leave core typing operational.
- A missing/corrupt vault key must never silently replace an existing key or overwrite encrypted data.
- A failed UI rebuild must not leave the service with a null or detached root view.
- Every background observer/worker must have one owner and a deterministic close path.

## Test strategy

1. **Pure unit tests:** state transitions, layouts, action validation, sensitive-field policy.
2. **Robolectric:** API-24 and API-35 adapter behavior, service startup, editor mapping, lifecycle cleanup.
3. **Instrumented smoke test:** install, enable, select, bind, show, type, delete, switch language, hide.
4. **Compatibility matrix:** API 24 baseline plus a modern API level. Emulator boot failures must be reported separately from application failures.
5. **Feature regression tests:** clipboard retention, encrypted vault recovery, backup import/export, and optional feature isolation.

## Migration and deletion policy

- Build V2 alongside the legacy implementation on this branch.
- Migrate one vertical slice at a time: service lifecycle -> text input -> layouts/languages -> sensitive-field policy -> feature adapters.
- Run unit/build/lint checks after each slice.
- Delete legacy code only after its replacement is wired, tests pass, and repository-wide references are gone.
- Keep the main branch and the existing PR untouched during the rewrite.
- Do not merge automatically.

## Reference projects

- FlorisBoard: lifecycle ownership, controller boundaries, window handling, and UI/state separation. Reuse design ideas, not its current minSdk/build configuration.
- AOSP LatinIME: Android IME contract, editor/input connection behavior, and compatibility expectations.
- AnySoftKeyboard: language/layout and feature organization.

All copied code must be reviewed for its license and attribution requirements. Prefer reimplementing the design pattern when copying is unnecessary.
