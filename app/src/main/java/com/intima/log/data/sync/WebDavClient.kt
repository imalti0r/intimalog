package com.intima.log.data.sync

import com.intima.log.data.repo.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 最小 WebDAV 客户端：PUT 上传 / GET 下载 / MKCOL 建目录 / PROPFIND 探测。
 * 仅够同步单个 .intima 备份文件使用，不做通用 WebDAV。
 */
class WebDavClient(private val config: ProfileRepository.WebDavConfig) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** 远端文件完整 URL：<url>/<dir>/<fileName> */
    private fun fileUrl(fileName: String): String =
        "${config.url.trimEnd('/')}/${config.dir.trim('/')}/$fileName"

    private fun basicAuth(): String {
        val cred = "${config.user}:${config.password}"
        return "Basic " + android.util.Base64.encodeToString(cred.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
    }

    private fun base(builder: Request.Builder.() -> Unit): Request.Builder =
        Request.Builder().header("Authorization", basicAuth()).apply(builder)

    sealed class Result {
        data class Ok(val bytes: ByteArray? = null, val message: String = "") : Result()
        data class Fail(val code: Int, val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    /** 确保远端目录存在（MKCOL，405=已存在视为成功） */
    suspend fun ensureDir(): Result = withContext(Dispatchers.IO) {
        try {
            val dirUrl = "${config.url.trimEnd('/')}/${config.dir.trim('/')}"
            val req = base { url(dirUrl).mkcol() }.build()
            http.newCall(req).execute().use { resp ->
                if (resp.code in setOf(201, 405)) Result.Ok(message = "dir ready")
                else Result.Fail(resp.code, "建目录失败：HTTP ${resp.code}")
            }
        } catch (e: Exception) {
            Result.Error("目录创建失败：${e.message}")
        }
    }

    /** 上传文件（覆盖写） */
    suspend fun upload(fileName: String, bytes: ByteArray): Result = withContext(Dispatchers.IO) {
        try {
            ensureDir().let { if (it is Result.Fail || it is Result.Error) return@withContext it }
            val body = bytes.toRequestBody("application/octet-stream".toMediaType())
            val req = base { url(fileUrl(fileName)).put(body) }.build()
            http.newCall(req).execute().use { resp ->
                if (resp.code in 200..299) Result.Ok(message = "已上传 $fileName")
                else Result.Fail(resp.code, "上传失败：HTTP ${resp.code}")
            }
        } catch (e: Exception) {
            Result.Error("上传失败：${e.message}")
        }
    }

    /** 下载文件；404 返回 Fail(404) 由调用方区分「远端无备份」 */
    suspend fun download(fileName: String): Result = withContext(Dispatchers.IO) {
        try {
            val req = base { url(fileUrl(fileName)).get() }.build()
            http.newCall(req).execute().use { resp ->
                when {
                    resp.code == 404 -> Result.Fail(404, "远端不存在 $fileName")
                    resp.code in 200..299 -> Result.Ok(bytes = resp.body?.bytes())
                    else -> Result.Fail(resp.code, "下载失败：HTTP ${resp.code}")
                }
            }
        } catch (e: Exception) {
            Result.Error("下载失败：${e.message}")
        }
    }

    /** 探测远端文件信息（Last-Modified / 大小），用于显示与冲突判断 */
    suspend fun stat(fileName: String): Result = withContext(Dispatchers.IO) {
        try {
            val req = base { url(fileUrl(fileName)).head() }.build()
            http.newCall(req).execute().use { resp ->
                when {
                    resp.code == 404 -> Result.Fail(404, "远端不存在 $fileName")
                    resp.code in 200..299 -> {
                        val lm = resp.header("Last-Modified") ?: "未知时间"
                        val size = resp.header("Content-Length")?.toLongOrNull()?.let { "${it / 1024} KB" } ?: "?"
                        Result.Ok(message = "$lm · $size")
                    }
                    else -> Result.Fail(resp.code, "探测失败：HTTP ${resp.code}")
                }
            }
        } catch (e: Exception) {
            Result.Error("探测失败：${e.message}")
        }
    }

    private fun Request.Builder.mkcol() = method("MKCOL", null)
}
