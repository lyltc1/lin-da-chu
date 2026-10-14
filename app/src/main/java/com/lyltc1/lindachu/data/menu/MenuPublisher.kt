package com.lyltc1.lindachu.data.menu

import android.content.Context
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.MenuEntity
import com.lyltc1.lindachu.data.prefs.SyncPreferences
import com.lyltc1.lindachu.data.repository.MenuRepository
import com.lyltc1.lindachu.data.sync.GitHubContentClient
import org.json.JSONArray
import org.json.JSONObject

/**
 * 把菜单发布成静态页，推到仓库的 `menus/<menuId>/index.html`。
 *
 * 页面自包含——菜谱数据以 JSON 内嵌在 `<script>` 里，因此 GitHub Pages
 * 拿到就能渲染，不需要任何构建步骤或后端服务；路径用目录形式而非
 * `xxx.html`，扫出来的 URL 也更干净。
 */
class MenuPublisher(
    private val context: Context,
    private val menuRepository: MenuRepository,
    private val preferences: SyncPreferences
) {

    /** 发布成功返回可访问的页面地址 */
    suspend fun publish(menu: MenuEntity): Result<String> = runCatching {
        val token = preferences.getToken()
        if (token.isBlank()) error("尚未设置 GitHub 令牌，请先到设置页填写")

        val dishes = menuRepository.getMenuDishes(menu)
        if (dishes.isEmpty()) error("菜单里还没有可用的菜")

        val client = GitHubContentClient(token, preferences.getOwner(), preferences.getRepo())
        val path = "menus/${menu.id}/index.html"
        val existingSha = client.read(path)?.sha
        client.writeText(
            path = path,
            content = buildHtml(menu, dishes),
            message = "chore: 发布菜单 ${menu.title}",
            existingSha = existingSha
        )

        pageUrl(client.owner, client.repo, menu.id)
    }

    private fun buildHtml(menu: MenuEntity, dishes: List<DishEntity>): String {
        val template = context.assets.open(TEMPLATE).bufferedReader().use { it.readText() }
        // 转义 </ 防止菜名或备注意外闭合 script 标签
        val data = buildDataJson(menu, dishes).replace("</", "<\\/")
        return template.replace(PLACEHOLDER_DATA, data)
    }

    private fun buildDataJson(menu: MenuEntity, dishes: List<DishEntity>): String {
        val items = JSONArray().apply {
            dishes.forEach { dish ->
                put(
                    JSONObject()
                        .put("id", dish.id)
                        .put("title", dish.title)
                        .put("cover", dish.coverPath.orEmpty())
                        .put("tags", dish.tags)
                        .put("difficulty", dish.difficulty)
                        .put("durationMin", dish.durationMin)
                        .put("servings", dish.servings)
                        .put("desc", dish.notes)
                )
            }
        }
        return JSONObject()
            .put("menuId", menu.id)
            .put("title", menu.title)
            .put("greeting", menu.greeting)
            .put("dishes", items)
            .toString(2)
    }

    companion object {
        private const val TEMPLATE = "menu_template.html"
        private const val PLACEHOLDER_DATA = "__MENU_DATA__"

        fun pageUrl(owner: String, repo: String, menuId: String): String =
            "https://$owner.github.io/$repo/menus/$menuId/"
    }
}
