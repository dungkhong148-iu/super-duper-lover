package com.superduperlover.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.superduperlover.finance.ui.FinanceApp
import com.superduperlover.finance.ui.setChiLiContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setChiLiContent { FinanceApp() }
    }
}
