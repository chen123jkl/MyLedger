package com.example.myledger.utils

import android.content.Context
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myledger.R
import java.text.SimpleDateFormat
import java.util.*

object CommonUtils {

    data class CategoryStyle(
        val iconRes: Int,
        val colorRes: Int
    )

    fun getCategoryStyle(category: String): CategoryStyle {
        return when {
            category.contains("餐饮", true) -> CategoryStyle(R.drawable.ic_food, R.color.color_food)
            category.contains("交通", true) -> CategoryStyle(R.drawable.ic_transport, R.color.color_transport)
            category.contains("购物", true) -> CategoryStyle(R.drawable.ic_shop, R.color.color_shop)
            category.contains("娱乐", true) -> CategoryStyle(R.drawable.ic_game, R.color.color_game)
            category.contains("转账", true) -> CategoryStyle(R.drawable.ic_transfer, R.color.color_transfer)
            else -> CategoryStyle(R.drawable.ic_other, R.color.color_other)
        }
    }

    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance()
        yesterdayCal.add(Calendar.DAY_OF_MONTH, -1)

        val isToday = todayCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                && todayCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

        val isYesterday = yesterdayCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                && yesterdayCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

        val isThisWeek = (now - timestamp) < 7 * 24 * 60 * 60 * 1000L

        return when {
            isToday -> "今天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            isYesterday -> "昨天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            isThisWeek -> SimpleDateFormat("EEEE HH:mm", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun showPaymentSuccessNotification(context: Context, amount: Double, note: String) {
        val channelId = "payment_success_channel"
        val channelName = "支付成功通知"
        val notificationId = 1001

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 8.0+ 必须创建渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = nm.getNotificationChannel(channelId)
            if (channel == null) {
                channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH  // 👈 高优先级！必弹
                )
                channel.enableVibration(true)
                channel.setShowBadge(true)
                nm.createNotificationChannel(channel)
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✅ 已自动记账")
            .setContentText("¥$amount - $note")
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // 👈 高优先级
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)

        nm.notify(notificationId, builder.build())
    }

    fun getTodayTop10(transactions: List<com.example.myledger.data.Transaction>): List<com.example.myledger.data.Transaction> {
        val today = Calendar.getInstance()
        return transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            today.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                    && today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        }.sortedByDescending { it.amount }.take(10)
    }

    fun getMonthTop10(transactions: List<com.example.myledger.data.Transaction>): List<com.example.myledger.data.Transaction> {
        val today = Calendar.getInstance()
        return transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            today.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                    && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        }.sortedByDescending { it.amount }.take(10)
    }
}