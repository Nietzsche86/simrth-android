package id.mts.rth.data.local

import id.mts.rth.data.local.entity.SyncOutboxEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit Test untuk memverifikasi logika antrean mutasi dengan Coalescing (Outbox Pattern).
 * Memastikan tidak ada duplikasi antrean untuk pasangan (tableNameTarget, recordId) yang sama,
 * serta urutan eksekusi mengutamakan Parent (priority 1) sebelum Child (priority 2).
 */
class OutboxCoalescingLogicTest {

    // Simulasi in-memory repository untuk menguji aturan bisnis Coalescing
    private class FakeOutboxStorage {
        private val storage = mutableMapOf<Pair<String, String>, SyncOutboxEntity>()

        fun recordMutationWithCoalescing(
            targetTable: String,
            recordId: String,
            priority: Int,
            action: String,
            jsonPayload: String,
            nowMillis: Long
        ) {
            val key = Pair(targetTable, recordId)
            val existing = storage[key]
            if (existing != null) {
                storage[key] = existing.copy(
                    actionType = action,
                    payloadJson = jsonPayload,
                    updatedAt = nowMillis
                )
            } else {
                storage[key] = SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    tableNameTarget = targetTable,
                    priorityOrder = priority,
                    recordId = recordId,
                    actionType = action,
                    payloadJson = jsonPayload,
                    createdAt = nowMillis,
                    updatedAt = nowMillis
                )
            }
        }

        fun getPendingOutbox(): List<SyncOutboxEntity> {
            return storage.values.sortedWith(
                compareBy<SyncOutboxEntity> { it.priorityOrder }.thenBy { it.createdAt }
            )
        }

        fun count(): Int = storage.size
    }

    @Test
    fun `test coalescing prevents duplicate queue items for same record`() {
        val outbox = FakeOutboxStorage()
        val studentId = "student-uuid-123"
        val initialTime = 1000L
        val updateTime = 2000L

        // Mutasi awal: input presensi Hadir
        outbox.recordMutationWithCoalescing(
            targetTable = "presensi_detail",
            recordId = studentId,
            priority = 2,
            action = "UPSERT",
            jsonPayload = """{"status": "H"}""",
            nowMillis = initialTime
        )

        assertEquals(1, outbox.count())
        val initialEntry = outbox.getPendingOutbox().first()
        assertEquals("""{"status": "H"}""", initialEntry.payloadJson)
        assertEquals(initialTime, initialEntry.createdAt)
        assertEquals(initialTime, initialEntry.updatedAt)

        // Guru mengoreksi presensi menjadi Sakit beberapa saat kemudian
        outbox.recordMutationWithCoalescing(
            targetTable = "presensi_detail",
            recordId = studentId,
            priority = 2,
            action = "UPSERT",
            jsonPayload = """{"status": "S"}""",
            nowMillis = updateTime
        )

        // Verifikasi: jumlah item tetap 1 (coalesced), tidak ada record duplikat
        assertEquals(1, outbox.count())
        val updatedEntry = outbox.getPendingOutbox().first()

        assertEquals(initialEntry.id, updatedEntry.id) // ID antrean tetap sama
        assertEquals("""{"status": "S"}""", updatedEntry.payloadJson) // Payload diperbarui
        assertEquals(initialTime, updatedEntry.createdAt) // Waktu antrean pertama dipertahankan
        assertEquals(updateTime, updatedEntry.updatedAt) // UpdatedAt diperbarui
    }

    @Test
    fun `test outbox queue prioritizes parent before child`() {
        val outbox = FakeOutboxStorage()

        // 1. Simpan Child (Detail Nilai) lebih dulu secara timestamp
        outbox.recordMutationWithCoalescing(
            targetTable = "nilai_siswa",
            recordId = "nilai-001",
            priority = 2, // Child
            action = "UPSERT",
            jsonPayload = """{"skor": 85.0}""",
            nowMillis = 1000L
        )

        // 2. Simpan Parent (Komponen Nilai) beberapa saat kemudian
        outbox.recordMutationWithCoalescing(
            targetTable = "komponen_nilai",
            recordId = "komponen-001",
            priority = 1, // Parent
            action = "UPSERT",
            jsonPayload = """{"judul": "Tugas 1"}""",
            nowMillis = 1500L
        )

        val pending = outbox.getPendingOutbox()
        assertEquals(2, pending.size)

        // Verifikasi: Komponen (Parent, priority 1) harus selalu berada di urutan pertama
        // meskipun dibuat setelah Child (priority 2)
        assertEquals("komponen_nilai", pending[0].tableNameTarget)
        assertEquals(1, pending[0].priorityOrder)

        assertEquals("nilai_siswa", pending[1].tableNameTarget)
        assertEquals(2, pending[1].priorityOrder)
    }

    @Test
    fun `test delete action coalescing updates actionType`() {
        val outbox = FakeOutboxStorage()
        val recordId = "sesi-999"

        outbox.recordMutationWithCoalescing(
            targetTable = "presensi_sesi",
            recordId = recordId,
            priority = 1,
            action = "UPSERT",
            jsonPayload = """{"jamKe": "1-2"}""",
            nowMillis = 1000L
        )

        // Kemudian sesi dihapus
        outbox.recordMutationWithCoalescing(
            targetTable = "presensi_sesi",
            recordId = recordId,
            priority = 1,
            action = "DELETE",
            jsonPayload = """{"isDeleted": true}""",
            nowMillis = 1200L
        )

        assertEquals(1, outbox.count())
        val item = outbox.getPendingOutbox().first()
        assertEquals("DELETE", item.actionType)
        assertEquals("""{"isDeleted": true}""", item.payloadJson)
    }
}
