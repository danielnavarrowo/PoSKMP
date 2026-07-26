# Point of Sale Kotlin Multiplatform (PoSKMP) Development Guide

Welcome to the PoSKMP project! This guide compiles all the business logic, technical stack details, UI behaviors, state management rules, and key shortcut configurations implemented in the codebase. Use this document as the reference source of truth when modifying, refactoring, or expanding the application.

---

## 1. Project Description
**PoSKMP** is a high-performance Point of Sale (POS) system designed for retail environments, supporting both Desktop (JVM) and Mobile (Android) platforms using a single Kotlin Multiplatform (KMP) codebase. It is designed to allow cashiers to check out products at maximum efficiency using quick keyboard shortcuts, barcode scanners, and custom layouts. It is meant to work offline, but an online sync option to a supabase project is given so the database can be shared between devices.

---

## 2. Technical Stack
- **Languages**: Kotlin
- **Design guidelines**: Material 3 Expressive
- **UI Framework**: Compose Multiplatform (Jetpack Compose shared UI)
- **Architecture**: MVI / MVVM pattern with hoisted states for screen persistence
- **Local Storage**: SQLDelight (cross-platform SQLite driver wrapper)
- **Platforms**:
    - Android (Android target compile)
    - JVM (Desktop executable target compile)

---

## 3. UI Requirements & Layout Design

To avoid extremely large and monolithic user interface files, strict modular segmentation should be applied to views:

* **Views and Interface:** Screens should be subdivided into independent logical components in the `com.dnavarro.poskmp.ui` package.

* Example: In the Sales module, the catalog grid is delegated to `CatalogSection.kt`, and the shopping cart to `TicketSection.kt`.

* *Rule:* When creating new screens, segment reusable components and logical sections into separate files.

* **Utilities:** Common price formatting methods, quantities, or parsers should be centralized in `com.dnavarro.poskmp.util` (such as `FormatUtils.kt`).



---

## 4. User Interface (UI/UX) Requirements
* **Language:** **Absolutely all** user-oriented text, labels, placeholders, and error messages must be in **Spanish**. Class names, methods, variables, resources, etc. must be named in english. DON'T hardcode strings into the code, 
* use string resources instead. 


* **Premium Aesthetics:**
* Use harmonious Material 3 color schemes (avoid pure, flat primary colors). * Implements rounded corners (`MaterialTheme.shapes.medium` / `large`) and clean backgrounds (`surfaceContainerLowest`).

* Adds micro-animations for selection states and element transitions.

---

Keep in mind, the app should have adaptive layouts in compact, medium and wide screens, using canonical layouts from Compose when possible. 


---


## 5. Common Pitfalls & Compose Gotchas

1. **Closure Capture in LaunchedEffects**: When launching a side-effect that has a delay (e.g., waiting for layout pass to focus a list row), do not reference mutable or hoisted parameter states directly inside the delayed block. Calculate a local snapshot variable (e.g. `val targetIndex = selectedIndex`) before the delay and use it, avoiding race conditions with subsequent recompositions.
2. **Dynamic Composables in loops**: When instantiating `FocusRequester()` dynamic instances inside an indexed loop, wrap them in `remember(index)` so that recompositions do not re-instantiate new focus request instances, which would invalidate active focus states.
