package com.pixellauncherforall.home.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pixellauncherforall.home.helpers.Converters
import com.pixellauncherforall.home.interfaces.AppLaunchersDao
import com.pixellauncherforall.home.interfaces.HiddenIconsDao
import com.pixellauncherforall.home.interfaces.HomeScreenGridItemsDao
import com.pixellauncherforall.home.models.AppLauncher
import com.pixellauncherforall.home.models.HiddenIcon
import com.pixellauncherforall.home.models.HomeScreenGridItem

@Database(
    entities = [AppLauncher::class, HomeScreenGridItem::class, HiddenIcon::class],
    version = 5
)
@TypeConverters(Converters::class)
abstract class AppsDatabase : RoomDatabase() {

    abstract fun AppLaunchersDao(): AppLaunchersDao

    abstract fun HomeScreenGridItemsDao(): HomeScreenGridItemsDao

    abstract fun HiddenIconsDao(): HiddenIconsDao

    companion object {
        private var db: AppsDatabase? = null

        fun getInstance(context: Context): AppsDatabase {
            if (db == null) {
                synchronized(AppsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(
                            context.applicationContext,
                            AppsDatabase::class.java,
                            "apps.db"
                        ).build()
                    }
                }
            }
            return db!!
        }
    }
}
