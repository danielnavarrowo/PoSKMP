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
* **Language:** **Absolutely all** user-oriented text, labels, placeholders, and error messages must be in **Spanish**.


* **Premium Aesthetics:**
* Use harmonious Material 3 color schemes (avoid pure, flat primary colors). * Implements rounded corners (`MaterialTheme.shapes.medium` / `large`) and clean backgrounds (`surfaceContainerLowest`).

* Adds micro-animations for selection states and element transitions.

---

### A. Compact (Mobile) View
- Activated when the screen width is less than `600.dp`.
- Uses a **tab system** (SecondaryTabRow) containing:
    1. **Catálogo**: Product search list, categories chips, and favorite toggle buttons.
    2. **Ticket**: Active shopping cart items list, checkout, and wholesale actions.

### B. Widescreen (Desktop) View
- Activated on wider screens (width $\ge$ `600.dp`).
- Displays both **Catálogo** and **Ticket** side-by-side.
- **Widescreen Resizable & Swappable Layout**:
    - The columns are separated by an interactive, draggable vertical **divider bar** (`detectHorizontalDragGestures`).
    - Cashiers can drag the bar left and right to adjust the column width ratio.
    - **Double-clicking** (`detectTapGestures(onDoubleTap = ...)`) on the divider bar swaps the positions of the columns (sending the Ticket to the left and Catalog to the right, or vice versa).

---


## 5. Common Pitfalls & Compose Gotchas

1. **Closure Capture in LaunchedEffects**: When launching a side-effect that has a delay (e.g., waiting for layout pass to focus a list row), do not reference mutable or hoisted parameter states directly inside the delayed block. Calculate a local snapshot variable (e.g. `val targetIndex = selectedIndex`) before the delay and use it, avoiding race conditions with subsequent recompositions.
2. **Dynamic Composables in loops**: When instantiating `FocusRequester()` dynamic instances inside an indexed loop, wrap them in `remember(index)` so that recompositions do not re-instantiate new focus request instances, which would invalidate active focus states.
