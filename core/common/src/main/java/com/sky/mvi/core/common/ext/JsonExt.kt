package com.sky.mvi.core.common.ext

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Json 序列化 / 反序列化扩展，基于 Moshi。
 */
inline fun <reified T> T.toJson(): String =
    Moshi.Builder().build().adapter(T::class.java).toJson(this)

inline fun <reified T> String.fromJson(): T? =
    Moshi.Builder().build().adapter<T>(object : TypeToken<T>() {}.type).fromJson(this)

inline fun <reified T> String.jsonToList() = Moshi.Builder().build().adapter<List<T>>(
    Types.newParameterizedType(
        List::class.java,
        T::class.java
    )
).fromJson(this)

inline fun <reified K, reified V> String.jsonToMap() = Moshi.Builder().build().adapter<Map<K, V>>(
    Types.newParameterizedType(
        Map::class.java,
        K::class.java,
        V::class.java
    )
).fromJson(this)

abstract class TypeToken<T> {
    val type: Type = run {
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
    }
}
