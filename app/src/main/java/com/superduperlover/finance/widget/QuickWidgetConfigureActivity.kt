package com.superduperlover.finance.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.superduperlover.finance.R
import com.superduperlover.finance.core.TxnKind

/** Shown when a quick-add widget is placed: pick Chi tiêu / Thu nhập / Chuyển khoản. Plain views, no Compose. */
class QuickWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Back or a touch outside = cancel: the system drops the widget.
        setResult(RESULT_CANCELED)
        setTitle(R.string.widget_pick_title)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContentView(R.layout.activity_widget_config)
        findViewById<View>(R.id.pick_expense).setOnClickListener { pick(TxnKind.EXPENSE) }
        findViewById<View>(R.id.pick_income).setOnClickListener { pick(TxnKind.INCOME) }
        findViewById<View>(R.id.pick_transfer).setOnClickListener { pick(TxnKind.TRANSFER) }
    }

    private fun pick(kind: TxnKind) {
        WidgetKinds.set(this, appWidgetId, kind)
        QuickWidgetProvider.update(this, AppWidgetManager.getInstance(this), appWidgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}
