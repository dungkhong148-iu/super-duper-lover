package com.superduperlover.finance.quickadd

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.superduperlover.finance.MainActivity
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.ui.add.AddTransactionScreen
import com.superduperlover.finance.ui.setChiLiContent

/**
 * Opens straight on the add-transaction screen, for the home-screen widgets and the app shortcuts.
 * The transaction type comes from the [EXTRA_KIND] intent extra (a [TxnKind] name; default Chi tiêu).
 * Own task, not in recents; finishing returns to wherever the user came from.
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { TxnKind.valueOf(it) }.getOrNull() }
            ?: TxnKind.EXPENSE
        setChiLiContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .systemBarsPadding()
            ) {
                AddTransactionScreen(
                    txnId = 0L,
                    initialKind = kind,
                    onClose = { finish() },
                    onGoAccounts = {
                        // No account yet: the account tiles live on the main screen.
                        startActivity(Intent(this@QuickAddActivity, MainActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "com.superduperlover.finance.KIND"
    }
}
