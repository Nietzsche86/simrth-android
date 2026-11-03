package id.mts.rth.di

import id.mts.rth.data.local.db.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    // Singleton Room AppDatabase
    single { AppDatabase.buildDatabase(androidContext()) }

    // DAOs
    single { get<AppDatabase>().masterAcademicDao() }
    single { get<AppDatabase>().studentDao() }
    single { get<AppDatabase>().attendanceDao() }
    single { get<AppDatabase>().gradingDao() }
    single { get<AppDatabase>().syncOutboxDao() }
}
