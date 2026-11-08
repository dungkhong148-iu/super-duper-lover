package com.superduperlover.finance

import android.app.Application
import android.content.Context
import com.superduperlover.finance.data.AppDatabase
import com.superduperlover.finance.data.Repo

/** Built once, held by [FinanceApplication]. No DI framework. */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val repo: Repo = Repo(database)
}

class FinanceApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
