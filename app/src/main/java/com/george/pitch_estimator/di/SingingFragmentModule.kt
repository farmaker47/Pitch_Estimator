package com.george.pitch_estimator.di

import com.george.pitch_estimator.PitchModelExecutor
import com.george.pitch_estimator.SingRecorder
import com.george.pitch_estimator.singingFragment.SingingFragmentViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val singingFragmentModule = module {

    single { SingRecorder("hotKey", 0, get()) }
    single { PitchModelExecutor(get(), getKoin().getProperty("koinUseGpu")!!) }

    viewModel {
        SingingFragmentViewModel(get())
    }
}