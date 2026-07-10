package com.example.sheeps.core.cache

import com.example.sheeps.data.model.ShopItem
import com.tencent.mmkv.MMKV
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 商城商品本地缓存层（多语言动态下发 + 用户无感刷新）。
 *
 * 职责：
 *  - 将服务端 /api/shop/items 返回的（已按请求语言本地化的）[ShopItem] 列表序列化落盘到 MMKV，
 *    复用项目既有的 [MMKV] 单例（[kv]），不新建偏好系统；保证离线 / 首启也有内容可显示。
 *  - 每次拉取后与本地缓存做**内容比对**（SHA-256 指纹，顺序无关），仅当内容变化时回写，
 *    调用方据此决定是否刷新 UI；无变化则跳过，用户完全无感、零无效刷新与零无效流量。
 *
 * 线程安全：MMKV 自身支持多进程 / 多线程读写；本类无额外可变状态，可安全并发调用。
 *
 * @param kv 项目复用 MMKV 单例（由 [com.example.sheeps.core.di.StorageModule] 提供）
 * @param json 项目复用 Json 单例（容忍未知字段）
 */
@Singleton
class ShopCache @Inject constructor(
    private val kv: MMKV,
    private val json: Json
) {
    companion object {
        private const val KEY_ITEMS = "shop_cache_items_v1"
        private const val KEY_HASH = "shop_cache_items_hash_v1"
    }

    /**
     * 读取上次缓存快照（可能为空）。解析失败返回空列表，由调用方回退。
     */
    fun getCachedItems(): List<ShopItem> {
        val raw = kv.decodeString(KEY_ITEMS) ?: return emptyList()
        return try {
            json.decodeFromString<List<ShopItem>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 当前缓存内容指纹（无缓存返回空串），用于快速比对。
     */
    fun getCachedHash(): String = kv.decodeString(KEY_HASH) ?: ""

    /**
     * 比对新列表与缓存：
     *  - 内容无变化 -> 不写盘，返回 false（调用方跳过 UI 刷新）；
     *  - 内容有变化 -> 写盘并返回 true（调用方据此刷新 UI）。
     *
     * @param items 服务端返回的最新商品列表
     * @return true 表示缓存已更新（内容有变化）
     */
    fun saveIfChanged(items: List<ShopItem>): Boolean {
        val hash = computeHash(items)
        if (hash == getCachedHash()) {
            return false
        }
        val raw = try {
            json.encodeToString<List<ShopItem>>(items)
        } catch (_: Exception) {
            return false
        }
        kv.encode(KEY_ITEMS, raw)
        kv.encode(KEY_HASH, hash)
        return true
    }

    /**
     * 计算列表内容指纹：逐 item 取关键字段（id / name / description / image_url / item_type /
     * points_price / stock / group / tiles）拼接后 SHA-256；先按 id 排序，保证顺序无关。
     * 仅当这些字段任一变化时才认为内容变化，触发缓存刷新。
     */
    private fun computeHash(items: List<ShopItem>): String {
        val sb = StringBuilder()
        items.sortedBy { it.id }.forEach { item ->
            sb.append(item.id).append('|')
                .append(item.name).append('|')
                .append(item.description ?: "").append('|')
                .append(item.image_url ?: "").append('|')
                .append(item.item_type).append('|')
                .append(item.points_price).append('|')
                .append(item.stock).append('|')
                .append(item.group ?: "").append('|')
                .append(item.tiles?.joinToString(",") ?: "").append(';')
        }
        return sha256(sb.toString())
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
