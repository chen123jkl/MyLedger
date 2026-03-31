package com.example.myledger.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myledger.data.AppDatabase
import com.example.myledger.data.Transaction
import com.example.myledger.utils.CommonUtils
import com.example.myledger.utils.PaymentInfoExtractor
import com.example.myledger.utils.FileLogHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AutoRecordService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoRecord"
        private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
        private const val WECHAT_PACKAGE = "com.tencent.mm"

        // 🔥 终极去重：10秒内完全相同的记录只记一次
        private const val DUPLICATE_INTERVAL_MS = 10000L
        private var lastUniqueKey = ""
        private var lastKeyTime = 0L
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        val eventType = event.eventType

        FileLogHelper.log(this, "Event", "收到事件: type=$eventType, pkg=$packageName")

        if ((packageName == ALIPAY_PACKAGE || packageName == WECHAT_PACKAGE) &&
            (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) {

            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                handlePaymentPage(packageName)
            }, 300)
        }
    }

    private fun handlePaymentPage(packageName: String) {
        FileLogHelper.log(this, "Handle", "开始处理支付页面: $packageName")

        val root = rootInActiveWindow
        if (root == null) {
            FileLogHelper.log(this, "Error", "rootInActiveWindow 为空")
            return
        }

        if (isMyLedgerScreen(root)) {
            FileLogHelper.log(this, "Skip", "检测到记账本主界面，跳过")
            return
        }

        val amount = PaymentInfoExtractor.extractAmount(root)
        if (amount == null) {
            FileLogHelper.log(this, "Extract", "未提取到金额")
            return
        }

        val merchant = PaymentInfoExtractor.extractMerchant(root) ?: when (packageName) {
            ALIPAY_PACKAGE -> "支付宝支付"
            WECHAT_PACKAGE -> "微信支付"
            else -> "未知支付"
        }

        val goodsNote = PaymentInfoExtractor.extractGoodsNote(root)
        val isTransfer = PaymentInfoExtractor.isTransfer(root, merchant, goodsNote)
        val category = if (isTransfer) "转账" else "自动记账"
        val note = if (goodsNote.isNotEmpty()) "$merchant - $goodsNote" else merchant

        // ==============================================
        // 🔥 🔥 🔥 终极去重核心代码
        // ==============================================
        val now = System.currentTimeMillis()
        val uniqueKey = "${amount}_${merchant}_${goodsNote}"

        if (uniqueKey == lastUniqueKey && now - lastKeyTime < DUPLICATE_INTERVAL_MS) {
            FileLogHelper.log(this, "Duplicate", "✅ 去重成功，忽略重复记录")
            return
        }

        lastUniqueKey = uniqueKey
        lastKeyTime = now

        // 保存
        saveTransaction(amount, category, note)
    }

    private fun isMyLedgerScreen(root: AccessibilityNodeInfo): Boolean {
        return containsText(root, "本月总支出")
    }

    private fun containsText(node: AccessibilityNodeInfo, target: String): Boolean {
        if (node.text?.toString()?.contains(target) == true) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsText(child, target)) return true
        }
        return false
    }

    private fun saveTransaction(amount: Double, category: String, note: String) {
        FileLogHelper.log(this, "Save", "准备保存: amount=$amount, category=$category")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val transaction = Transaction(amount = amount, category = category, note = note)
                db.transactionDao().insert(transaction)
                FileLogHelper.log(this@AutoRecordService, "Save", "✅ 交易保存成功")

                // --------------------------
                // 必须切主线程弹通知！
                // --------------------------
                Handler(Looper.getMainLooper()).post {
                    CommonUtils.showPaymentSuccessNotification(
                        this@AutoRecordService,
                        amount,
                        note
                    )
                }

            } catch (e: Exception) {
                FileLogHelper.log(this@AutoRecordService, "Error", "保存失败: ${e.message}")
            }
        }
    }

    override fun onInterrupt() {}
    override fun onServiceConnected() {
        super.onServiceConnected()
        FileLogHelper.log(this, "Lifecycle", "✅ 无障碍服务已连接")
    }
}