package com.lyltc1.lindachu.data.sync

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** 同步过程中的已知错误，便于 UI 直接展示原因 */
class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * GitHub Contents API 的最小封装，只覆盖同步所需的读与写。
 *
 * 图片同样走这个接口的 base64 通道：压缩后单张通常小于 500KB，
 * 编码后约 670KB，远低于 Contents API 的 100MB 上限，
 * 换来的是"文本与图片用同一套鉴权和冲突处理"的实现简化。
 */
class GitHubContentClient(
    private val token: String,
    val owner: String,
    val repo: String
) {

    data class RemoteFile(val content: String, val sha: String?)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun url(path: String) = "https://api.github.com/repos/$owner/$repo/contents/$path"

    private fun requestBuilder(path: String): Request.Builder = Request.Builder()
        .url(url(path))
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")

    /** 读取文本；远端不存在返回 null，其他错误抛出 SyncException */
    suspend fun read(path: String): RemoteFile? = withContext(Dispatchers.IO) {
        client.newCall(requestBuilder(path).get().build()).execute().use { response ->
            if (response.code == 404) return@withContext null
            val body = response.body?.string().orEmpty()
            if (response.code !in 200..299) {
                throw SyncException("读取 $path 失败（${response.code}）：${errorMessage(body)}")
            }
            val json = JSONObject(body)
            RemoteFile(
                content = String(Base64.decode(json.optString("content"), Base64.DEFAULT)),
                sha = json.optString("sha").takeIf { it.isNotBlank() }
            )
        }
    }

    /** 判断远端文件是否已存在。用 HEAD 请求，避免为了查 sha 下载整个文件 */
    suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        client.newCall(requestBuilder(path).head().build()).execute().use { it.code == 200 }
    }

    /** 读取二进制；远端不存在返回 null */
    suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        client.newCall(requestBuilder(path).get().build()).execute().use { response ->
            if (response.code == 404) return@withContext null
            val body = response.body?.string().orEmpty()
            if (response.code !in 200..299) {
                throw SyncException("读取 $path 失败（${response.code}）：${errorMessage(body)}")
            }
            Base64.decode(JSONObject(body).optString("content"), Base64.DEFAULT)
        }
    }

    /** 写入文本，返回新文件的 sha；已存在的文件必须带 existingSha */
    suspend fun writeText(
        path: String,
        content: String,
        message: String,
        existingSha: String? = null
    ): String = write(path, content.toByteArray(), message, existingSha, isText = true)

    /** 写入二进制（图片），返回新文件的 sha */
    suspend fun writeBytes(
        path: String,
        bytes: ByteArray,
        message: String,
        existingSha: String? = null
    ): String = write(path, bytes, message, existingSha, isText = false)

    private suspend fun write(
        path: String,
        bytes: ByteArray,
        message: String,
        existingSha: String?,
        isText: Boolean
    ): String = withContext(Dispatchers.IO) {
        val encoded = if (isText) {
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } else {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
        val payload = JSONObject().apply {
            put("message", message)
            put("content", encoded)
            existingSha?.let { put("sha", it) }
        }.toString()

        client.newCall(
            requestBuilder(path).put(payload.toRequestBody(JSON_MEDIA)).build()
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.code !in 200..299) {
                throw SyncException("写入 $path 失败（${response.code}）：${errorMessage(body)}")
            }
            JSONObject(body).optJSONObject("content")?.optString("sha").orEmpty()
        }
    }

    private fun errorMessage(body: String): String =
        runCatching { JSONObject(body).optString("message") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: body.take(200)

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
