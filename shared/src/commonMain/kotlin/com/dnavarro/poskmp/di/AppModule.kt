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
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.ui.venta.VentaViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

import com.dnavarro.poskmp.data.getDataStore

val dataModule = module {
    single { DatabaseDriverFactory() }
    single { createDatabase(get()) }
    single { getDataStore() }
    singleOf(::SqlDelightProductDataSource) bind ProductLocalDataSource::class
    singleOf(::ProductRepositoryImpl) bind ProductRepository::class
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
}

val domainModule = module {
    factoryOf(::GetProductsUseCase)
    factoryOf(::SaveProductUseCase)
    factoryOf(::FindProductByBarcodeUseCase)
    factoryOf(::ApplyBulkModificationUseCase)
}

val viewModelModule = module {
    viewModelOf(::ProductosViewModel)
    viewModelOf(::VentaViewModel)
    viewModelOf(::AjustesViewModel)
}

val appModule = listOf(dataModule, domainModule, viewModelModule)
