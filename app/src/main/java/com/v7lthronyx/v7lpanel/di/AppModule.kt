package com.v7lthronyx.v7lpanel.di

import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.db.V7LDatabase
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.data.repository.SubscriptionRepository
import com.v7lthronyx.v7lpanel.data.security.SecureTokenStore
import com.v7lthronyx.v7lpanel.domain.usecase.FormatTrafficUseCase
import com.v7lthronyx.v7lpanel.ui.screens.home.HomeViewModel
import com.v7lthronyx.v7lpanel.ui.screens.locations.LocationsViewModel
import com.v7lthronyx.v7lpanel.ui.screens.login.LoginViewModel
import com.v7lthronyx.v7lpanel.ui.screens.management.ManagementViewModel
import com.v7lthronyx.v7lpanel.ui.screens.subscription.SubViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { SettingsDataStore(androidContext()) }
    single { SecureTokenStore(androidContext()) }

    single { V7LDatabase.getInstance(androidContext()) }
    single { get<V7LDatabase>().serverProfileDao() }
    single { get<V7LDatabase>().manualConfigDao() }
    single { get<V7LDatabase>().favoriteConfigDao() }
    single { get<V7LDatabase>().connectionRecordDao() }

    factory { FormatTrafficUseCase() }

    viewModel { LoginViewModel(get()) }
    viewModel { ManagementViewModel() }

    viewModel { (uuid: String) ->
        SubViewModel(
            repo       = SubscriptionRepository(SessionHolder.getOrCreateClient()),
            uuid       = uuid,
            serverUrl  = SessionHolder.serverUrl.trim().trimEnd('/'),
            manualDao  = get()
        )
    }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel {
        LocationsViewModel(
            favoriteConfigDao = get(),
            manualConfigDao   = get(),
            settingsDataStore = get(),
            repo = SubscriptionRepository(SessionHolder.getOrCreateClient())
        )
    }
}
