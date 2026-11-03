package id.mts.rth.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.mts.rth.data.local.dao.AttendanceDao
import id.mts.rth.data.local.dao.GradingDao
import id.mts.rth.data.local.dao.MasterAcademicDao
import id.mts.rth.data.local.dao.StudentDao
import id.mts.rth.data.local.dao.SyncOutboxDao
import id.mts.rth.data.local.entity.AnggotaRombelEntity
import id.mts.rth.data.local.entity.GuruEntity
import id.mts.rth.data.local.entity.KelasEntity
import id.mts.rth.data.local.entity.KkmMapelEntity
import id.mts.rth.data.local.entity.KomponenNilaiEntity
import id.mts.rth.data.local.entity.MapelEntity
import id.mts.rth.data.local.entity.NilaiSiswaEntity
import id.mts.rth.data.local.entity.PenugasanGuruEntity
import id.mts.rth.data.local.entity.PresensiDetailEntity
import id.mts.rth.data.local.entity.PresensiSesiEntity
import id.mts.rth.data.local.entity.SiswaCoreEntity
import id.mts.rth.data.local.entity.SiswaDetailEntity
import id.mts.rth.data.local.entity.SyncOutboxEntity
import id.mts.rth.data.local.entity.TahunAjaranEntity

@Database(
    entities = [
        TahunAjaranEntity::class,
        GuruEntity::class,
        KelasEntity::class,
        MapelEntity::class,
        PenugasanGuruEntity::class,
        SiswaCoreEntity::class,
        SiswaDetailEntity::class,
        AnggotaRombelEntity::class,
        PresensiSesiEntity::class,
        PresensiDetailEntity::class,
        KkmMapelEntity::class,
        KomponenNilaiEntity::class,
        NilaiSiswaEntity::class,
        SyncOutboxEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun masterAcademicDao(): MasterAcademicDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun gradingDao(): GradingDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        const val DATABASE_NAME = "simrth_k13.db"

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(*DbMigrations.ALL_MIGRATIONS)
                .build()
        }
    }
}
