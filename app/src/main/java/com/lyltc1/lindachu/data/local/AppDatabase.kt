package com.lyltc1.lindachu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lyltc1.lindachu.data.local.dao.CategoryDao
import com.lyltc1.lindachu.data.local.dao.CookLogDao
import com.lyltc1.lindachu.data.local.dao.DishDao
import com.lyltc1.lindachu.data.local.dao.IngredientDao
import com.lyltc1.lindachu.data.local.dao.MenuDao
import com.lyltc1.lindachu.data.local.dao.OrderDao
import com.lyltc1.lindachu.data.local.dao.StepDao
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.local.entity.CookLogEntity
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.MenuEntity
import com.lyltc1.lindachu.data.local.entity.OrderEntity
import com.lyltc1.lindachu.data.local.entity.OrderItemEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity

@Database(
    entities = [
        DishEntity::class,
        CategoryEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        CookLogEntity::class,
        MenuEntity::class,
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun stepDao(): StepDao
    abstract fun cookLogDao(): CookLogDao
    abstract fun menuDao(): MenuDao
    abstract fun orderDao(): OrderDao

    companion object {
        /** v2：新增菜单与订单三张表，纯增量，不触碰既有菜谱数据 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `menu` (" +
                        "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `dishIds` TEXT NOT NULL, " +
                        "`greeting` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `orders` (" +
                        "`id` TEXT NOT NULL, `menuId` TEXT NOT NULL, `menuTitle` TEXT NOT NULL, " +
                        "`tableNo` TEXT NOT NULL, `note` TEXT NOT NULL, `status` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_item` (" +
                        "`id` TEXT NOT NULL, `orderId` TEXT NOT NULL, `dishId` TEXT NOT NULL, " +
                        "`dishTitle` TEXT NOT NULL, `count` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_orders_createdAt` ON `orders` (`createdAt`)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_status` ON `orders` (`status`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_order_item_orderId` ON `order_item` (`orderId`)"
                )
            }
        }

        /** v3：步骤新增小结字段，纯增量，已有步骤默认留空（只展示正文） */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `step` ADD COLUMN `summary` TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v4：菜谱新增评价字段，与"小贴士"分开，旧数据评价留空 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `dish` ADD COLUMN `review` TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v5：菜谱新增别名字段，让"炒包菜"这类叫法也能搜到"手撕莲白" */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `dish` ADD COLUMN `aliases` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v6：菜谱去掉难度与份量两列。
         *
         * 不少设备的 SQLite 还不支持 DROP COLUMN，所以走重建表的路子：
         * 建新表 → 搬运要保留的列 → 删旧表 → 改名 → 补回索引。
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dish_new` (" +
                        "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `coverPath` TEXT, " +
                        "`categoryId` TEXT, `tags` TEXT NOT NULL, `aliases` TEXT NOT NULL, " +
                        "`durationMin` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                        "`review` TEXT NOT NULL, `rating` INTEGER NOT NULL, " +
                        "`cookCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT INTO `dish_new` (" +
                        "`id`, `title`, `coverPath`, `categoryId`, `tags`, `aliases`, " +
                        "`durationMin`, `notes`, `review`, `rating`, `cookCount`, " +
                        "`createdAt`, `updatedAt`, `deletedAt`) " +
                        "SELECT " +
                        "`id`, `title`, `coverPath`, `categoryId`, `tags`, `aliases`, " +
                        "`durationMin`, `notes`, `review`, `rating`, `cookCount`, " +
                        "`createdAt`, `updatedAt`, `deletedAt` FROM `dish`"
                )
                db.execSQL("DROP TABLE `dish`")
                db.execSQL("ALTER TABLE `dish_new` RENAME TO `dish`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_dish_categoryId` ON `dish` (`categoryId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_dish_updatedAt` ON `dish` (`updatedAt`)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dish_title` ON `dish` (`title`)")
            }
        }
    }
}
