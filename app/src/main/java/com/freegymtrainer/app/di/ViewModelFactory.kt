package com.freegymtrainer.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Wraps a lambda that builds a ViewModel - lets each screen inject its AppContainer manually. */
class GenericViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
