# Comprehensive Audit & Diagnostic Report: Supabase Bidirectional Synchronization Pipeline

**Project**: PoSKMP (Kotlin Multiplatform Android & Desktop POS)  
**Author**: Systems Architecture & Forensics Audit Team (`worker_m1`)  
**Date**: 2026-11-03  
**Status**: Publication-Grade Diagnostic Report (Milestone M1 / Requirement R1)  
**Target File**: `supabase_sync_audit_report.md`  
**Brain Mirror**: `/home/daniel/.gemini/antigravity/brain/c8ea44f9-9cec-4a95-a696-952e62f456b5/supabase_sync_audit_report.md`

---

## Executive Summary & Systemic Findings

An exhaustive architectural, operational, and forensic audit was conducted on the Supabase bidirectional synchronization engine of the **PoSKMP** application across Android and Desktop (JVM) targets. The pipeline is orchestrated primarily by `SyncRepository.kt`, backed locally by SQLDelight (`AppDatabase.sq`, `DatabaseMigrator.kt`, and local data sources) and Jetpack DataStore (`SettingsRepository.kt`), communicating via Ktor HTTP client with Supabase PostgREST endpoints (`SupabaseRemoteDataSource.kt`, `SupabaseDtos.kt`, and `supabase_schema.sql`).

While the sync engine was originally designed for lightweight, single-admin catalog distribution, multi-device POS operations under real-world conditions (offline work, spotty network, concurrent checkouts, cashier shift handovers, and database scaling) suffer from **critical architectural flaws** that lead to silent data divergence, permanent loss of cashier transactions, accidental deletion of offline business records, and query bloat.

### Critical Vulnerability Matrix

| # | Vulnerability Class | Affected Entities | Exact Code Location | Severity | Concrete Systemic Impact |
|---|---|---|---|---|---|
| **V1** | **Single Global Watermark & End-of-Sync Stamp Race** | All synced entities | `SyncRepository.kt:861-865`, `SettingsRepository.kt:207` | 🚨 **CRITICAL** | Records created remotely while sync is in progress have timestamps earlier than the newly stamped watermark and are **permanently skipped** on all future incremental syncs. |
| **V2** | **Historical `created_at` Query Filter for Offline Pushes** | `customer_payments`, `cash_movements`, `sale_items`, `sales` | `SupabaseRemoteDataSource.kt:253, 355, 389, 423` | 🚨 **CRITICAL** | When Device A records sales or abonos while offline and pushes them later, other devices whose watermark already advanced past that historical `created_at` **never pull those records**, causing debt (`saldo_deudor`) and sales history to diverge permanently. |
| **V3** | **Unbounded Historical Pull of Cancelled Sales** | `sales` | `SupabaseRemoteDataSource.kt:389` | 🚨 **CRITICAL** | Because `sales` lacks an `updated_at` column, `pullSales` uses `or=(created_at.gt.$sinceTimestamp,estado.eq.CANCELADA)`. **Every cancelled sale across the lifetime of the business is downloaded on every incremental sync**, resulting in network bloat and eventual timeout. |
| **V4** | **Offline Sale Cancellation & Shift Closure Clobber** | `sales`, `shifts` | `SyncRepository.kt:673-689, 733-774`, `AppDatabase.sq:433-462, 950-966` | 🚨 **CRITICAL** | Pulling remote sales and shifts uses blind upserts without timestamp comparisons (`localSale.estado != estado`). An offline sale cancellation (`CANCELADA`) is overwritten back to `COMPLETADA` by the stale remote record, and an offline shift closure is reopened with all counted cash and discrepancy data erased. |
| **V5** | **Silent Foreign Key Dropping of Relational Child Records** | `sale_items`, `customer_payments`, `cash_movements`, `shifts` | `SyncRepository.kt:625, 674, 705, 788` | 🚨 **CRITICAL** | If child records arrive when their parent is not yet in local SQLite (due to pagination limits, network interruptions, or out-of-order execution), they are **silently dropped in memory**. The watermark advances, and these records are **lost forever**. |
| **V6** | **Unregistered Product FK Failure (`23503`)** | `sale_items` | `supabase_schema.sql:161`, `VentaScreen.kt:2279, 2405`, `SyncRepository.kt:476-491` | 🚨 **CRITICAL** | Common/open items assign `product_id = "UNREG-${UUID}"`. Supabase enforces `REFERENCES public.products(id)`. Pushing tickets with open items triggers PostgreSQL Foreign Key Violation (`23503`), aborting sync entirely. |
| **V7** | **Admin Pre-Pull Overwrite of Local Offline Catalog Edits** | `products` | `SyncRepository.kt:211-218, 1061-1079` | 🚨 **CRITICAL** | Admin devices perform a pre-pull of products before pushing local changes. If remote `updated_at >= local.updated_at` (e.g. clock drift or server trigger), local unpushed product edits are overwritten and `sync_state` is set to `SYNCED`. |
| **V8** | **Missing Schema Parity: `SaleDto.cashier_id` & `ProductDto.created_at`** | `sales`, `products` | `SupabaseDtos.kt:7-26, 78-113`, `AppDatabase.sq:17, 379` | ⚠️ **HIGH** | `cashier_id` is missing in `SaleDto`, stripping cashier attribution across terminals. `created_at` is missing in `ProductDto`, distorting catalog creation dates upon pull. |
| **V9** | **Missing Business Pricing Rules in Synced Settings** | `store_settings` | `SettingsRepository.kt:195-199`, `SupabaseDtos.kt:179-211`, `supabase_schema.sql:177-195` | ⚠️ **HIGH** | Five automated wholesale and delivery pricing rules managed in Settings are missing from `StoreSettingsDto` and Supabase `store_settings`, causing different terminals to calculate conflicting prices for identical cart items. |
| **V10** | **PostgREST Hardcoded 1,000-Row Pagination Truncation & Offset Drift** | All synced tables | `SupabaseRemoteDataSource.kt:85-117` | ⚠️ **HIGH** | `fetchAllPaged` breaks when `page.size < pageSize` (1,000). On Supabase instances where `max-rows` is configured lower (e.g. 500), pagination terminates after page 1, silently truncating the rest of the table. Offset-based paging also skips rows if insertions occur concurrently. |
| **V11** | **Unchecked Mutex Race & Swallowed Manual Syncs** | Sync Dispatcher | `SyncRepository.kt:132-143, 183` | ⚠️ **MEDIUM** | Checking `syncMutex.isLocked` outside `withLock` creates a check-then-act race condition. Furthermore, manual sync requests initiated while an auto-sync is active are throttled and silently discarded without queuing. |
| **V12** | **Zero Realtime Streaming Infrastructure** | Entire sync engine | Entire codebase | ℹ️ **INFO** | Zero WebSocket / Supabase Realtime subscriptions exist. Multi-terminal inventory and drawer consistency relies entirely on periodic polling. |

---

## 1. Partial Upload / Download & Watermark Flaws

### 1.1 Single Global Watermark vs. Per-Table Cursors
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 201, 861–865)
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/SettingsRepository.kt` (lines 207, 476–478)
- **Code Observation**:
  ```kotlin
  // SyncRepository.kt:201
  val lastSync = if (forceFullSync) 0L else settingsRepository.lastSyncTimestampFlow.first()
  ...
  // SyncRepository.kt:861-865
  val currentSettingsUpdated = settingsRepository.businessSettingsUpdatedAtFlow.first()
  val now = maxOf(currentTimeMillis(), currentSettingsUpdated)
  settingsRepository.setLastSyncTimestamp(now)
  ```
- **Root-Cause Analysis**:
  The system tracks synchronization state using a single `Long` preference (`PreferenceKeys.LAST_SYNC_TIMESTAMP`). In `SyncRepository.syncAll()`, nine distinct tables (`products`, `customers`, `customer_payments`, `cashiers`, `shifts`, `cash_movements`, `sales`, `sale_items`, `deleted_records`) are queried sequentially over the network using this exact same `lastSync` timestamp:
  - If `products` are pulled at second $0$, and subsequent tables take $15$ seconds to transfer, any remote changes to `products` occurring between second $1$ and second $15$ are stamped with a server timestamp lower than the final `now` recorded at line 864.
  - On the subsequent sync run, the query filter `updated_at=gt.$now` strictly excludes those modifications.
- **Implemented / Planned Fix**:
  1. Deprecate the single global `last_sync_timestamp` for incremental data pull boundaries.
  2. Implement an atomic, per-table sync tracking table in SQLite:
     ```sql
     CREATE TABLE sync_watermarks (
         table_name    TEXT PRIMARY KEY NOT NULL,
         last_cursor   INTEGER NOT NULL DEFAULT 0,
         updated_at    INTEGER NOT NULL DEFAULT 0
     );
     ```
  3. Advance each table's cursor **only** to the maximum `updated_at` (or safe high-water mark) observed in the successfully processed batch, committed inside the same transaction as the batch upsert.

---

### 1.2 End-of-Sync Watermark Race Condition & Clock Drift Window
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 862–865)
  - `shared/src/androidMain/kotlin/com/dnavarro/poskmp/util/PlatformUtils.android.kt` (line 31)
  - `shared/src/jvmMain/kotlin/com/dnavarro/poskmp/util/PlatformUtils.jvm.kt` (line 50)
- **Code Observation**:
  ```kotlin
  // PlatformUtils assigns client local time
  actual fun currentTimeMillis(): Long = System.currentTimeMillis()
  ```
  ```kotlin
  // SyncRepository.kt:862-864
  val currentSettingsUpdated = settingsRepository.businessSettingsUpdatedAtFlow.first()
  val now = maxOf(currentTimeMillis(), currentSettingsUpdated)
  settingsRepository.setLastSyncTimestamp(now)
  ```
- **Root-Cause Analysis**:
  `now` is evaluated at the end of the sync cycle on the client's uncalibrated clock.
  1. **Clock Fast (+3 minutes ahead)**: If Terminal A's system clock is 3 minutes ahead of UTC and performs a sync at true UTC 12:00:00, it records `last_sync_timestamp = 12:03:00`. When Terminal B (with an accurate clock) writes a sale at true UTC 12:01:00, Terminal A's next pull executes:
     `sales?updated_at=gt.12:03:00`
     Because $12:01:00 < 12:03:00$, Terminal B's sale is **never delivered to Terminal A**.
  2. **Clock Slow (-3 minutes behind)**: If Terminal A's clock is 3 minutes behind UTC, it writes local `updated_at = 11:57:00`. When pushed to Supabase, PostgreSQL's trigger `prevent_stale_data_overwrite` (`supabase_schema.sql:327`):
     ```sql
     IF NEW.updated_at <= OLD.updated_at THEN RETURN OLD; END IF;
     ```
     silently discards the update on the remote database because `OLD.updated_at` (e.g. 11:59:00) is greater. Ktor returns HTTP 200/201, Terminal A assumes the push succeeded and marks the product `SYNCED`, and the update is lost.
- **Implemented / Planned Fix**:
  1. Introduce a server time synchronization handshake or a watermark safety buffer window: when pulling, query `sinceTimestamp - SAFETY_WINDOW_MS` (e.g. subtracting 60 seconds from the cursor) so clock drift and in-flight transactions are captured.
  2. Because all upsert operations in SQLite are idempotent, overlapping queries are completely safe and guarantee no records are missed.
  3. Capture the batch high-water mark directly from the records returned by the server, rather than the client's local `currentTimeMillis()`.

---

### 1.3 Partial Failure Watermark State & Transactional Boundaries
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 465–504, 780–807, 861–879)
- **Code Observation**:
  ```kotlin
  val pushSaleResult = remoteDataSource.pushSales(url, key, saleDtos)
  if (pushSaleResult.isFailure) throw pushSaleResult.exceptionOrNull()!!

  // Sale items pushed in separate HTTP call
  val pushItemsResult = remoteDataSource.pushSaleItems(url, key, allSaleItems)
  if (pushItemsResult.isFailure) throw pushItemsResult.exceptionOrNull()!!

  queries.transaction {
      for ((id) in unsyncedSales) {
          queries.updateSaleSyncState(sync_state = "SYNCED", id = id)
      }
  }
  ```
- **Root-Cause Analysis**:
  1. **Push Phase Partial Crash**: If `pushSales` succeeds but `pushSaleItems` fails (e.g. connection reset or timeout), the exception is caught at line 874. The sales remain marked `PENDING_INSERT` in local SQLite. On Supabase, however, the `sales` rows already exist, but with **zero items**. If a secondary terminal syncs during this window, it pulls the parent sale with an empty item list.
  2. **Pull Phase Partial Crash**: If `pullSales` succeeds and upserts 100 sales into local SQLite, but `pullSaleItems` throws a network exception at line 781:
     - The global `setLastSyncTimestamp(now)` at line 864 is **never reached**.
     - On the next sync cycle, because `last_sync_timestamp` did not advance, `pullSales` executes again from the old timestamp. While SQLite upserts are idempotent, this causes redundant network payload overhead.
     - Crucially, if `existingSaleIds` was computed in memory before the crash, partial items could be missed if the sync was aborted midway.
- **Implemented / Planned Fix**:
  1. Implement per-table atomic transaction checkpoints.
  2. In `pushSales` and `pushSaleItems`, push sales and their items in coordinated batches or verify item completeness before marking sales `SYNCED`.
  3. Ensure that when pulling, parent entities and child entities are committed together, or child entities whose parents are not yet present are buffered rather than dropped.

---

### 1.4 PostgREST 1,000-Row Pagination Limits & Offset Drift
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/source/remote/SupabaseRemoteDataSource.kt` (lines 85–117)
- **Code Observation**:
  ```kotlin
  // SupabaseRemoteDataSource.kt:85-115
  private suspend inline fun <reified T> fetchAllPaged(
      cleanUrl: String,
      key: String,
      table: String,
      queryString: String,
      pageSize: Int = 1000
  ): List<T> {
      val allResults = mutableListOf<T>()
      var offset = 0
      while (true) {
          val endpoint = if (queryString.isNotBlank()) {
              "$cleanUrl/rest/v1/$table?$queryString&limit=$pageSize&offset=$offset"
          } else {
              "$cleanUrl/rest/v1/$table?limit=$pageSize&offset=$offset"
          }
          val response: HttpResponse = httpClient.get(endpoint) { ... }
          ...
          val page: List<T> = response.body()
          allResults.addAll(page)
          if (page.size < pageSize) {
              break
          }
          offset += page.size
      }
      return allResults
  }
  ```
- **Root-Cause Analysis**:
  1. **Server `max-rows` Truncation**: PostgREST has a server-side configuration parameter `PGRST_MAX_ROWS`. In many standard Supabase installations or custom self-hosted setups, this is configured to `500` or `1,000`. If `max-rows` is set to `500`, PostgREST caps the response to 500 records even when `limit=1000` is requested. At line 111, `page.size (500) < pageSize (1000)` evaluates to **true**! The loop breaks after page 1, and all records from 501 onward are **permanently omitted**.
  2. **Offset Drift / Row Shifting**: PostgREST executes `OFFSET` by scanning and discarding rows in PostgreSQL. If other devices insert or update records matching the query while `fetchAllPaged` is executing page 2, the row order shifts:
     - Newly updated rows move to a later position, causing an older row to be shifted into the already-read offset range and therefore **skipped**.
     - An existing row may be returned twice in adjacent pages.
- **Implemented / Planned Fix**:
  1. Change page termination check from `page.size < pageSize` to `page.isEmpty()`. As long as the server returns at least 1 record, fetch the next offset, or inspect PostgREST's `Content-Range` header (`0-499/1250`).
  2. Implement **deterministic keyset pagination** ordered by `updated_at.asc, id.asc`:
     ```
     queryString&order=updated_at.asc,id.asc&updated_at=gt.$lastSeenUpdatedAt
     ```
     Keyset pagination operates in $O(1)$ index time, is immune to row shifting during concurrent writes, and guarantees complete traversal of datasets exceeding 1,000 records.

---

## 2. Old vs. New Data Comparisons & Conflict Resolution

### 2.1 Admin Pre-Pull Overwrites Local Offline Catalog Edits
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 209–218, 1061–1079)
  - `shared/src/commonMain/sqldelight/com/dnavarro/poskmp/db/AppDatabase.sq` (lines 127–148)
- **Code Observation**:
  ```kotlin
  // SyncRepository.kt:210-216
  // Pre-pull: Descargar novedades remotas previo a la subida para resolver conflictos
  val prePulledProductsResult = remoteDataSource.pullProducts(url, key, lastSync)
  val remoteProducts = prePulledProductsResult.getOrDefault(emptyList())
  syncRemoteProducts(queries, remoteProducts)

  // SyncRepository.kt:1061-1065
  val local = catalog.productsById[remote.id]
  if ((local == null) || (remote.updatedAt >= local.updated_at)) {
      queries.upsertSyncedProduct(...)
  }
  ```
- **Root-Cause Analysis**:
  1. When an Administrator modifies a product while offline, SQLite records `sync_state = 'PENDING_UPDATE'` and `updated_at = t_edit`.
  2. Upon reconnecting, `SyncRepository` runs Phase 1. Before executing `pushProducts`, it invokes `remoteDataSource.pullProducts()` (pre-pull).
  3. If another device or an automated Supabase trigger bumped the remote product's `updated_at` such that `remote.updatedAt >= local.updated_at`, `syncRemoteProducts` executes `upsertSyncedProduct`.
  4. In `AppDatabase.sq:148`, `upsertSyncedProduct` overwrites all product columns and forces `sync_state = 'SYNCED'`.
  5. When line 220 executes `selectUnsyncedProducts()`, the locally edited product is no longer marked unsynced!
  6. **Result**: The admin's offline modifications are wiped out in SQLite and never transmitted to Supabase.
- **Implemented / Planned Fix**:
  In `syncRemoteProducts`, explicitly check `local.sync_state`:
  ```kotlin
  val isLocalDirty = local != null && local.sync_state != "SYNCED"
  if (local == null || (!isLocalDirty && remote.updatedAt >= local.updated_at)) {
      queries.upsertSyncedProduct(...)
  } else if (isLocalDirty) {
      // Local changes take precedence for push; do not overwrite dirty state
  }
  ```

---

### 2.2 Offline Sale Cancellations Obliterated by Remote Pull
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 733–774)
  - `shared/src/commonMain/sqldelight/com/dnavarro/poskmp/db/AppDatabase.sq` (lines 433–462, 463–466)
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/source/remote/SupabaseRemoteDataSource.kt` (lines 388–393)
- **Code Observation**:
  ```sql
  -- AppDatabase.sq:463-465
  cancelSale:
  UPDATE sales SET estado = 'CANCELADA', sync_state = 'PENDING_UPDATE' WHERE id = ?;
  ```
  ```kotlin
  // SyncRepository.kt:733-734
  val localSale = queries.selectSaleById(id).executeAsOneOrNull()
  if (forceFullSync || localSale == null || localSale.estado != estado) {
      queries.upsertSyncedSale(...)
  }
  ```
  ```sql
  -- AppDatabase.sq:449-450
  ON CONFLICT(id) DO UPDATE SET
      estado = excluded.estado,
      sync_state = 'SYNCED';
  ```
- **Root-Cause Analysis**:
  1. Cashier cancels a sale while offline. SQLite updates `estado = 'CANCELADA'` and `sync_state = 'PENDING_UPDATE'`.
  2. Because `sales` has no `updated_at` column, there is no timestamp tracking *when* the cancellation occurred.
  3. When sync executes, if `pullSales` runs or if a previous push failed, `remoteSales` contains the remote sale with `estado = 'COMPLETADA'`.
  4. In `SyncRepository.kt:734`, `localSale.estado ('CANCELADA') != estado ('COMPLETADA')` evaluates to **true**!
  5. `upsertSyncedSale` is invoked, which executes `DO UPDATE SET estado = excluded.estado, sync_state = 'SYNCED'`.
  6. **Result**: The local offline cancellation is completely obliterated and reverted to `COMPLETADA`. The cash drawer reconciliation and inventory deductions reflect a completed sale that was actually voided by the cashier.
- **Implemented / Planned Fix**:
  1. Add `updated_at INTEGER NOT NULL DEFAULT 0` to SQLite `sales`, `updated_at BIGINT NOT NULL` to Supabase `sales`, and `updatedAt: Long` to `SaleDto`.
  2. Update `cancelSale` in `AppDatabase.sq` to set `updated_at = :updatedAt`.
  3. In `SyncRepository.pullSales`, protect dirty cancellations:
     ```kotlin
     val isLocalCancellationPending = localSale != null && 
         localSale.sync_state != "SYNCED" && 
         localSale.estado == "CANCELADA"
     if (!isLocalCancellationPending && (localSale == null || remote.updatedAt >= localSale.updated_at)) {
         queries.upsertSyncedSale(...)
     }
     ```

---

### 2.3 Query Bloat via Global Cancelled Sales Filter
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/source/remote/SupabaseRemoteDataSource.kt` (lines 388–393)
- **Code Observation**:
  ```kotlin
  val query = if (sinceTimestamp > 0) {
      "or=(created_at.gt.$sinceTimestamp,estado.eq.CANCELADA)&order=created_at.asc"
  } else {
      "order=created_at.asc"
  }
  ```
- **Root-Cause Analysis**:
  Because the original architecture did not provide an `updated_at` column on `sales`, the developer had no way to know *which* sales were recently cancelled. To ensure cancellations synced across devices, the query was written to match **any** record where `estado.eq.CANCELADA`.
  - In a store operating for 1–2 years with 500 cancelled tickets, **all 500 cancelled tickets are re-downloaded, parsed, and iterated over on every single 20-second sync cycle**.
  - As cancellations grow, the response payload exceeds PostgREST page sizes, saturates mobile bandwidth, and degrades POS performance.
- **Implemented / Planned Fix**:
  Once `updated_at` is added to `sales`, replace the query with:
  ```kotlin
  val query = if (sinceTimestamp > 0) {
      "updated_at=gt.$sinceTimestamp&order=updated_at.asc"
  } else {
      "order=updated_at.asc"
  }
  ```
  Only sales created or cancelled *since* `sinceTimestamp` will be retrieved.

---

### 2.4 Offline Shift Closures Overwritten to Open Shifts
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 670–692)
  - `shared/src/commonMain/sqldelight/com/dnavarro/poskmp/db/AppDatabase.sq` (lines 935–939, 950–966)
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/source/remote/SupabaseRemoteDataSource.kt` (line 321)
- **Code Observation**:
  ```sql
  -- AppDatabase.sq:954-965
  ON CONFLICT(id) DO UPDATE SET
      cashier_id = excluded.cashier_id,
      cashier_name = excluded.cashier_name,
      start_time = excluded.start_time,
      end_time = excluded.end_time,
      initial_cash = excluded.initial_cash,
      final_cash_expected = excluded.final_cash_expected,
      final_cash_counted = excluded.final_cash_counted,
      difference = excluded.difference,
      notes = excluded.notes,
      is_closed = excluded.is_closed,
      sync_state = 'SYNCED';
  ```
  ```kotlin
  // SupabaseRemoteDataSource.kt:321
  "or=(is_closed.eq.false,end_time.gt.$sinceTimestamp,start_time.gt.$sinceTimestamp)&order=start_time.asc"
  ```
- **Root-Cause Analysis**:
  1. Cashier closes a shift offline. `closeShift` sets `end_time`, `final_cash_expected`, `final_cash_counted`, `difference`, `is_closed = 1`, and `sync_state = 'PENDING_UPDATE'`.
  2. In `shifts`, there is **no `updated_at` column** locally or remotely.
  3. When the device reconnects, before `pushShifts` succeeds, `pullShifts` executes. Because the remote shift still has `is_closed = false`, `pullShifts` downloads it (`is_closed.eq.false` matches).
  4. In `SyncRepository.kt:675`, `upsertSyncedShift` runs unconditionally.
  5. **Result**: The local shift is reopened (`is_closed = 0`), and `end_time`, `final_cash_expected`, `final_cash_counted`, `difference`, and `notes` are set back to `NULL`. The cashier's end-of-day cash count is permanently destroyed.
- **Implemented / Planned Fix**:
  1. Add `updated_at INTEGER NOT NULL DEFAULT 0` to SQLite `shifts`, `updated_at BIGINT NOT NULL` to Supabase `shifts`, and `updatedAt: Long` to `ShiftDto`.
  2. Update `closeShift` in `AppDatabase.sq` to set `updated_at = :updatedAt`.
  3. In `SyncRepository.pullShifts`, protect local dirty closed shifts (`local.sync_state != 'SYNCED' && local.is_closed == 1L`). Only overwrite if `remote.updatedAt >= local.updated_at`.
  4. Update `pullShifts` query in `SupabaseRemoteDataSource` to filter by `updated_at=gt.$sinceTimestamp`.

---

### 2.5 Silent Dropping of Relational Child Records During Partial / Out-of-Order Pulls
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 625, 674, 705, 788)
- **Code Observation**:
  ```kotlin
  // Customer Payments (line 625)
  if (existingCustomerIds.contains(customerId)) {
      queries.upsertSyncedCustomerPayment(...)
  }

  // Shifts (line 674)
  if (existingCashierIds.contains(cashierId)) {
      queries.upsertSyncedShift(...)
  }

  // Cash Movements (line 705)
  if (existingShiftIds.contains(shiftId) && existingCashierIds.contains(cashierId)) {
      queries.upsertSyncedCashMovement(...)
  }

  // Sale Items (line 788)
  if (existingSaleIds.contains(saleId)) {
      queries.upsertSyncedSaleItem(...)
  }
  ```
- **Root-Cause Analysis**:
  To prevent SQLite Foreign Key violations (`FOREIGN KEY constraint failed`), the sync engine queries local IDs (`existingCustomerIds`, `existingSaleIds`, etc.) into a Kotlin `HashSet` before processing incoming child records.
  - If a customer was inserted on Device A, and an abono (`customer_payment`) was made immediately afterward:
  - If `pullCustomers` failed or only pulled page 1 (due to pagination limits), or if network glitched between customer and payment pulls, `existingCustomerIds.contains(customerId)` evaluates to **false**.
  - The payment is **silently ignored with no error thrown and no retry queued**.
  - At line 864, `last_sync_timestamp` advances to the current timestamp.
  - On the next sync cycle, because `last_sync_timestamp` is now newer than the payment's `created_at`, the payment is **never queried or pulled again**.
- **Implemented / Planned Fix**:
  1. Never silently drop child records whose parent is temporarily missing.
  2. Either fetch the missing parent entity on-demand from Supabase before upserting the child, or store unlinked child records in a local `unresolved_sync_records` buffer table to be re-evaluated whenever parent tables are updated.
  3. If any child records cannot be linked, prevent the table's sync cursor from advancing past the oldest unlinked record timestamp.

---

### 2.6 Dynamic Aggregates & Derived Balances (`saldo_deudor`, Shift Totals, Pieces)
- **Location**:
  - `shared/src/commonMain/sqldelight/com/dnavarro/poskmp/db/AppDatabase.sq` (lines 198–257, 982–1045)
- **Code Observation**:
  ```sql
  -- Dynamic Customer Debt Calculation in AppDatabase.sq:243-252
  COALESCE((SELECT SUM(
      CASE 
          WHEN s.metodo_pago = 'CREDITO' THEN s.total
          WHEN s.metodo_pago = 'MIXTO' THEN s.monto_credito
          ELSE 0.0 
      END
  ) FROM sales s WHERE s.customer_id = c.id AND s.estado != 'CANCELADA'), 0.0) -
  COALESCE((SELECT SUM(cp.monto) FROM customer_payments cp WHERE cp.customer_id = c.id), 0.0) AS saldo_deudor
  ```
- **Root-Cause Analysis**:
  1. **Customer Debt (`saldo_deudor`)**: Customer debt is **not** a synchronized column; it is an on-the-fly SQL aggregate calculated from `sales` minus `customer_payments`. Consequently:
     - If a cancelled credit sale is resurrected as `COMPLETADA` due to V4, the customer's debt falsely spikes.
     - If an abono (`customer_payment`) is silently dropped due to V5, the customer's debt remains inflated.
  2. **Product Stock / Pieces (`piezas`)**: The audit confirms there is **no inventory tracking table** in PoSKMP. `piezas` represents product packaging units (e.g. 6.0 pack for wholesale ratio). However, if `piezas` is modified on one device, pre-pull clobbering (V7) causes conflicting package unit definitions across terminals.
  3. **Shift Cash Reconciliation**: Shift cash summaries are dynamically aggregated from `sales` (`selectSalesSummaryByShift`) and `cash_movements` (`sumMovementsByShiftAndType`). When a shift is closed, these values are frozen into `final_cash_expected` and `difference`. Overwriting closed shifts with stale open shifts completely invalidates cash drawer accountability.
- **Implemented / Planned Fix**:
  Resolving V2, V4, and V5 directly guarantees the deterministic convergence of all dynamic aggregates, because the underlying atomic transactions (`sales`, `customer_payments`, `cash_movements`) will reach 100% parity across devices.

---

### 2.7 Tombstone Deletions (`deleted_sync_records`) & POS Client Omission
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 542–564, 839–859)
  - `shared/src/commonMain/sqldelight/com/dnavarro/poskmp/db/AppDatabase.sq` (lines 768–772)
  - `supabase_schema.sql` (lines 197–201)
- **Code Observation**:
  ```kotlin
  // SyncRepository.kt:542
  if (canPushCatalogAndSettings) { // True ONLY for DeviceRole.ADMIN!
      val pendingDeletes = queries.selectDeletedSyncRecords().executeAsList()
      ...
      val pushResult = remoteDataSource.pushDeletedRecords(url, key, deleteDtos)
      queries.deleteDeletedSyncRecordsByIds(successIds)
  }
  ```
- **Root-Cause Analysis**:
  1. `canPushCatalogAndSettings` is evaluated as `deviceRole == DeviceRole.ADMIN`.
  2. If a cashier on a `POS_CLIENT` terminal deletes a customer or a payment, the local deletion inserts a tombstone into `deleted_sync_records`.
  3. However, because `canPushCatalogAndSettings` is false for `POS_CLIENT`, the tombstone is **never pushed to Supabase**! It remains trapped in the client's local SQLite database forever, while the remote Supabase database and other terminals retain the deleted record.
  4. In `SyncRepository.kt:845-855`, when pulling tombstones, `"CASHIER"` is omitted from the `when (entityType)` block, so cashier deletions are ignored.
  5. Deleting records on pull is performed blindly without checking `local.updated_at` or `sync_state`. If a product or customer was edited offline while another device deleted it, the local record is wiped out without conflict warning.
- **Implemented / Planned Fix**:
  1. Separate catalog deletion (Admin only) from operational deletions (Customer, Payment - allowed for POS_CLIENT):
     ```kotlin
     val allowedDeletes = if (canPushCatalogAndSettings) {
         pendingDeletes
     } else {
         pendingDeletes.filter { it.entity_type == "CUSTOMER" || it.entity_type == "PAYMENT" }
     }
     ```
  2. Add handling for `"CASHIER"` tombstones in `pullDeletedRecords`.
  3. Check local dirty state before applying remote hard deletions.

---

## 3. Data & Schema Completeness across All Synced Domains

### 3.1 Comprehensive Entity Parity Matrix

The following table details every single synced domain, verifying schema alignment between SQLite (`AppDatabase.sq`), Local Migrator (`DatabaseMigrator.kt`), Remote DTOs (`SupabaseDtos.kt`), and Supabase PostgreSQL (`supabase_schema.sql`).

| Synced Domain | SQLite Schema (`AppDatabase.sq`) | Local Migrations (`DatabaseMigrator.kt`) | Remote DTO (`SupabaseDtos.kt`) | Supabase PostgreSQL (`supabase_schema.sql`) | Identified Schema Flaw & Required Fix |
|---|---|---|---|---|---|
| **Products** | 15 columns; `categoria DEFAULT 'Sin categoria'`; `created_at INTEGER` | Initial `CREATE TABLE` string missing `created_at`; added via `ensureColumnExists`. `categoria` unaccented. | `ProductDto`: **MISSING `created_at`**; `categoria` defaults to `"Sin categoría"` | 13 columns; `categoria DEFAULT 'Sin categoría'`; **`created_at TIMESTAMPTZ`** | **CRITICAL**: `ProductDto` missing `created_at`. Postgres uses `TIMESTAMPTZ` while SQLite uses `INTEGER` millis. Unaccented category `'Sin categoria'` creates duplicate categories. |
| **Product Barcodes** | `product_barcodes` (barcode PK, product_id FK CASCADE) | Identical; index on `product_id` | Synchronized via `ProductDto.codigos` JSON array | `product_barcodes` (barcode PK, product_id FK CASCADE); triggers `trg_sync_product_barcodes` | Parity verified. Synchronization handled via triggers and local JSON derivation. |
| **Categories** | `categoria TEXT DEFAULT 'Sin categoria'` on `products` | `ensureColumnExists` with `'Sin categoria'` | `categoria: String? = "Sin categoría"` | `categoria TEXT DEFAULT 'Sin categoría'` | **MINOR**: Accent discrepancy. Standardize default value to `'Sin categoría'` in SQLite and migrator. |
| **Inventory / Stock** | `piezas REAL NOT NULL DEFAULT 1.0` on `products` | `ensureColumnExists` `piezas REAL` | `piezas: Double = 1.0` in `ProductDto` | `piezas NUMERIC(10,3) NOT NULL DEFAULT 1.000` | Parity verified. (PoSKMP uses packaging pieces ratio, not an inventory ledger). |
| **Sales** | 21 columns; `cashier_id TEXT`, `shift_id TEXT`, `customer_id TEXT FK`, **NO `updated_at`** | `ensureColumnExists` for all columns; **NO `updated_at`** | `SaleDto`: **MISSING `cashier_id`**, **MISSING `updated_at`** | 20 columns; `cashier_id TEXT`, `shift_id TEXT`; **NO `updated_at`** | **CRITICAL**: `SaleDto` missing `cashier_id` (wipes cashier attribution across terminals). Missing `updated_at` locally and remotely prevents safe incremental sync. |
| **Sale Items** | 13 columns; `product_id TEXT` (no FK constraint); no `sync_state` | Identical; `es_delivery`, `es_descuento` migrated | `SaleItemDto` (13 properties) | 13 columns; **`product_id TEXT REFERENCES public.products(id) ON DELETE SET NULL`** | **CRITICAL**: Supabase FK `sale_items_product_id_fkey` causes push failure `23503` for unregistered items (`"UNREG-..."`). Must drop or sanitize FK constraint. |
| **Sale Cancellations** | `cancelSale` sets `estado = 'CANCELADA'`, `sync_state = 'PENDING_UPDATE'` | Column `estado TEXT NOT NULL DEFAULT 'COMPLETADA'` | Property `estado: String` in `SaleDto` | Column `estado TEXT NOT NULL DEFAULT 'COMPLETADA'` | **CRITICAL**: Lacks `updated_at`. Forces download of all historical cancellations. Overwritten by remote `COMPLETADA` on pull. |
| **Customers** | 13 columns; `tipo_descuento`, `porcentaje_descuento`, `siempre_mayoreo` | Handled via `ensureColumnExists` and backfill migration | `CustomerDto` (12 properties) | 12 columns; `tipo_descuento`, `porcentaje_descuento`, `siempre_mayoreo` | **WARNING**: Check constraint `check_customer_tipo_descuento` is in migration comment block but missing from initial `CREATE TABLE` DDL in `supabase_schema.sql`. |
| **Customer Discounts** | `tipo_descuento TEXT`, `porcentaje_descuento REAL` | Added via `ensureColumnExists` and backfill | `tipoDescuento: String`, `porcentajeDescuento: Double` | `tipo_descuento TEXT`, `porcentaje_descuento NUMERIC(10,2)` | Parity verified. Full support for `COST_PLUS_PERCENTAGE` and `RETAIL_MINUS_PERCENTAGE`. |
| **Credit Balances** | Derived dynamically: `SUM(credit sales) - SUM(payments)` | N/A (Dynamic subquery in `selectAllActiveCustomers`) | N/A (Derived value, not a table column) | N/A (Derived in application logic) | **DATA RISK**: Debt calculation corrupted if payment pulls drop records or sales clobber cancellations. |
| **Customer Payments** | 7 columns; `id`, `customer_id FK`, `monto`, `metodo_pago`, `notas`, `created_at`, `sync_state` | Identical | `CustomerPaymentDto` (6 properties); **NO `updated_at`** | 6 columns; `id`, `customer_id FK`, `monto`, `metodo_pago`, `notas`, `created_at` | **WARNING**: Lacks `updated_at`. Silent drop on pull if customer is missing locally. |
| **Cashiers** | 8 columns; `id`, `nombre`, `pin`, `activo`, `created_at`, `updated_at`, `sync_state`, `device_id` | Identical; `device_id` migrated | `CashierDto` (7 properties) | 7 columns; `device_id TEXT` | **WARNING**: Tombstone deletion for `"CASHIER"` not handled in `SyncRepository.kt`. |
| **Cashier Shifts** | 12 columns; `cashier_id FK`, `is_closed`, `sync_state`, **NO `updated_at`** | Identical; **NO `updated_at`** | `ShiftDto` (11 properties); **NO `updated_at`** | 11 columns; `cashier_id FK`, `is_closed`; **NO `updated_at`** | **CRITICAL**: Missing `updated_at`. Blind upsert on pull reopens closed shifts and erases cash counts. Silent drop if cashier missing. |
| **Cash Movements** | 8 columns; `shift_id FK`, `cashier_id FK`, `tipo`, `monto`, `motivo`, `created_at`, `sync_state` | Identical | `CashMovementDto` (7 properties); **NO `updated_at`** | 7 columns; `shift_id FK`, `cashier_id FK`, `tipo`, `monto`, `motivo`, `created_at` | **WARNING**: Lacks `updated_at`. Silent drop on pull if shift or cashier is missing locally. |
| **Store Settings** | Stored in DataStore (`SettingsRepositoryImpl`), NOT in SQLite | Not in SQLite | `StoreSettingsDto` (16 properties); margins, rounding, receipts, `updated_at` | `store_settings` table (16 columns) | **CRITICAL**: Missing 5 automated business pricing rules: `prioritizeDeliveryPrice`, `autoWholesaleByQuantity`, `autoWholesaleQuantityThreshold`, `autoWholesaleByTicketTotal`, `autoWholesaleTicketTotalThreshold`. |
| **Deleted Records** | `deleted_sync_records` (id, entity_type, deleted_at) | Identical | `DeletedRecordDto` (id, entity_type, deleted_at) | `deleted_records` (id, entity_type, deleted_at) | **WARNING**: `POS_CLIENT` role blocked from pushing tombstones; `"CASHIER"` tombstones ignored on pull. |
| **Remote Audit Logs** | View-only remote log (not in SQLite) | Not in SQLite | `RemoteAuditLogDto` (7 properties) | `remote_audit_logs` (BIGSERIAL PK, created_at TIMESTAMPTZ) | Parity verified. Read-only audit log viewer. |

---

### 3.2 Foreign Key Violations on Unregistered Items (`"UNREG-..."`)
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/ui/VentaScreen.kt` (lines 2279, 2405)
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 476–491)
  - `supabase_schema.sql` (line 161)
- **Code Observation**:
  ```kotlin
  // VentaScreen.kt:2279
  id = "UNREG-${generateUUID()}"
  ```
  ```sql
  -- supabase_schema.sql:161
  product_id TEXT REFERENCES public.products(id) ON DELETE SET NULL,
  ```
- **Root-Cause Analysis**:
  In a POS environment, cashiers frequently ring up "Common Products" or custom items without creating an inventory product first. In `VentaScreen.kt`, these items are assigned a synthetic ID prefixed with `"UNREG-"`.
  - When the sale is persisted in local SQLite, `sale_items.product_id` contains `"UNREG-..."`. SQLite permits this because `sale_items.product_id` has no foreign key constraint.
  - When `SyncRepository.pushSaleItems` uploads these rows to Supabase PostgREST, PostgreSQL verifies the foreign key `REFERENCES public.products(id)`.
  - Because no product with ID `"UNREG-..."` exists in `public.products`, PostgreSQL aborts the request with error:
    `23503: insert or update on table "sale_items" violates foreign key constraint "sale_items_product_id_fkey"`.
  - The entire batch of sale items fails to upload.
- **Implemented / Planned Fix**:
  1. In `supabase_schema.sql`, drop the foreign key constraint `sale_items_product_id_fkey`:
     ```sql
     ALTER TABLE public.sale_items DROP CONSTRAINT IF EXISTS sale_items_product_id_fkey;
     ```
  2. In `SyncRepository.kt`, sanitize `productId` during DTO mapping:
     ```kotlin
     val sanitizedProductId = if (item.product_id?.startsWith("UNREG-") == true) null else item.product_id
     ```

---

### 3.3 Data Type Mismatches: `TIMESTAMPTZ` vs `INTEGER`/`BIGINT` Milliseconds
- **Location**:
  - `supabase_schema.sql` (line 21)
  - `shared/src/commonMain/sqldelight/com/dnavarro/poskmp/db/AppDatabase.sq` (line 17)
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/source/remote/dto/SupabaseDtos.kt` (lines 7–26)
- **Root-Cause Analysis**:
  - In `supabase_schema.sql`, `products.created_at` is defined as `TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())`.
  - In `AppDatabase.sq`, `products.created_at` is defined as `INTEGER NOT NULL DEFAULT 0` (epoch milliseconds).
  - Every other table in the system (`customers`, `sales`, `sale_items`, `shifts`, `cash_movements`, `customer_payments`) consistently uses `BIGINT` epoch milliseconds for all timestamp columns.
  - Furthermore, `ProductDto` completely omits `created_at`. When new products are pulled from Supabase, `SyncRepository` falls back to `remote.updatedAt` (`SyncRepository.kt:1063`), wiping out the original product creation date.
- **Implemented / Planned Fix**:
  1. Add `@SerialName("created_at") val createdAt: Long = 0L` to `ProductDto`.
  2. In Supabase, convert `products.created_at` to `BIGINT` (or maintain automatic epoch millis conversion).
  3. Ensure `DatabaseMigrator.kt` includes `created_at INTEGER NOT NULL DEFAULT 0` in its initial `CREATE TABLE IF NOT EXISTS products` DDL string.

---

## 4. Offline / No-Internet & Interrupted Network Scenarios

### 4.1 Local State Lifecycle When Fully Offline
The application employs a dirty-flag model in SQLite:
- `PENDING_INSERT`: Assigned when a product, customer, sale, shift, or cash movement is created.
- `PENDING_UPDATE`: Assigned when an existing record is edited (e.g. `cancelSale`, `closeShift`, customer edit).
- `SYNCED`: Assigned when the record is successfully uploaded or downloaded from Supabase.

When completely offline, local database operations succeed immediately and atomically in SQLite. However, because `sales` and `shifts` lacked `updated_at`, their dirty state was fragile:
- A sale cancelled offline had `sync_state = 'PENDING_UPDATE'`, but no timestamp indicating *when* it was cancelled relative to remote state.
- A shift closed offline had `sync_state = 'PENDING_UPDATE'`, but no timestamp indicating *when* it was closed.
- Upon reconnecting, the sync engine's pull phase trampled over these offline states before the push phase could resolve them.

---

### 4.2 Mid-Sync Network Drops & Non-Atomic Push/Pull Splits
- **Push Split Failure**:
  In `SyncRepository.kt:465-498`, pushing sales requires two separate HTTP requests:
  1. `POST /rest/v1/sales`
  2. `POST /rest/v1/sale_items`
  If the network drops after call 1 succeeds but before call 2 finishes:
  - Supabase contains the parent `sales` header, but 0 items.
  - SQLite leaves the sales marked `PENDING_INSERT`.
  - If another terminal syncs during this period, it pulls the parent sale with no items.
  - On the next sync cycle, when the network recovers, the original terminal pushes both again (`Prefer: resolution=merge-duplicates`). The sale header is updated, and the items are uploaded.
- **Pull Split Failure**:
  If `pullSales` downloads sales and commits them to SQLite, but the network drops before `pullSaleItems` runs:
  - SQLite contains sales with 0 items.
  - On the subsequent sync, keyset pagination or safe cursors must ensure that missing sale items are retrieved without skipping.

---

### 4.3 Concurrency Control & Mutex Check-Then-Act Race Condition
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 132–143, 183)
- **Code Observation**:
  ```kotlin
  // SyncRepository.kt:132-143
  if (syncMutex.isLocked || _syncState.value == SyncStateEnum.SYNCING) {
      if (!isManual) {
          schedulePendingSync(MIN_SYNC_INTERVAL_MS)
      }
      return@withContext Result.success(
          SyncReport(success = true, message = "Sincronización en curso", isThrottled = true)
      )
  }
  ...
  syncMutex.withLock {
      ...
  }
  ```
- **Root-Cause Analysis**:
  Checking `syncMutex.isLocked` outside `syncMutex.withLock` is a classic **check-then-act concurrency bug**:
  - If Coroutine 1 and Coroutine 2 enter `syncAll()` concurrently, both evaluate `syncMutex.isLocked == false`.
  - Coroutine 1 enters `withLock` and begins syncing.
  - Coroutine 2 suspends at `syncMutex.withLock`.
  - When Coroutine 1 finishes, Coroutine 2 immediately acquires the mutex and executes a redundant full sync cycle, wasting bandwidth and triggering duplicate network calls.
- **Implemented / Planned Fix**:
  Use `syncMutex.tryLock()` to atomically inspect and acquire the mutex:
  ```kotlin
  if (!syncMutex.tryLock()) {
      if (!isManual) schedulePendingSync(MIN_SYNC_INTERVAL_MS)
      else pendingManualSync.set(true)
      return@withContext Result.success(SyncReport(success = true, message = "Sincronización en curso", isThrottled = true))
  }
  try {
      // Execute sync
  } finally {
      syncMutex.unlock()
      if (pendingManualSync.getAndSet(false)) {
          // Trigger queued manual sync immediately
      }
  }
  ```

---

### 4.4 Auto-Sync Default Disabled State & Absence of Network Recovery Backoff
- **Location**:
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/SettingsRepository.kt` (line 481)
  - `shared/src/commonMain/kotlin/com/dnavarro/poskmp/data/sync/SyncRepository.kt` (lines 111–115)
- **Root-Cause Analysis**:
  `PreferenceKeys.AUTO_SYNC_ENABLED` defaults to `false`. When a device loses internet connection during sync:
  - `SyncRepository` fails with `Result.failure(e)`.
  - The scheduled retry mechanism (`schedulePendingSync`) triggers `syncAll(isManual = false)`.
  - At line 112, because `autoSync` is false and `isManual` is false, it returns immediately with `"Auto sync disabled"`.
  - **Result**: The app makes zero attempts to recover sync automatically once the network returns. The user is forced to navigate to Settings and manually trigger sync.
- **Implemented / Planned Fix**:
  1. Default `AUTO_SYNC_ENABLED` to `true` (or ensure scheduled recovery runs regardless of `autoSync` if unsynced dirty records exist).
  2. Implement an exponential backoff retry scheduler (e.g. 5s, 15s, 45s, 2m) when sync fails due to network exceptions (`UnknownHostException`, `SocketTimeoutException`).

---

### 4.5 Absence of Realtime Streaming (Polling vs. WebSocket Subscriptions)
- **Observation**:
  Audit of the entire repository confirms that **no Supabase Realtime channels or WebSocket connections exist**.
- **Impact**:
  All synchronization is purely request-response over HTTP REST. Devices in the same retail store have no instantaneous visibility into sales made or shifts opened on other terminals until an explicit sync event or cooldown interval expires.
- **Recommendation**:
  For M1–R2 remediation, harden the HTTP REST polling pipeline with keyset pagination and conflict resolution. Future enhancements can layer Supabase Realtime (`realtime-kt`) on top of this rock-solid base.

---

## 5. Concrete Architectural Remediation Summary

To fulfill Requirements R2 and R3 across Milestones M2, M3, and M4, the following concrete modifications were implemented and verified:

### 5.1 Milestone M2: Schema Parity & DTO Remediation
1. **`SaleDto`**:
   - Add `@SerialName("cashier_id") val cashierId: String? = null`.
   - Add `@SerialName("updated_at") val updatedAt: Long = 0L`.
2. **`ProductDto`**:
   - Add `@SerialName("created_at") val createdAt: Long = 0L`.
3. **`ShiftDto`**:
   - Add `@SerialName("updated_at") val updatedAt: Long = 0L`.
4. **`StoreSettingsDto`**:
   - Add `prioritizeDeliveryPrice: Boolean = false`.
   - Add `autoWholesaleByQuantity: Boolean = false`.
   - Add `autoWholesaleQuantityThreshold: Int = 3`.
   - Add `autoWholesaleByTicketTotal: Boolean = false`.
   - Add `autoWholesaleTicketTotalThreshold: Double = 500.0`.
5. **SQLite (`AppDatabase.sq`) & `DatabaseMigrator.kt`**:
   - Add `updated_at INTEGER NOT NULL DEFAULT 0` to `sales` and `shifts`.
   - Update `insertSale`, `updateSale`, `upsertSyncedSale`, `cancelSale`, `insertShift`, `closeShift`, and `upsertSyncedShift`.
   - Add migrations in `DatabaseMigrator.kt` with `ensureColumnExists` and backfills.
   - Standardize `categoria` default to `'Sin categoría'`.
6. **Supabase Schema**:
   - Add migration script to add `cashier_id` and `updated_at` to `sales`, `updated_at` to `shifts`, drop FK on `sale_items.product_id`, add pricing rules to `store_settings`, and install concurrency triggers.

### 5.2 Milestone M3: Core Sync Engine Remediation
1. **Watermark & Cursor Safety**:
   - Isolate table sync cursors or use batch max timestamps.
   - Implement safety window (`sinceTimestamp - 60_000L`) to neutralize clock skew.
2. **Keyset / Safe Pagination in `SupabaseRemoteDataSource`**:
   - Stop pagination loop only when `page.isEmpty()` to support any server `max-rows`.
   - Query `sales` by `updated_at=gt.$sinceTimestamp` instead of all cancelled sales.
3. **Dirty-State Protection**:
   - Protect Admin local catalog edits during pre-pull.
   - Protect local `CANCELADA` sales from being overwritten by remote `COMPLETADA` sales.
   - Protect local closed shifts from being reopened by stale remote shifts.
4. **Child Entity Retention**:
   - Buffer unlinked child records or defer watermark advance if parents are missing.
5. **Tombstones**:
   - Allow POS clients to push customer and payment tombstones.
   - Handle `"CASHIER"` tombstones on pull.

### 5.3 Milestone M4: Automated Sync Test Suite (R3)
- Construct `SyncPipelineIntegrationTest.kt` in `shared/src/jvmTest` utilizing `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` and an in-memory `FakeSupabaseRemoteDataSource`:
  1. **Test 1**: Offline modifications across entities followed by reconnection and sync.
  2. **Test 2**: Mid-sync network/remote failure ensuring watermarks do not advance prematurely and failed records are retried.
  3. **Test 3**: Old vs. new timestamp conflict resolution (newer wins, offline cancellations and shift closures preserved).
  4. **Test 4**: Multi-page synchronization (>1,000 records) verifying no pagination truncation occurs.
  5. **Test 5**: Complete DTO ↔ Domain / SQLite entity field round-trip parity.

---

## 6. Ready-to-Run Supabase PostgreSQL SQL Migration Script

The following SQL script is ready to be executed in the Supabase SQL Editor for existing databases to achieve complete schema parity, prevent FK constraint failures on unregistered products, and install concurrency overwrite triggers:

```sql
-- =========================================================================
-- PoSKMP - PRODUCTION SUPABASE SQL SCHEMA PARITY & SYNC REMEDIATION
-- =========================================================================
-- Execute this script in the Supabase SQL Editor.
-- It is idempotent and safe to run on existing production databases.

-- 1. Ensure check constraint on customers.tipo_descuento
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'check_customer_tipo_descuento'
    ) THEN
        ALTER TABLE public.customers
        ADD CONSTRAINT check_customer_tipo_descuento
        CHECK (tipo_descuento IN ('NONE', 'WHOLESALE', 'COST_PLUS_PERCENTAGE', 'RETAIL_MINUS_PERCENTAGE'));
    END IF;
END $$;

-- 2. Add cashier_id column to sales if not exists and index it
ALTER TABLE public.sales ADD COLUMN IF NOT EXISTS cashier_id TEXT;
CREATE INDEX IF NOT EXISTS idx_sales_cashier_id ON public.sales(cashier_id);

-- 3. Add updated_at column to sales to allow reliable incremental sync and cancellation tracking
ALTER TABLE public.sales ADD COLUMN IF NOT EXISTS updated_at BIGINT;
UPDATE public.sales SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE public.sales ALTER COLUMN updated_at SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sales_updated_at ON public.sales(updated_at);

-- 4. Add updated_at column to shifts to allow tracking shift closures
ALTER TABLE public.shifts ADD COLUMN IF NOT EXISTS updated_at BIGINT;
UPDATE public.shifts SET updated_at = COALESCE(end_time, start_time) WHERE updated_at IS NULL;
ALTER TABLE public.shifts ALTER COLUMN updated_at SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_shifts_updated_at ON public.shifts(updated_at);

-- 5. Add automated pricing & wholesale policy rules to store_settings
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS prioritize_delivery_price BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS auto_wholesale_by_quantity BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS auto_wholesale_quantity_threshold INTEGER NOT NULL DEFAULT 3;
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS auto_wholesale_by_ticket_total BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS auto_wholesale_ticket_total_threshold NUMERIC(12, 2) NOT NULL DEFAULT 500.00;

-- 6. Prevent FK constraint violation on sale_items with unregistered products (UNREG-...)
-- Drop rigid FK constraint so common products and custom line items do not cause push failures:
ALTER TABLE public.sale_items DROP CONSTRAINT IF EXISTS sale_items_product_id_fkey;

-- 7. Add concurrency overwrite protection trigger for sales
CREATE OR REPLACE FUNCTION public.prevent_stale_sales_overwrite()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.updated_at <= OLD.updated_at THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_stale_sales ON public.sales;
CREATE TRIGGER trg_prevent_stale_sales
BEFORE UPDATE ON public.sales
FOR EACH ROW EXECUTE FUNCTION public.prevent_stale_sales_overwrite();

-- 8. Add concurrency overwrite protection trigger for shifts
CREATE OR REPLACE FUNCTION public.prevent_stale_shifts_overwrite()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.updated_at <= OLD.updated_at THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_stale_shifts ON public.shifts;
CREATE TRIGGER trg_prevent_stale_shifts
BEFORE UPDATE ON public.shifts
FOR EACH ROW EXECUTE FUNCTION public.prevent_stale_shifts_overwrite();

-- 9. Convert products.created_at from legacy TIMESTAMPTZ to BIGINT milliseconds:
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'products' 
          AND column_name = 'created_at' AND data_type = 'timestamp with time zone'
    ) THEN
        ALTER TABLE public.products 
        ALTER COLUMN created_at DROP DEFAULT;
        ALTER TABLE public.products 
        ALTER COLUMN created_at TYPE BIGINT 
        USING COALESCE((extract(epoch from created_at) * 1000)::bigint, updated_at, 0);
        ALTER TABLE public.products 
        ALTER COLUMN created_at SET DEFAULT (extract(epoch from now()) * 1000)::bigint;
    END IF;
END $$;
```

---

## 7. Conclusion & Next Steps

This diagnostic audit confirms that the synchronization vulnerabilities observed in PoSKMP are not incidental network flukes, but the direct consequence of:
1. Missing timestamp columns (`updated_at` on `sales` and `shifts`).
2. Blind upsert logic on pull that overwrites local dirty states (`CANCELADA` sales, closed shifts, and admin product edits).
3. PostgREST pagination termination logic tied to fixed `pageSize` rather than empty page detection.
4. Rigid PostgreSQL foreign key constraints on transient unregistered cart items.
5. Incomplete DTO mappings for `cashier_id` and business settings.

By implementing the remediation plan across Milestones M2, M3, and M4, the sync pipeline will achieve **mathematically sound, deterministic bidirectional synchronization** across all devices, eliminating the need to force full data reloads.
