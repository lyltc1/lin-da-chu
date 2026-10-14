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
    version = 2,
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
    }
}
