package com.example.myledger.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object FileLogHelper {
    private const val LOG_FILE_NAME = "auto_record_log.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun log(context: Context, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "$timestamp [$tag] $message\n"

        ioScope.launch {
            try {
                appendToDownloadLog(context, logLine)
                Log.d("FileLogHelper", logLine.trim())
            } catch (e: Exception) {
                Log.e("FileLogHelper", "写入日志失败", e)
            }
        }
    }

    private fun appendToDownloadLog(context: Context, logLine: String) {
        val resolver = context.contentResolver

        val existingUri = queryExistingFile(context)
        if (existingUri != null) {
            // 文件已存在：读取原有内容，追加新行，覆盖写回
            val existingContent = readAllTextFromUri(context, existingUri)
            val newContent = existingContent + logLine
            resolver.openOutputStream(existingUri, "w")?.use { outputStream ->
                outputStream.write(newContent.toByteArray())
            }
        } else {
            // 文件不存在：创建新文件
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, LOG_FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val newUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            newUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(logLine.toByteArray())
                }
            }
        }
    }

    private fun queryExistingFile(context: Context): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(LOG_FILE_NAME)

        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return Uri.withAppendedPath(collection, id.toString())
            }
        }
        return null
    }

    private fun readAllTextFromUri(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append('\n')
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    // 可选：清空日志文件
    fun clearLog(context: Context) {
        ioScope.launch {
            try {
                val existingUri = queryExistingFile(context)
                existingUri?.let { uri ->
                    context.contentResolver.delete(uri, null, null)
                }
                log(context, "System", "日志已清空")
            } catch (e: Exception) {
                Log.e("FileLogHelper", "清空日志失败", e)
            }
        }
    }
}