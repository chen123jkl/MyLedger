package com.example.myledger.utils

import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

object PaymentInfoExtractor {

    private val AMOUNT_KEYWORDS = listOf("消费金额", "支付金额", "实付", "金额")
    private val MERCHANT_KEYWORDS = listOf("商家", "收款方", "商户", "店铺", "收款")
    private val GOODS_KEYWORDS = listOf(
        "商品说明", "商品", "订单详情", "交易备注", "备注",
        "商品名称", "项目", "详情", "摘要", "产品", "收款备注"
    )
    private val TRANSFER_KEYWORDS = listOf("转账", "转给", "向", "转账给", "转款")

    fun extractAmount(root: AccessibilityNodeInfo): Double? {
        val amountIds = listOf(
            "com.eg.android.AlipayGphone:id/amount",
            "com.eg.android.AlipayGphone:id/pay_amount",
            "com.eg.android.AlipayGphone:id/tv_amount",
            "com.tencent.mm:id/amount_tv",
            "com.tencent.mm:id/pay_amount_tv",
            "com.tencent.mm:id/tv_price"
        )
        for (id in amountIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                val text = nodes[0].text?.toString()
                val amount = extractNumberFromText(text)
                if (amount != null) return amount
            }
        }

        val currencyNode = findNodeByTextPattern(root, Pattern.compile("[¥￥]\\s*\\d+\\.?\\d*"))
        if (currencyNode != null) {
            return extractNumberFromText(currencyNode.text?.toString())
        }

        for (kw in AMOUNT_KEYWORDS) {
            val node = findNodeByTextContains(root, kw)
            if (node != null) {
                return extractNumberFromText(node.text?.toString())
            }
        }
        return null
    }

    fun extractMerchant(root: AccessibilityNodeInfo): String? {
        for (kw in MERCHANT_KEYWORDS) {
            val node = findNodeByTextContains(root, kw)
            if (node != null) {
                var text = node.text?.toString() ?: ""
                text = text.replace(Regex("^$kw[:：]\\s*"), "").trim()
                if (text.isNotEmpty()) return text
                val sibling = getNextSiblingText(node)
                if (sibling.isNotEmpty()) return sibling
            }
        }
        return null
    }

    fun extractGoodsNote(root: AccessibilityNodeInfo): String {
        for (kw in GOODS_KEYWORDS) {
            val node = findNodeByTextContains(root, kw)
            if (node != null) {
                var text = node.text?.toString() ?: ""
                text = text.replace(Regex("^$kw[:：]\\s*"), "").trim()
                if (text.isNotEmpty()) return text
                val sibling = getNextSiblingText(node)
                if (sibling.isNotEmpty()) return sibling
            }
        }

        val noteIds = listOf(
            "com.eg.android.AlipayGphone:id/order_title",
            "com.eg.android.AlipayGphone:id/goods_name",
            "com.tencent.mm:id/remark_tv",
            "com.tencent.mm:id/order_desc"
        )
        for (id in noteIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                val text = nodes[0].text?.toString()
                if (!text.isNullOrBlank()) return text
            }
        }

        val heuristic = findGoodsNoteHeuristic(root)
        if (heuristic != null) return heuristic

        return ""
    }

    fun isTransfer(root: AccessibilityNodeInfo, merchant: String?, goodsNote: String): Boolean {
        if (goodsNote.isNotBlank()) return false
        if (merchant != null && TRANSFER_KEYWORDS.any { merchant.contains(it) }) return true
        return containsAnyKeyword(root, TRANSFER_KEYWORDS)
    }

    private fun findNodeByTextPattern(node: AccessibilityNodeInfo, pattern: Pattern): AccessibilityNodeInfo? {
        if (node.text != null && pattern.matcher(node.text).matches()) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByTextPattern(child, pattern)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByTextContains(node: AccessibilityNodeInfo, keyword: String): AccessibilityNodeInfo? {
        if (node.text?.contains(keyword, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findNodeByTextContains(child, keyword)
            if (res != null) return res
        }
        return null
    }

    private fun getNextSiblingText(node: AccessibilityNodeInfo): String {
        val p = node.parent ?: return ""
        val idx = p.findIndexOfChild(node)
        if (idx >= 0 && idx + 1 < p.childCount) {
            return p.getChild(idx + 1)?.text?.toString() ?: ""
        }
        return ""
    }

    private fun AccessibilityNodeInfo.findIndexOfChild(child: AccessibilityNodeInfo): Int {
        for (i in 0 until childCount) if (getChild(i) == child) return i
        return -1
    }

    private fun extractNumberFromText(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        return "\\d+\\.?\\d*".toRegex().find(text)?.value?.toDoubleOrNull()
    }

    private fun findGoodsNoteHeuristic(node: AccessibilityNodeInfo, maxDepth: Int = 8, depth: Int = 0): String? {
        if (depth > maxDepth) return null
        val t = node.text?.toString()?.trim() ?: ""
        val black = listOf("完成", "取消", "确定", "返回", "我的", "消息", "理财", "首页", "客服", "设置")
        if (t.length in 4..60
            && !t.matches(Regex("^\\d+.*$"))
            && !t.contains(Regex("[¥￥$]"))
            && !t.matches(Regex("^\\d{2}:\\d{2}"))
            && t !in black
            && !t.contains(Regex("商家|收款|优惠|红包|余额|方式|手续费|抵扣"))
        ) return t

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findGoodsNoteHeuristic(child, maxDepth, depth + 1)
            if (res != null) return res
        }
        return null
    }

    private fun containsAnyKeyword(root: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        fun s(n: AccessibilityNodeInfo): Boolean {
            val t = n.text?.toString() ?: ""
            if (keywords.any { t.contains(it) }) return true
            for (i in 0 until n.childCount) if (s(n.getChild(i) ?: continue)) return true
            return false
        }
        return s(root)
    }
}