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

---

## 6. Architecture Guidelines (Google Recommended Standards)

All new features, refactors, and screens must strictly follow Google's Modern Android Architecture guidelines adapted for Kotlin Multiplatform (KMP):

### 6.1 Data Layer Architecture
* **Repository Abstraction Gatekeeper**: UI components (Composables and ViewModels) **must never interact directly with raw data sources** (e.g., SQLDelight database drivers, DataStore/SharedPreferences, Supabase/Firebase network clients, GPS location, or Bluetooth providers). All application data must be exposed exclusively through a Repository interface.
* **Data Sources Abstraction**: Separate raw database (SQLDelight) and remote API (Supabase) operations into dedicated Data Source implementations (e.g., `ProductLocalDataSource` in `com.dnavarro.poskmp.data.source.local`). Repositories orchestrate data sources rather than calling raw database queries directly.
* **Repository Contracts**: Define interfaces for all repositories (e.g., `ProductRepository`) and provide concrete implementations (`ProductRepositoryImpl`) to ensure easy testability and preview mocking.
* **Guaranteed Main-Safety**: All repository methods performing disk I/O, database reads/writes, or network operations **must be main-safe**. Implement them as `suspend` functions using `withContext(Dispatchers.IO)` or reactive `Flow` streams mapped on `Dispatchers.IO`.

### 6.2 UI Layer Architecture & State Management
* **State Holders (`ViewModel`)**: Each screen must be backed by a dedicated `ViewModel` extending `androidx.lifecycle.ViewModel` located in its feature package (e.g., `com.dnavarro.poskmp.ui.productos.ProductosViewModel`).
* **Immutable UI State**: Screens must observe a single, immutable UI state data class (e.g., `ProductosUiState`) exposed as a `StateFlow<UiState>` from the ViewModel.
* **Unidirectional Data Flow (UDF)**:
  * **State Flows Down**: `@Composable` views observe state reactively using `collectAsStateWithLifecycle()`.
  * **Events Flow Up**: User interactions (button clicks, form inputs, dialog toggles) call ViewModel functions (`viewModel.saveProduct(...)`), which handle business logic and update state.
* **Composable Signatures**: Always place `modifier: Modifier = Modifier` as the **first optional parameter** in `@Composable` functions.

### 6.3 Coroutines & Flows Best Practices
* **Inter-Layer Async Communication**:
  * Use Kotlin **`suspend` functions** for one-shot asynchronous tasks (e.g., saving a product, soft deleting, updating settings).
  * Use Kotlin **`Flow` / `StateFlow`** for observable streams of data across layers (e.g., database changes via SQLDelight `.asFlow().mapToList(Dispatchers.IO)`, DataStore preferences, and UI states).
* **Structured Concurrency Scopes**:
  * Launch ViewModel tasks inside `viewModelScope`.
  * Launch UI side-effects inside `LaunchedEffect` or `rememberCoroutineScope()` within `@Composable` views. Avoid unmanaged global scope launches.
* **Cold Streams to Hot StateFlow Conversion**: In ViewModels, combine cold repository flows into a single hot `StateFlow` using `.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ...)`.
* **Explicit Dispatchers & Main-Safety**: Repositories and Data Sources must execute disk I/O, SQL operations, or network tasks on `Dispatchers.IO` using `withContext(Dispatchers.IO)`. ViewModel functions calling these `suspend` methods can safely run on the main thread.

### 6.4 Domain Layer Architecture & Use Cases (Optional Layer)
* **Single-Responsibility Use Cases**: Create discrete Use Cases in `com.dnavarro.poskmp.domain.usecase` when business logic interact with repositories and is:
  1. Reused across multiple ViewModels or components (e.g., `FindProductByBarcodeUseCase` shared between `VentaViewModel`, `ChecadorDialog`, and `ProductosViewModel`).
  2. Complex enough that encapsulating it simplifies the ViewModel's state holder responsibilities (e.g., `SaveProductUseCase` and `ApplyBulkModificationUseCase`).
* **Invocable Syntax**: Implement Use Cases as classes defining `operator fun invoke(...)` for clean, functional call sites.
* **Layer Independence**: Use Cases must depend only on Repository interfaces, not on specific Data Sources, SQLDelight classes, or Compose UI elements.



