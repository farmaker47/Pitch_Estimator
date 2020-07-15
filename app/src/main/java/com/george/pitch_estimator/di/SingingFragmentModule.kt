package com.george.pitch_estimator.di

import com.george.pitch_estimator.SingRecorder
import com.george.pitch_estimator.singingFragment.SingingFragmentViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val singingFragmentModule = module {

    single { SingRecorder("hotKey", 0, get()) }

    viewModel {
        SingingFragmentViewModel(get())
    }
}