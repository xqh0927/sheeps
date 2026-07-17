package com.example.sheeps.lib_network.network

import com.example.sheeps.lib_base.crypto.AesGcmCipher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject

/**
 * OkHttp 加密拦截器
 *
 * 实现请求体加密和响应体解密，与服务端 middleware.ts 配对使用。
 *
 * 拦截规则：
 * 1. 拦截所有 /api/ 请求
 * 2. 请求体 JSON → AesGcmCipher.encrypt → {"encrypted":"..."}
 * 3. 响应体 {"encrypted":"..."} → AesGcmCipher.decrypt → 原始 JSON
 * 4. 跳过 WebSocket 升级请求
 * 5. 非加密响应（无 encrypted 字段）→ 透传（兼容老服务端）
 */
class EncryptionInterceptor : Interceptor {

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url
        val path = url.encodedPath

        // 跳过非 API 请求
        if (!path.startsWith("/api/")) {
            return chain.proceed(originalRequest)
        }

        // 跳过 WebSocket 升级请求
        if ("websocket".equals(originalRequest.header("Upgrade"), ignoreCase = true)) {
            return chain.proceed(originalRequest)
        }

        // 跳过 GET/HEAD/OPTIONS 请求（无请求体）
        val method = originalRequest.method.uppercase()
        if (method == "GET" || method == "HEAD" || method == "OPTIONS") {
            return chain.proceed(originalRequest)
        }

        // ---- 加密请求体 ----
        var processedRequest = originalRequest
        val requestBody = originalRequest.body

        // 跳过 multipart 请求（如头像上传），二进制数据通过 HTTPS 传输层加密已足够
        val contentType = requestBody?.contentType()?.toString() ?: ""
        if (contentType.startsWith("multipart/")) {
            return chain.proceed(originalRequest)
        }

        if (requestBody != null) {
            try {
                // 读取原始请求体内容
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                val originalBodyString = buffer.readUtf8()

                if (originalBodyString.isNotBlank()) {
                    // 加密并包装为 {"encrypted":"..."}
                    val encrypted = AesGcmCipher.encrypt(originalBodyString)
                    val wrappedBody = JSONObject().apply {
                        put("encrypted", encrypted)
                    }.toString()

                    processedRequest = originalRequest.newBuilder()
                        .method(originalRequest.method, wrappedBody.toRequestBody(JSON_MEDIA_TYPE))
                        .header("Content-Type", "application/json")
                        .build()
                }
            } catch (e: Exception) {
                // 加密失败时使用原始请求（服务端会因格式错误而返回错误）
                e.printStackTrace()
            }
        }

        // 执行请求
        val response = chain.proceed(processedRequest)

        // ---- 解密响应体 ----
        val responseBody = response.body
        if (responseBody != null) {
            var responseBodyString: String? = null
            try {
                responseBodyString = responseBody.string()
                if (responseBodyString.isNotBlank()) {
                    val jsonObject = try { JSONObject(responseBodyString) } catch (e: Exception) { null }

                    // 检查是否包含 encrypted 字段
                    if (jsonObject != null && jsonObject.has("encrypted") && !jsonObject.isNull("encrypted")) {
                        val encryptedData = jsonObject.getString("encrypted")
                        val decrypted = AesGcmCipher.decrypt(encryptedData)

                        // 返回解密后的响应
                        val newBody = decrypted.toResponseBody(JSON_MEDIA_TYPE)
                        return response.newBuilder()
                            .body(newBody)
                            .build()
                    } else {
                        // 无 encrypted 字段 → 用刚才读出来的 responseBodyString 重建 body 返回（防止流被关闭后下层报错）
                        val newBody = responseBodyString.toResponseBody(responseBody.contentType() ?: JSON_MEDIA_TYPE)
                        return response.newBuilder()
                            .body(newBody)
                            .build()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果已经成功读取到了 body 字符串，即使后续解析/解密出错，也必须用读到的内容重建 body，防止外部发生 closed 崩溃
                if (responseBodyString != null) {
                    try {
                        val newBody = responseBodyString.toResponseBody(responseBody.contentType() ?: JSON_MEDIA_TYPE)
                        return response.newBuilder()
                            .body(newBody)
                            .build()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        return response
    }
}
