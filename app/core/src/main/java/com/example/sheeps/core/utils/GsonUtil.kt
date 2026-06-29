package com.example.sheeps.core.utils

import com.google.gson.Gson
import java.lang.reflect.Type

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
