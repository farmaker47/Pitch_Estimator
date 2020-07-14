package com.george.pitch_estimator.di

import com.george.pitch_estimator.singingFragment.SingingFragmentViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val singingFragmentModule = module {
    viewModel {
        SingingFragmentViewModel(get())
    }
}