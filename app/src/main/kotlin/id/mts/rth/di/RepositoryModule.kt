package id.mts.rth.di

import id.mts.rth.core.media.StudentPhotoManager
import id.mts.rth.core.security.SessionPreferences
import id.mts.rth.data.repository.AcademicRepository
import id.mts.rth.data.repository.AdminRepository
import id.mts.rth.data.repository.AttendanceRepository
import id.mts.rth.data.repository.AuthRepository
import id.mts.rth.data.repository.GradingRepository
import id.mts.rth.data.repository.StudentRepository
import id.mts.rth.data.repository.SyncEngineRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    // Media & Security Singletons
    single { StudentPhotoManager(androidContext()) }
    single { SessionPreferences(androidContext()) }

    // Repositories
    single { AuthRepository(get(), get(), get()) }
    single { AcademicRepository(get()) }
    single { AttendanceRepository(get(), get(), get(), get()) }
    single { GradingRepository(get(), get(), get()) }
    single { StudentRepository(get(), get(), get(), get()) }
    single { AdminRepository(get(), get(), get(), get(), get(), get()) }
    single { SyncEngineRepository(get(), get(), get(), get()) }
}
