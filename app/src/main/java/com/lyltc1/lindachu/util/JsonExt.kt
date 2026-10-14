package com.lyltc1.lindachu.util

import org.json.JSONArray
import org.json.JSONObject

/**
 * 步骤图片等轻量列表以 JSON 数组字符串存库，避免为单一字段引入序列化框架。
 * 解析失败一律退化为空列表，不让脏数据拖垮界面。
 */
fun parseStringList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
            repeat(array.length()) { index -> add(array.getString(index)) }
        }
    }.getOrDefault(emptyList())
}

fun encodeStringList(values: List<String>): String = JSONArray().apply {
    values.forEach { put(it) }
}.toString()

/** 把任意结构化数据编码为紧凑 JSON（用于同步时导出附加信息） */
inline fun <reified T> encodeJson(value: T): String = JSONObject().apply {
    put("data", JSONObject.wrap(value))
}.toString()
