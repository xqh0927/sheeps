package com.example.sheeps

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.sheeps.core.utils.NetworkMonitor
import com.example.sheeps.theme.ThemeManager
import com.example.sheeps.core.utils.NetworkStatus
import com.example.sheeps.data.repository.SyncRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // MMKV / Toaster / Logcat / TheRouter are initialized via App Startup Initializers
        // declared in the AndroidManifest.xml InitializationProvider block.

        // 初始化主题管理器（从 MMKV 读取上次保存的主题）
        ThemeManager.init()

        setupForegroundBackgroundListener()
        setupNetworkObserver()
    }


    private fun setupForegroundBackgroundListener() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                logcat("AppLifecycle") { "App moved to FOREGROUND" }
                CoroutineScope(Dispatchers.IO).launch {
                    syncRepository.syncDirtyData()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                logcat("AppLifecycle") { "App moved to BACKGROUND" }
                CoroutineScope(Dispatchers.IO).launch {
                    syncRepository.syncDirtyData()
                }
            }
        })
    }

    private fun setupNetworkObserver() {
        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.status.collectLatest { status ->
                if (status == NetworkStatus.ONLINE) {
                    logcat("NetworkObserver") { "Network came ONLINE, triggering sync..." }
                    syncRepository.syncDirtyData()
                }
            }
        }
    }
}
