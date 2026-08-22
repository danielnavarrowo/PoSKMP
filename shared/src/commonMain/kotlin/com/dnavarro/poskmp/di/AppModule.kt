package com.dnavarro.poskmp.di

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.ProductRepositoryImpl
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.SettingsRepositoryImpl
import com.dnavarro.poskmp.data.source.local.ProductLocalDataSource
import com.dnavarro.poskmp.data.source.local.SqlDelightProductDataSource
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.db.createDatabase
import com.dnavarro.poskmp.domain.usecase.ApplyBulkModificationUseCase
import com.dnavarro.poskmp.domain.usecase.FindProductByBarcodeUseCase
import com.dnavarro.poskmp.domain.usecase.GetProductsUseCase
import com.dnavarro.poskmp.domain.usecase.SaveProductUseCase
import com.dnavarro.poskmp.ui.ajustes.AjustesViewModel
import com.dnavarro.poskmp.ui.clientes.ClientesViewModel
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.ui.venta.VentaViewModel
import com.dnavarro.poskmp.ui.ventas.VentasViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import com.dnavarro.poskmp.data.getDataStore
import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.data.CustomerRepositoryImpl
import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.data.SaleRepositoryImpl
import com.dnavarro.poskmp.data.source.local.CustomerLocalDataSource
import com.dnavarro.poskmp.data.source.local.SaleLocalDataSource
import com.dnavarro.poskmp.data.source.local.SqlDelightCustomerDataSource
import com.dnavarro.poskmp.data.source.local.SqlDelightSaleDataSource
import com.dnavarro.poskmp.domain.usecase.GetCustomerAccountStatementUseCase
import com.dnavarro.poskmp.domain.usecase.GetCustomersUseCase
import com.dnavarro.poskmp.domain.usecase.GetSalesSummaryUseCase
import com.dnavarro.poskmp.domain.usecase.PrintReceiptUseCase
import com.dnavarro.poskmp.domain.usecase.RecordCustomerPaymentUseCase
import com.dnavarro.poskmp.domain.usecase.RecordSaleUseCase
import com.dnavarro.poskmp.domain.usecase.SaveCustomerUseCase
import com.dnavarro.poskmp.printer.ReceiptPrinter
import com.dnavarro.poskmp.printer.createReceiptPrinter

import com.dnavarro.poskmp.data.source.remote.SupabaseRemoteDataSource
import com.dnavarro.poskmp.data.source.remote.SupabaseRemoteDataSourceImpl
import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.data.sync.SyncRepositoryImpl
import com.dnavarro.poskmp.data.updater.UpdateRepository

val dataModule = module {
    single { DatabaseDriverFactory() }
    single { createDatabase(get()) }
    single { getDataStore() }
    singleOf(::SqlDelightProductDataSource) bind ProductLocalDataSource::class
    singleOf(::ProductRepositoryImpl) bind ProductRepository::class
    singleOf(::SqlDelightSaleDataSource) bind SaleLocalDataSource::class
    singleOf(::SaleRepositoryImpl) bind SaleRepository::class
    singleOf(::SqlDelightCustomerDataSource) bind CustomerLocalDataSource::class
    singleOf(::CustomerRepositoryImpl) bind CustomerRepository::class
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
    single<ReceiptPrinter> { createReceiptPrinter() }
    singleOf(::UpdateRepository)
    single<SupabaseRemoteDataSource> { SupabaseRemoteDataSourceImpl() }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get()) }
}

val domainModule = module {
    factoryOf(::GetProductsUseCase)
    factoryOf(::SaveProductUseCase)
    factoryOf(::FindProductByBarcodeUseCase)
    factoryOf(::ApplyBulkModificationUseCase)
    factoryOf(::RecordSaleUseCase)
    factoryOf(::GetSalesSummaryUseCase)
    factoryOf(::GetCustomersUseCase)
    factoryOf(::SaveCustomerUseCase)
    factoryOf(::RecordCustomerPaymentUseCase)
    factoryOf(::GetCustomerAccountStatementUseCase)
    factoryOf(::PrintReceiptUseCase)
}

val viewModelModule = module {
    viewModelOf(::ProductosViewModel)
    viewModelOf(::VentaViewModel)
    viewModelOf(::ClientesViewModel)
    viewModelOf(::AjustesViewModel)
    viewModelOf(::VentasViewModel)
}

val appModule = listOf(dataModule, domainModule, viewModelModule)

fun initKoin() {
    if (org.koin.core.context.GlobalContext.getOrNull() == null) {
        org.koin.core.context.startKoin {
            modules(appModule)
        }
    }
}
