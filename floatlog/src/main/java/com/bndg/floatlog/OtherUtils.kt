package com.bndg.floatlog

import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils

/**
 * @author r
 * @date 2025/1/20
 * @description Brief description of the file content.
 */
object OtherUtils {
    fun formatJson(json: String?): String {
        if (json == null) {
            return ""
        }
        // 计数tab的个数
        var tabNum = 0
        val builder = StringBuilder()
        val length = json.length

        var last = 0.toChar()
        for (i in 0 until length) {
            val c = json[i]
            if (c == '{') {
                tabNum++
                builder.append(c).append("\n")
                    .append(getSpaceOrTab(tabNum))
            } else if (c == '}') {
                tabNum--
                builder.append("\n")
                    .append(getSpaceOrTab(tabNum))
                    .append(c)
            } else if (c == ',') {
                // 是否格式化处理
                var formatFlag = true
                // 获取冒号最后所在位置
                val colonIndex = json.lastIndexOf(":", i)
                // 再获取引号最后所在位置
                val quoteIndex = json.lastIndexOf(":\"", i)
                if (colonIndex != -1) {
                    if (quoteIndex == colonIndex) {
                        // {"code":"12.0101.0122651.00,300.200000,210428,,,,,,10002,,01"}
                        if (json[i - 1] != '"') {
                            formatFlag = false
                        }
                    }
                }

                if (formatFlag) {
                    builder.append(c).append("\n")
                        .append(getSpaceOrTab(tabNum))
                } else {
                    builder.append(c)
                }
            } else if (c == ':') {
                if (i > 0 && json[i - 1] == '"') {
                    builder.append(" ").append(c).append(" ")
                } else {
                    builder.append(c)
                }
            } else if (c == '[') {
                tabNum++
                val next = json[i + 1]
                if (next == ']') {
                    builder.append(c)
                } else {
                    builder.append(c).append("\n")
                        .append(getSpaceOrTab(tabNum))
                }
            } else if (c == ']') {
                tabNum--
                if (last == '[') {
                    builder.append(c)
                } else {
                    builder.append("\n")
                        .append(getSpaceOrTab(tabNum)).append(c)
                }
            } else {
                builder.append(c)
            }
            last = c
        }

        return builder.toString()
    }

    /**
     * 创建对应数量的制表符
     */
    fun getSpaceOrTab(tabNum: Int): String {
        val builder = java.lang.StringBuilder()
        for (i in 0 until tabNum) {
            builder.append('\t')
        }
        return builder.toString()
    }

    fun copyTextToBoard(context: Context, string: String?) {
        if (TextUtils.isEmpty(string)) return
        val clip = context
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.text = string
    }
}