package id.mts.rth.data.local.db

import androidx.room.migration.Migration

object DbMigrations {
    /**
     * Daftar migrasi database antar versi skema.
     * Saat rilis awal versi 1, array ini kosong.
     * Setiap perubahan skema di versi mendatang wajib ditambahkan ke sini
     * agar data nilai dan presensi offline guru tidak hilang.
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf()
}
