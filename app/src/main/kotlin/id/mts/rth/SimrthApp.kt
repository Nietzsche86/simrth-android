package id.mts.rth

import android.app.Application
import id.mts.rth.di.databaseModule
import id.mts.rth.di.networkModule
import id.mts.rth.di.repositoryModule
import id.mts.rth.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SimrthApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SimrthApp)
            modules(
                databaseModule,
                networkModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}
