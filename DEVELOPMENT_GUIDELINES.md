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
* **Single-Responsibility Use Cases**: Create discrete Use Cases in `com.dnavarro.poskmp.domain.usecase` when business logic interacts with repositories and is:
  1. Reused across multiple ViewModels or components (e.g., `FindProductByBarcodeUseCase` shared between `VentaViewModel`, `ChecadorDialog`, and `ProductosViewModel`).
  2. Complex enough that encapsulating it simplifies the ViewModel's state holder responsibilities (e.g., `SaveProductUseCase` and `ApplyBulkModificationUseCase`).
* **Invocable Syntax**: Implement Use Cases as classes defining `operator fun invoke(...)` for clean, functional call sites.
* **Layer Independence**: Use Cases must depend only on Repository interfaces, not on specific Data Sources, SQLDelight classes, or Compose UI elements.

### 6.5 Unidirectional Data Flow (UDF) Guidelines
* **Strict Loop Architecture**:
  * **State Flows Down**: ViewModels expose UI state using an immutable `StateFlow<UiState>`. Composable screens observe state using `collectAsStateWithLifecycle()` and pass state values down to child UI components.
  * **Events Flow Up**: User interactions (typing, tapping, toggling) trigger explicit event callbacks or ViewModel method calls (e.g., `viewModel.onSearchQueryChanged(query)`).
* **No Direct State Mutation**: `@Composable` views must never directly reassign or mutate domain/UI state variables. All state transformations must occur within the ViewModel or Domain Use Cases.
* **Stateful / Stateless Overloads**: Every major screen must provide:
  1. A **Stateful Composable** that accepts the `ViewModel`, collects UI state, and connects event callbacks.
  2. A **Stateless Composable** that accepts raw UI state data and event lambdas, enabling easy Jetpack Compose previews, isolated unit testing, and design iteration.

### 6.6 AAC / KMP ViewModel Guidelines
* **Common `androidx.lifecycle.ViewModel` Base**: All ViewModels must inherit from `androidx.lifecycle.ViewModel` in `commonMain` to ensure multiplatform compatibility across Android and JVM Desktop.
* **Lifecycle-Bound Coroutine Scope**: Launch all ViewModel side-effects inside `viewModelScope`. Do not use `GlobalScope` or unmanaged custom coroutine scopes.
* **UI Framework Decoupling**: ViewModels must never reference Android `Context`, `@Composable` functions, or view layout elements directly. They remain pure Kotlin business state holders.
* **Clean Destruction & Cleanup**: Override `onCleared()` if custom resource closing (such as closing stream listeners or sockets) is required when the ViewModel is destroyed.

### 6.7 Lifecycle-Aware State Collection Guidelines
* **Use `collectAsStateWithLifecycle()`**: All `@Composable` screens and components collecting state from a `Flow` or `StateFlow` must use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose`. Do not use standard `collectAsState()`.
* **Automatic Resource Pausing**: `collectAsStateWithLifecycle()` automatically pauses flow collection when the UI drops below the `STARTED` lifecycle state (e.g. app sent to background), preventing unnecessary CPU work, database reads, or battery drain.
* **Pair with `WhileSubscribed(5000)`**: In ViewModels, expose flows with `SharingStarted.WhileSubscribed(5000)` so that when `collectAsStateWithLifecycle()` pauses collection, upstream data repository subscriptions automatically stop after 5 seconds.

### 6.8 Modeling UI Events in UI State Guidelines
* **No One-Off Event Channels**: ViewModels **must not emit one-off single-fire events** (e.g., using `Channel`, `SharedFlow`, or `SingleLiveEvent`) to trigger UI toasts, dialogs, snackbars, or navigation.
* **State-Driven Outcomes**: Process events immediately in the ViewModel and update the `UiState` data class with the result (e.g., `userMessage = "Export complete"`, `showProductDialogFor = product`, `errorMessage = "Invalid price"`).
* **Clear-State Callbacks**: When the UI finishes displaying a message or when a dialog is dismissed, it calls a ViewModel method (e.g., `viewModel.onMessageShown()` or `viewModel.onDismissDialog()`) to clear the state field back to `null` or `false`.

### 6.9 ViewModel Lifecycle & Context Independence Guidelines
* **Zero `Context` / `Activity` References**: ViewModels **must never hold or accept references to `Context`**, `Activity`, `Fragment`, `View`, or `Lifecycle` types.
* **No Android UI / Resource Dependencies**: ViewModels must not call Android resource lookup methods. String resources (`stringResource(...)`) must be resolved inside `@Composable` functions, while ViewModels expose plain Kotlin types (Strings, numbers, enums, domain data classes).
* **KMP Multiplatform Portability**: By avoiding platform-specific UI references, ViewModels in `commonMain` remain 100% portable and runnable across Android, JVM Desktop, and future KMP targets without modification.

### 6.10 Screen-Level ViewModel Scoping Guidelines
* **Screen-Level Injection Only**: ViewModels must only be instantiated or passed as parameters at **top-level screen composables** or navigation graph destinations (e.g., `ProductosScreen(viewModel)`, `VentaScreen(viewModel)`, `AjustesScreen(viewModel)`).
* **No ViewModels in Reusable UI Components**: Lower-level reusable UI components (e.g., `ProductCard`, `TicketSection`, `CatalogSection`, dialogs, buttons) **must never take a ViewModel parameter**.
* **State & Lambda Callbacks**: Reusable child components must accept only **raw state data parameters** (e.g., `product: Products`, `selectedCategory: String?`) and **event lambda callbacks** (e.g., `onProductClick: (Products) -> Unit`), preserving component reusability, testability, and `@Preview` rendering.

### 6.11 Plain State Holder Classes for Reusable UI Components
* **Plain Kotlin Classes for UI Complexity**: When a reusable UI component contains non-trivial layout state, scroll positions, animation controls, or list selections, encapsulate that state in a **plain Kotlin state holder class** (e.g., `CartState`, `SearchFilterState`) rather than embedding logic directly inside the composable body.
* **No ViewModel Base Requirement**: Plain state holder classes do not extend `androidx.lifecycle.ViewModel`. They contain `mutableStateOf` / Compose state properties and pure Kotlin functions.
* **Support State Hoisting & `remember` Helpers**: Provide a `rememberXState()` function (e.g., `rememberCartState()`) so that the component's state can be created locally or hoisted and controlled externally by parent views.

### 6.12 Do Not Use `AndroidViewModel`
* **Inherit from `ViewModel`, Not `AndroidViewModel`**: ViewModels **must inherit directly from `androidx.lifecycle.ViewModel`**, never `AndroidViewModel`.
* **No `Application` Context Dependencies**: Do not pass the Android `Application` class or `Context` into ViewModel constructors.
* **Inject Context into Data Layer**: Dependencies requiring an `Application` or `Context` (e.g., SQLDelight database drivers, DataStore storage paths, file system providers) must be injected into Data Sources or Repositories in the Data Layer, keeping ViewModels pure and multiplatform-compatible.

### 6.13 Exposing UI State (`uiState` & `WhileSubscribed(5000)`)
* **Single `uiState` Property**: ViewModels must expose a single, consolidated, read-only property named `val uiState: StateFlow<${Screen}UiState>`.
* **Data Class / Sealed Interface Naming**: Name state models using the `${Screen}UiState` convention (e.g., `ProductosUiState`, `VentaUiState`, `AjustesUiState`). Use data classes for screens with concurrent/overlapping state fields, or sealed interfaces for mutually exclusive screen states (`Loading`, `Success`, `Error`).
* **`stateIn` with `SharingStarted.WhileSubscribed(5000)`**: When creating `uiState` from repository streams, transform cold flows using:
  ```kotlin
  val uiState: StateFlow<UiState> = combine(
      flowA, flowB
  ) { a, b -> UiState(a, b) }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UiState()
  )
  ```
  The 5-second buffer (`5000` ms) keeps subscriptions alive during brief configuration changes (e.g., screen rotation) while stopping upstream DB/network subscriptions when the screen is hidden.

### 6.14 Lifecycle-Aware Side Effects in Composables
* **No Overriding Activity Callbacks**: Do not override `Activity` lifecycle callbacks (`onResume`, `onPause`, `onStart`, `onStop`) to run UI-related tasks or register listeners.
* **Compose `LifecycleStartEffect` & `LifecycleResumeEffect`**: Use `LifecycleStartEffect` or `LifecycleResumeEffect` directly inside `@Composable` functions to register hardware listeners (e.g. barcode scanners, sensors, location managers) and perform setup work. Always define cleanup in `onStopOrDispose` or `onPauseOrDispose`.
* **Asynchronous Lifecycle Work**: Use `repeatOnLifecycle` or `collectAsStateWithLifecycle()` to perform or collect asynchronous data bound to component lifecycle events.

### 6.15 Dependency Injection & Constructor Injection Guidelines
* **Constructor Injection Priority**: Always use **constructor injection** to supply dependencies (repositories, data sources, use cases, dispatchers) to ViewModels, Use Cases, and Repositories.
* **No Internal Manual Instantiation**: Do not instantiate concrete repository or data source classes internally inside a ViewModel or Use Case body (e.g. avoid `val repo = ProductRepositoryImpl()`).
* **Depend on Interfaces**: Declare parameters as abstract interface types (e.g., `ProductRepository`) rather than concrete implementation classes (`ProductRepositoryImpl`), maximizing testability and decoupling.
* **Seamless Testability**: Constructor injection enables instantiating classes in unit tests using lightweight fake or in-memory implementations without requiring reflection or complex mocking frameworks.

### 6.16 Dependency Scoping Guidelines
* **Scope Expensive or State-Holding Dependencies**: Scope dependencies to a singleton or component container (e.g. SQLite database connections, SQLDelight drivers, DataStore preference files, HTTP/Supabase clients) when:
  1. The instance holds shared mutable state or in-memory caches that must be consistent across multiple screens.
  2. The instance is expensive to initialize (such as database drivers or network socket clients).
* **Unscoped Factory Default for Stateless Dependencies**: Do not scope lightweight, stateless dependencies (such as Use Cases, data mappers, or formatters) to `@Singleton` unnecessarily. Keep them unscoped so they can be garbage-collected cleanly when no longer in use.

### 6.17 Dependency Injection Framework in KMP (Koin Active DI)
* **Koin KMP Architecture**: `PoSKMP` uses **Koin** (`io.insert-koin:koin-core` & `koin-compose`) as its primary Dependency Injection framework across Kotlin Multiplatform targets (Android & JVM Desktop).
* **Module Segmentation (`AppModule.kt`)**: All dependencies are declared in `com.dnavarro.poskmp.di.AppModule.kt`:
  * `dataModule`: Database driver (`DatabaseDriverFactory`), `AppDatabase`, `SqlDelightProductDataSource`, `ProductRepositoryImpl`, and `SettingsRepositoryImpl` declared as singletons (`single` / `singleOf`).
  * `domainModule`: `GetProductsUseCase`, `SaveProductUseCase`, `FindProductByBarcodeUseCase`, and `ApplyBulkModificationUseCase` declared as factories (`factoryOf`).
  * `viewModelModule`: `ProductosViewModel`, `VentaViewModel`, and `AjustesViewModel` declared as ViewModels (`viewModelOf`).
* **Compose Root Context**: Koin is initialized at the Compose composition root using `KoinApplication(application = { modules(appModule) }) { ... }` in `App.kt`. ViewModels and repositories are injected into composables using `koinViewModel<MyViewModel>()` and `koinInject<MyRepository>()`.













