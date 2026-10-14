package com.lyltc1.lindachu.di

import android.content.Context
import androidx.room.Room
import com.lyltc1.lindachu.data.backup.BackupManager
import com.lyltc1.lindachu.data.local.AppDatabase
import com.lyltc1.lindachu.data.local.ImageStorage
import com.lyltc1.lindachu.data.menu.MenuPublisher
import com.lyltc1.lindachu.data.prefs.SyncPreferences
import com.lyltc1.lindachu.data.repository.DishRepository
import com.lyltc1.lindachu.data.repository.MenuRepository
import com.lyltc1.lindachu.data.sync.SyncManager

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
    ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    val dishRepository: DishRepository = DishRepository(database)

    val imageStorage: ImageStorage = ImageStorage(appContext)

    val syncPreferences: SyncPreferences = SyncPreferences(appContext)

    val syncManager: SyncManager = SyncManager(database, imageStorage, syncPreferences)

    val backupManager: BackupManager = BackupManager(appContext, imageStorage)

    val menuRepository: MenuRepository = MenuRepository(database)

    val menuPublisher: MenuPublisher = MenuPublisher(appContext, menuRepository, syncPreferences)

    companion object {
        const val DB_NAME = "lindachu.db"
    }
}
