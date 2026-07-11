package com.example.sheeps.core.utils

import com.google.gson.Gson
import java.lang.reflect.Type

/**
 * Gson JSON 序列化工具（进程级单例）。
 *
 * 封装 [com.google.gson.Gson] 实例，提供 [toJson] 与两个重载的 [fromJson]。
 * Gson 实例本身线程安全，可并发调用。
 */
object GsonUtil {
    val gson = Gson()

    fun <T> toJson(src: T): String {
        return gson.toJson(src)
    }

    fun <T> fromJson(json: String, classOfT: Class<T>): T {
        return gson.fromJson(json, classOfT)
    }

    fun <T> fromJson(json: String, typeOfT: Type): T {
        return gson.fromJson(json, typeOfT)
    }
}
