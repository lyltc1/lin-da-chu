package com.lyltc1.lindachu.data.order

/**
 * 订单码编解码。
 *
 * 格式：`LC1|<menuId>|<tableNo>|<note>|<序号>:<份数>,...`
 *
 * 只带序号而不带菜名，是刻意的：二维码容量有限，中文按 UTF-8 编码要占 3 字节，
 * 而菜单本身是固定快照、菜品列表已经内嵌在页面里，用序号引用足矣。
 * 码越简单越好扫——朋友举着手机给你扫的那几秒，容错率就是体验。
 */
object OrderCodec {

    private const val PREFIX = "LC1"
    private const val SEP = "|"
    private const val ITEM_SEP = ","
    private const val PAIR_SEP = ":"
    private const val MAX_FIELD = 60

    data class Pick(val index: Int, val count: Int)

    data class Payload(
        val menuId: String,
        val tableNo: String,
        val note: String,
        val picks: List<Pick>
    )

    fun encode(payload: Payload): String {
        val items = payload.picks
            .filter { it.count > 0 }
            .joinToString(ITEM_SEP) { "${it.index}$PAIR_SEP${it.count}" }
        return listOf(PREFIX, payload.menuId, sanitize(payload.tableNo), sanitize(payload.note), items)
            .joinToString(SEP)
    }

    fun decode(raw: String): Payload? {
        val parts = raw.trim().split(SEP)
        if (parts.size != 5 || parts[0] != PREFIX) return null

        val menuId = parts[1].trim()
        if (menuId.isBlank()) return null

        val picks = parts[4].split(ITEM_SEP)
            .filter { it.isNotBlank() }
            .mapNotNull { token ->
                val pair = token.split(PAIR_SEP)
                val index = pair.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
                val count = pair.getOrNull(1)?.trim()?.toIntOrNull() ?: return@mapNotNull null
                Pick(index, count.coerceIn(1, 99))
            }
        if (picks.isEmpty()) return null

        return Payload(
            menuId = menuId,
            tableNo = parts[2].trim(),
            note = parts[3].trim(),
            picks = picks
        )
    }

    /** 去掉会破坏格式的分隔符，并限制长度避免二维码过大 */
    private fun sanitize(value: String): String = value
        .replace(SEP, " ")
        .replace(ITEM_SEP, " ")
        .replace(PAIR_SEP, " ")
        .replace("\n", " ")
        .trim()
        .take(MAX_FIELD)
}
