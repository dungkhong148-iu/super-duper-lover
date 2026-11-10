package com.superduperlover.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.superduperlover.finance.ui.FinanceApp
import com.superduperlover.finance.ui.setChiLiContent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setChiLiContent { FinanceApp() }
    }

    override fun onStart() {
        super.onStart()
        // No background job: the jar periods that came due while the app was closed are reserved now.
        lifecycleScope.launch { (application as FinanceApplication).container.repo.materializeDueJarEntries() }
    }
}
