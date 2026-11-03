package id.mts.rth.core.export

import id.mts.rth.domain.model.StudentGradeSummary

data class CsvHeaderMetadata(
    val madrasahName: String = "MTs Roudhotut Thowalib",
    val namaMapel: String,
    val namaKelas: String,
    val tahunAjaran: String,
    val semester: String,
    val guruName: String,
    val kkm: Int
)

data class CsvStudentRowData(
    val summary: StudentGradeSummary,
    val nisn: String?,
    val nisLokal: String,
    val avgPraktik: Float? = null,
    val avgProyek: Float? = null,
    val avgPortofolio: Float? = null
)

/**
 * Utilitas untuk menghasilkan berkas CSV rekapitulasi nilai K13
 * berstandar RFC 4180 untuk ekspor darurat atau pelaporan madrasah.
 */
object CsvExporter {

    fun generateK13GradesCsv(
        metadata: CsvHeaderMetadata,
        rows: List<CsvStudentRowData>
    ): String {
        val sb = StringBuilder()

        // 1. Informasi Header Madrasah
        sb.append(escapeCsv("REKAPITULASI NILAI KURIKULUM 2013 (K13)")).append("\n")
        sb.append(escapeCsv(metadata.madrasahName)).append("\n")
        sb.append(escapeCsv("Mata Pelajaran: ${metadata.namaMapel}")).append("\n")
        sb.append(escapeCsv("Kelas: ${metadata.namaKelas}")).append("\n")
        sb.append(escapeCsv("Tahun Ajaran: ${metadata.tahunAjaran} Semester: ${metadata.semester}")).append("\n")
        sb.append(escapeCsv("Guru Pengampu: ${metadata.guruName}")).append("\n")
        sb.append(escapeCsv("KKM: ${metadata.kkm}")).append("\n")
        sb.append("\n")

        // 2. Baris Kolom Tabel
        val headers = listOf(
            "No",
            "NIS",
            "NISN",
            "Nama Siswa",
            "Rata-rata Tugas",
            "Rata-rata UH",
            "PAS/PAT",
            "NA Pengetahuan (KI-3)",
            "Predikat KI-3",
            "Status KI-3",
            "Rata-rata Praktik",
            "Rata-rata Proyek",
            "Rata-rata Portofolio",
            "NA Keterampilan (KI-4)",
            "Predikat KI-4",
            "Status KI-4"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

        // 3. Baris Data per Siswa
        for (row in rows) {
            val s = row.summary
            val line = listOf(
                s.nomorAbsen.toString(),
                row.nisLokal,
                row.nisn ?: "-",
                s.namaLengkap,
                formatFloat(s.rataTugas),
                formatFloat(s.rataUh),
                formatFloat(s.pasScore),
                s.naPengetahuan.toString(),
                s.predikatKi3.toString(),
                if (s.isRemedialKi3) "Remedial" else "Tuntas",
                formatFloat(row.avgPraktik),
                formatFloat(row.avgProyek),
                formatFloat(row.avgPortofolio),
                s.naKeterampilan.toString(),
                s.predikatKi4.toString(),
                if (s.isRemedialKi4) "Remedial" else "Tuntas"
            )
            sb.append(line.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        return sb.toString()
    }

    private fun formatFloat(value: Float?): String {
        if (value == null) return "-"
        return String.format(java.util.Locale.US, "%.1f", value)
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        if (!containsSpecial) {
            return value
        }
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
