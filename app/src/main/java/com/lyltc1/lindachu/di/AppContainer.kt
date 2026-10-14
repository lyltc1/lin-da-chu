package com.lyltc1.lindachu.di

import android.content.Context
import androidx.room.Room
import com.lyltc1.lindachu.data.local.AppDatabase
import com.lyltc1.lindachu.data.repository.DishRepository

/**
 * 手动依赖装配。
 *
 * 项目规模不大，暂时不引入 Hilt；后续如需注入更多依赖，
 * 只需把这里的构造过程换成 DI 模块的 @Provides。
 */
class AppContainer(appContext: Context) {

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        DB_NAME
    ).build()

    val dishRepository: DishRepository = DishRepository(database)

    companion object {
        const val DB_NAME = "lindachu.db"
    }
}
