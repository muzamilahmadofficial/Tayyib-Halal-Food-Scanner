package com.example.core

import android.content.Context
import com.example.alternatives.AlternativesEngine
import com.example.data.ProductRepository
import com.example.data.SettingsRepository
import com.example.database.TayyibDatabase
import com.example.halalengine.HalalClassificationEngine
import com.example.halalengine.LoadedTaxonomy
import com.example.halalengine.TaxonomyLoader
import com.example.networking.HalalOpenFoodFactsApi
import com.example.networking.NetworkModule
import com.example.networking.OpenFoodFactsBackupApi

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val taxonomy: LoadedTaxonomy by lazy {
        TaxonomyLoader.loadFromAssets(appContext)
    }

    val classificationEngine: HalalClassificationEngine by lazy {
        HalalClassificationEngine(taxonomy)
    }

    val database: TayyibDatabase by lazy {
        TayyibDatabase.build(appContext)
    }

    val api: HalalOpenFoodFactsApi by lazy {
        NetworkModule.createApi()
    }

    val backupApi: OpenFoodFactsBackupApi by lazy {
        NetworkModule.createBackupApi()
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(appContext)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepository(
            api = api,
            backupApi = backupApi,
            engine = classificationEngine,
            historyDao = database.scanHistoryDao(),
            favouriteDao = database.favouriteDao(),
            cacheDao = database.cachedProductDao(),
            networkMonitor = networkMonitor
        )
    }

    val alternativesEngine: AlternativesEngine by lazy {
        AlternativesEngine(productRepository)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val soundFeedbackManager: SoundFeedbackManager by lazy {
        SoundFeedbackManager(appContext)
    }

    val hapticFeedbackManager: HapticFeedbackManager by lazy {
        HapticFeedbackManager(appContext)
    }
}
