package com.superduperlover.finance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.superduperlover.finance.R
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.quickadd.QuickAddActivity

/** Which transaction type each placed widget opens; chosen once when the widget is placed. */
object WidgetKinds {
    private const val PREFS = "chili_widgets"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context, appWidgetId: Int): TxnKind =
        prefs(context).getString("kind_$appWidgetId", null)
            ?.let { runCatching { TxnKind.valueOf(it) }.getOrNull() }
            ?: TxnKind.EXPENSE

    fun set(context: Context, appWidgetId: Int, kind: TxnKind) {
        prefs(context).edit().putString("kind_$appWidgetId", kind.name).apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove("kind_$appWidgetId").apply()
    }
}

/** One widget type; tapping it opens [QuickAddActivity] on the stored transaction type. No receiver hop on click. */
class QuickWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) update(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetKinds.remove(context, id)
    }

    companion object {
        fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val kind = WidgetKinds.get(context, appWidgetId)
            val intent = Intent(context, QuickAddActivity::class.java)
                .putExtra(QuickAddActivity.EXTRA_KIND, kind.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            // The widget id is the request code, so every widget has its own PendingIntent.
            val pending = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_quick).apply {
                setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    when (kind) {
                        TxnKind.EXPENSE -> R.drawable.widget_bg_expense
                        TxnKind.INCOME -> R.drawable.widget_bg_income
                        TxnKind.TRANSFER -> R.drawable.widget_bg_transfer
                    },
                )
                setTextViewText(
                    R.id.widget_label,
                    context.getString(
                        when (kind) {
                            TxnKind.EXPENSE -> R.string.kind_expense
                            TxnKind.INCOME -> R.string.kind_income
                            TxnKind.TRANSFER -> R.string.kind_transfer
                        },
                    ),
                )
                setOnClickPendingIntent(R.id.widget_root, pending)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
