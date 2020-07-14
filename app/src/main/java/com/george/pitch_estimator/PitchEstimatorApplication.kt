package com.george.pitch_estimator

import android.app.Application
import com.george.pitch_estimator.di.singingFragmentModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PitchEstimatorApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            //androidContext(applicationContext)
            androidContext(this@PitchEstimatorApplication)
            modules(
                singingFragmentModule
            )
        }

        delayedInit()
    }

    private fun delayedInit() {
        applicationScope.launch {
            Thread.sleep(4_000)
        }
    }
}