package id.mts.rth.di

import id.mts.rth.ui.screens.admin.AdminViewModel
import id.mts.rth.ui.screens.attendance.AttendanceViewModel
import id.mts.rth.ui.screens.auth.AuthViewModel
import id.mts.rth.ui.screens.grading.GradingViewModel
import id.mts.rth.ui.screens.home.HomeViewModel
import id.mts.rth.ui.screens.students.StudentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { AttendanceViewModel(get(), get(), get()) }
    viewModel { GradingViewModel(get(), get(), get(), get()) }
    viewModel { StudentViewModel(get(), get(), get()) }
    viewModel { AdminViewModel(get(), get(), get(), get()) }
}
