/**
 * SIMRTH PocketBase Sync & Academic Server Engine
 * Madrasah Roudhotut Thowalib (LAN / Wi-Fi Sync)
 */

// =========================================================================
// 1. BOOTSTRAP: SKEMA TABEL & SEED DATA AWAL
// =========================================================================

onBootstrap((e) => {
    e.next();

    // 1.1 Inisialisasi Koleksi Resmi PocketBase agar Tampil di Dashboard Web (/_/)
    function ensureCollection(name, fields) {
        try {
            return $app.findCollectionByNameOrId(name);
        } catch (_) {
            const col = new Collection({
                type: "base",
                name: name,
                fields: fields,
                listRule: "",
                viewRule: "",
                createRule: "",
                updateRule: "",
                deleteRule: ""
            });
            $app.save(col);
            return col;
        }
    }

    ensureCollection("tahun_ajaran", [
        { name: "tahun", type: "text", required: true },
        { name: "semester", type: "text", required: true },
        { name: "is_active", type: "number" },
        { name: "tanggal_mulai", type: "text" },
        { name: "tanggal_selesai", type: "text" },
        { name: "is_deleted", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("master_guru", [
        { name: "nip", type: "text" },
        { name: "npk", type: "text" },
        { name: "nuptk", type: "text" },
        { name: "nik", type: "text", required: true },
        { name: "nama_lengkap", type: "text", required: true },
        { name: "gelar_depan", type: "text" },
        { name: "gelar_belakang", type: "text" },
        { name: "jenis_kelamin", type: "text", required: true },
        { name: "no_wa", type: "text", required: true },
        { name: "status_kepegawaian", type: "text", required: true },
        { name: "role", type: "text", required: true },
        { name: "username", type: "text", required: true },
        { name: "password", type: "text", required: true },
        { name: "is_deleted", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("master_kelas", [
        { name: "tingkat", type: "number", required: true },
        { name: "nama_rombel", type: "text", required: true },
        { name: "wali_kelas_guru_id", type: "text" },
        { name: "is_deleted", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("master_mapel", [
        { name: "kode_mapel", type: "text", required: true },
        { name: "nama_mapel", type: "text", required: true },
        { name: "kelompok", type: "text", required: true },
        { name: "is_deleted", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("penugasan_guru", [
        { name: "guru_id", type: "text", required: true },
        { name: "mapel_id", type: "text", required: true },
        { name: "kelas_id", type: "text", required: true },
        { name: "tahun_ajaran_id", type: "text", required: true },
        { name: "jadwal_json", type: "text", required: true },
        { name: "is_deleted", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("siswa_core", [
        { name: "nis_lokal", type: "text", required: true },
        { name: "nisn", type: "text" },
        { name: "nama_lengkap", type: "text", required: true },
        { name: "jenis_kelamin", type: "text", required: true },
        { name: "foto_file_name", type: "text" },
        { name: "status_siswa", type: "text", required: true },
        { name: "is_deleted", type: "number" },
        { name: "sync_status", type: "text" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("siswa_detail", [
        { name: "siswa_id", type: "text", required: true },
        { name: "nik", type: "text", required: true },
        { name: "no_kk", type: "text", required: true },
        { name: "tempat_lahir", type: "text", required: true },
        { name: "tanggal_lahir", type: "text", required: true },
        { name: "agama", type: "text" },
        { name: "anak_ke", type: "number" },
        { name: "jumlah_saudara", type: "number" },
        { name: "alamat_jalan", type: "text", required: true },
        { name: "rt", type: "text" },
        { name: "rw", type: "text" },
        { name: "desa_kelurahan", type: "text", required: true },
        { name: "kecamatan", type: "text", required: true },
        { name: "kabupaten_kota", type: "text", required: true },
        { name: "kode_pos", type: "text" },
        { name: "status_tempat_tinggal", type: "text" },
        { name: "nik_ayah", type: "text" },
        { name: "nama_ayah", type: "text" },
        { name: "tahun_lahir_ayah", type: "text" },
        { name: "status_ayah", type: "text" },
        { name: "pendidikan_ayah", type: "text" },
        { name: "pekerjaan_ayah", type: "text" },
        { name: "penghasilan_ayah", type: "text" },
        { name: "nik_ibu", type: "text" },
        { name: "nama_ibu", type: "text" },
        { name: "tahun_lahir_ibu", type: "text" },
        { name: "status_ibu", type: "text" },
        { name: "pendidikan_ibu", type: "text" },
        { name: "pekerjaan_ibu", type: "text" },
        { name: "penghasilan_ibu", type: "text" },
        { name: "no_hp_orang_tua_wa", type: "text", required: true },
        { name: "nama_wali", type: "text" },
        { name: "hubungan_wali", type: "text" },
        { name: "no_hp_wali", type: "text" },
        { name: "no_kip", type: "text" },
        { name: "no_kis_pkh", type: "text" },
        { name: "is_deleted", type: "number" },
        { name: "sync_status", type: "text" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("anggota_rombel", [
        { name: "siswa_id", type: "text", required: true },
        { name: "kelas_id", type: "text", required: true },
        { name: "tahun_ajaran_id", type: "text", required: true },
        { name: "nomor_absen", type: "number", required: true },
        { name: "status_keanggotaan", type: "text", required: true },
        { name: "is_deleted", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("kkm_mapel", [
        { name: "mapel_id", type: "text", required: true },
        { name: "tingkat_kelas", type: "number", required: true },
        { name: "tahun_ajaran_id", type: "text", required: true },
        { name: "nilai_kkm", type: "number", required: true },
        { name: "bobot_harian", type: "number", required: true },
        { name: "bobot_ulangan", type: "number", required: true },
        { name: "bobot_ujian_akhir", type: "number", required: true },
        { name: "is_deleted", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("komponen_nilai", [
        { name: "penugasan_id", type: "text", required: true },
        { name: "aspek", type: "text", required: true },
        { name: "kategori", type: "text", required: true },
        { name: "judul", type: "text", required: true },
        { name: "tanggal_input", type: "text", required: true },
        { name: "is_deleted", type: "number" },
        { name: "sync_status", type: "text" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("nilai_siswa", [
        { name: "komponen_id", type: "text", required: true },
        { name: "siswa_id", type: "text", required: true },
        { name: "skor", type: "number", required: true },
        { name: "is_deleted", type: "number" },
        { name: "sync_status", type: "text" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("presensi_sesi", [
        { name: "penugasan_id", type: "text", required: true },
        { name: "tanggal", type: "text", required: true },
        { name: "jam_ke", type: "text", required: true },
        { name: "materi_pokok", type: "text" },
        { name: "is_deleted", type: "number" },
        { name: "sync_status", type: "text" },
        { name: "created_at", type: "number" },
        { name: "updated_at", type: "number" }
    ]);

    ensureCollection("presensi_detail", [
        { name: "sesi_id", type: "text", required: true },
        { name: "siswa_id", type: "text", required: true },
        { name: "status_kehadiran", type: "text", required: true },
        { name: "catatan", type: "text" },
        { name: "is_deleted", type: "number" },
        { name: "sync_status", type: "text" },
        { name: "updated_at", type: "number" }
    ]);

    // 1.2 Semai Data Awal jika master_guru belum ada
    const countCheck = new DynamicModel({ total: 0 });
    $app.db().newQuery("SELECT count(*) as total FROM master_guru").one(countCheck);

    if (countCheck.total === 0) {
        const now = Date.now();

        // 1. Tahun Ajaran
        $app.db().newQuery(`
            INSERT INTO tahun_ajaran (id, tahun, semester, is_active, tanggal_mulai, tanggal_selesai, is_deleted, updated_at) VALUES (
                'ta_2026_ganjil', '2026/2027', 'GANJIL', 1, '2026-07-15', '2026-12-20', 0, {:now}
            )
        `).bind({ now }).execute();

        // 2. Master Guru (Admin & Guru Wali Kelas)
        $app.db().newQuery(`
            INSERT INTO master_guru (id, nip, npk, nuptk, nik, nama_lengkap, gelar_depan, gelar_belakang, jenis_kelamin, no_wa, status_kepegawaian, role, username, password, is_deleted, updated_at) VALUES 
            (
                'guru_admin', '198001012005011001', 'NPK101', 'NUPTK101', '3301010101800001',
                'Drs. H. Maimun Zubair, M.Pd.', 'Drs. H.', 'M.Pd.', 'L', '081234567890',
                'PNS', 'ADMIN', 'admin', 'admin123', 0, {:now}
            ),
            (
                'guru_ahmad', '198505122010011002', 'NPK102', 'NUPTK102', '3301011205850002',
                'Ahmad Fauzi, S.Pd.I', '', 'S.Pd.I', 'L', '081298765432',
                'GTY', 'GURU_WALI', 'ahmad', 'guru123', 0, {:now}
            )
        `).bind({ now }).execute();

        // 3. Master Kelas
        $app.db().newQuery(`
            INSERT INTO master_kelas (id, tingkat, nama_rombel, wali_kelas_guru_id, is_deleted, updated_at) VALUES 
            ('kelas_7a', 7, '7A', 'guru_ahmad', 0, {:now}),
            ('kelas_7b', 7, '7B', '', 0, {:now}),
            ('kelas_8a', 8, '8A', '', 0, {:now})
        `).bind({ now }).execute();

        // 4. Master Mapel
        $app.db().newQuery(`
            INSERT INTO master_mapel (id, kode_mapel, nama_mapel, kelompok, is_deleted, updated_at) VALUES 
            ('mapel_pai', 'PAI-07', 'Pendidikan Agama Islam', 'A', 0, {:now}),
            ('mapel_bar', 'BAR-07', 'Bahasa Arab', 'A', 0, {:now}),
            ('mapel_mat', 'MAT-07', 'Matematika', 'A', 0, {:now}),
            ('mapel_tik', 'TIK-07', 'Informatika / TIK', 'B', 0, {:now})
        `).bind({ now }).execute();

        // 5. Penugasan Guru
        $app.db().newQuery(`
            INSERT INTO penugasan_guru (id, guru_id, mapel_id, kelas_id, tahun_ajaran_id, jadwal_json, is_deleted, updated_at) VALUES 
            ('penugasan_1', 'guru_admin', 'mapel_pai', 'kelas_7a', 'ta_2026_ganjil', '[{"hari":1,"jamKe":"1-2","mulai":"07:30"}]', 0, {:now}),
            ('penugasan_2', 'guru_ahmad', 'mapel_bar', 'kelas_7a', 'ta_2026_ganjil', '[{"hari":2,"jamKe":"3-4","mulai":"09:00"}]', 0, {:now}),
            ('penugasan_3', 'guru_ahmad', 'mapel_bar', 'kelas_7b', 'ta_2026_ganjil', '[{"hari":3,"jamKe":"1-2","mulai":"07:30"}]', 0, {:now})
        `).bind({ now }).execute();

        // 6. Siswa Core, Detail EMIS & Anggota Rombel
        const siswaList = [
            { id: "siswa_001", nis: "26001", nisn: "0081234561", nama: "Muhammad Ilham Ramadhan", jk: "L", foto: "siswa_001.webp", noWa: "628123456701", absen: 1 },
            { id: "siswa_002", nis: "26002", nisn: "0081234562", nama: "Siti Fatimah Az-Zahra", jk: "P", foto: "siswa_002.webp", noWa: "628123456702", absen: 2 },
            { id: "siswa_003", nis: "26003", nisn: "0081234563", nama: "Ahmad Danial Firdaus", jk: "L", foto: "siswa_003.webp", noWa: "628123456703", absen: 3 },
            { id: "siswa_004", nis: "26004", nisn: "0081234564", nama: "Nur Aini Salsabila", jk: "P", foto: "siswa_004.webp", noWa: "628123456704", absen: 4 },
            { id: "siswa_005", nis: "26005", nisn: "0081234565", nama: "Rizky Maulana Pratama", jk: "L", foto: "siswa_005.webp", noWa: "628123456705", absen: 5 },
            { id: "siswa_006", nis: "26006", nisn: "0081234566", nama: "Zahra Putri Lestari", jk: "P", foto: "siswa_006.webp", noWa: "628123456706", absen: 6 }
        ];

        for (let s of siswaList) {
            $app.db().newQuery(`
                INSERT INTO siswa_core (id, nis_lokal, nisn, nama_lengkap, jenis_kelamin, foto_file_name, status_siswa, is_deleted, sync_status, updated_at) VALUES 
                ({:id}, {:nis}, {:nisn}, {:nama}, {:jk}, {:foto}, 'AKTIF', 0, 'SYNCED', {:now})
            `).bind({ id: s.id, nis: s.nis, nisn: s.nisn, nama: s.nama, jk: s.jk, foto: s.foto, now }).execute();

            $app.db().newQuery(`
                INSERT INTO siswa_detail (
                    id, siswa_id, nik, no_kk, tempat_lahir, tanggal_lahir, agama, anak_ke, jumlah_saudara,
                    alamat_jalan, rt, rw, desa_kelurahan, kecamatan, kabupaten_kota, kode_pos,
                    status_tempat_tinggal, nama_ayah, status_ayah, nama_ibu, status_ibu,
                    no_hp_orang_tua_wa, is_deleted, sync_status, updated_at
                ) VALUES (
                    'detail_' || {:id}, {:id}, '330101' || {:nis}, '3301019999990001', 'Kudus', '2012-05-10',
                    'Islam', 1, 2, 'Jl. Madrasah No. 12', '001', '002', 'Bae', 'Bae', 'Kudus', '59381',
                    'Bersama Orang Tua', 'Bapak ' || {:nama}, 'Masih Hidup', 'Ibu ' || {:nama}, 'Masih Hidup',
                    {:noWa}, 0, 'SYNCED', {:now}
                )
            `).bind({ id: s.id, nis: s.nis, nama: s.nama, noWa: s.noWa, now }).execute();

            $app.db().newQuery(`
                INSERT INTO anggota_rombel (id, siswa_id, kelas_id, tahun_ajaran_id, nomor_absen, status_keanggotaan, is_deleted, updated_at) VALUES 
                ('rombel_' || {:id}, {:id}, 'kelas_7a', 'ta_2026_ganjil', {:absen}, 'AKTIF', 0, {:now})
            `).bind({ id: s.id, absen: s.absen, now }).execute();
        }

        // 7. KKM Mapel (Default KKM 75)
        $app.db().newQuery(`
            INSERT INTO kkm_mapel (id, mapel_id, tingkat_kelas, tahun_ajaran_id, nilai_kkm, bobot_harian, bobot_ulangan, bobot_ujian_akhir, is_deleted, updated_at) VALUES 
            ('kkm_pai_7', 'mapel_pai', 7, 'ta_2026_ganjil', 75, 2, 2, 1, 0, {:now}),
            ('kkm_bar_7', 'mapel_bar', 7, 'ta_2026_ganjil', 75, 2, 2, 1, 0, {:now}),
            ('kkm_mat_7', 'mapel_mat', 7, 'ta_2026_ganjil', 75, 2, 2, 1, 0, {:now})
        `).bind({ now }).execute();

        // 8. Komponen Nilai Awal
        $app.db().newQuery(`
            INSERT INTO komponen_nilai (id, penugasan_id, aspek, kategori, judul, tanggal_input, is_deleted, sync_status, updated_at) VALUES 
            ('komp_pai_1', 'penugasan_1', 'KI3', 'UH', 'PH 1 - Akidah Akhlak', '2026-08-10', 0, 'SYNCED', {:now}),
            ('komp_pai_2', 'penugasan_1', 'KI4', 'PRAKTIK', 'Praktik Bersuci (Wudhu)', '2026-08-15', 0, 'SYNCED', {:now})
        `).bind({ now }).execute();

        // 9. Nilai Siswa Awal
        $app.db().newQuery(`
            INSERT INTO nilai_siswa (id, komponen_id, siswa_id, skor, is_deleted, sync_status, updated_at) VALUES 
            ('nilai_1_1', 'komp_pai_1', 'siswa_001', 88.0, 0, 'SYNCED', {:now}),
            ('nilai_1_2', 'komp_pai_1', 'siswa_002', 92.0, 0, 'SYNCED', {:now}),
            ('nilai_2_1', 'komp_pai_2', 'siswa_001', 85.0, 0, 'SYNCED', {:now}),
            ('nilai_2_2', 'komp_pai_2', 'siswa_002', 90.0, 0, 'SYNCED', {:now})
        `).bind({ now }).execute();
    }
});

// =========================================================================
// 2. ENDPOINT: GET /api/v1/health
// =========================================================================

routerAdd("GET", "/api/v1/health", (e) => {
    return e.json(200, {
        status: "ok",
        serverTime: Date.now()
    });
});

// =========================================================================
// 3. ENDPOINT: POST /api/v1/auth/login
// =========================================================================

routerAdd("POST", "/api/v1/auth/login", (e) => {
    try {
        const body = e.requestInfo().body;
        const nipOrUsername = (body.nipOrUsername || "").trim();
        const password = (body.password || "").trim();

        if (!nipOrUsername || !password) {
            return e.json(400, { error: "Username/NIP dan Password wajib diisi." });
        }

        const userModel = new DynamicModel({
            id: "",
            nip: "",
            nama_lengkap: "",
            role: "",
            no_wa: "",
            password: ""
        });

        $app.db().newQuery(`
            SELECT id, IFNULL(nip, '') as nip, nama_lengkap, role, no_wa, password 
            FROM master_guru 
            WHERE (username = {:u} OR nip = {:u}) AND is_deleted = 0
            LIMIT 1
        `).bind({ u: nipOrUsername }).one(userModel);

        if (!userModel.id || userModel.password !== password) {
            return e.json(401, { error: "Username/NIP atau Password salah!" });
        }

        const now = Date.now();
        const token = "simrth_session_" + userModel.id + "_" + now;

        function loadAll(sql, proto, mapper) {
            const list = arrayOf(new DynamicModel(proto));
            $app.db().newQuery(sql).all(list);
            return list.map(mapper);
        }

        const snapshot = {
            serverTime: now,
            tahunAjaranList: loadAll("SELECT id, tahun, semester, is_active, tanggal_mulai, tanggal_selesai, is_deleted, updated_at FROM tahun_ajaran WHERE is_deleted = 0", {
                id: "", tahun: "", semester: "", is_active: 0,
                tanggal_mulai: "", tanggal_selesai: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, tahun: r.tahun, semester: r.semester,
                isActive: Number(r.is_active) === 1, tanggalMulai: r.tanggal_mulai,
                tanggalSelesai: r.tanggal_selesai, isDeleted: Number(r.is_deleted) === 1,
                updatedAt: Number(r.updated_at)
            })),

            guruList: loadAll(`
                SELECT id, IFNULL(nip,'') as nip, IFNULL(npk,'') as npk, IFNULL(nuptk,'') as nuptk,
                       nik, nama_lengkap, IFNULL(gelar_depan,'') as gelar_depan,
                       IFNULL(gelar_belakang,'') as gelar_belakang, jenis_kelamin, no_wa,
                       status_kepegawaian, role, is_deleted, updated_at
                FROM master_guru WHERE is_deleted = 0
            `, {
                id: "", nip: "", npk: "", nuptk: "", nik: "",
                nama_lengkap: "", gelar_depan: "", gelar_belakang: "",
                jenis_kelamin: "", no_wa: "", status_kepegawaian: "",
                role: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, nip: r.nip || null, npk: r.npk || null, nuptk: r.nuptk || null,
                nik: r.nik, namaLengkap: r.nama_lengkap, gelarDepan: r.gelar_depan || null,
                gelarBelakang: r.gelar_belakang || null, jenisKelamin: r.jenis_kelamin,
                noWa: r.no_wa, statusKepegawaian: r.status_kepegawaian, role: r.role,
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            kelasList: loadAll(`
                SELECT id, tingkat, nama_rombel, IFNULL(wali_kelas_guru_id,'') as wali_kelas_guru_id,
                       is_deleted, updated_at
                FROM master_kelas WHERE is_deleted = 0
            `, {
                id: "", tingkat: 0, nama_rombel: "", wali_kelas_guru_id: "",
                is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, tingkat: Number(r.tingkat), namaRombel: r.nama_rombel,
                waliKelasGuruId: r.wali_kelas_guru_id || null,
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            mapelList: loadAll("SELECT id, kode_mapel, nama_mapel, kelompok, is_deleted, updated_at FROM master_mapel WHERE is_deleted = 0", {
                id: "", kode_mapel: "", nama_mapel: "", kelompok: "",
                is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, kodeMapel: r.kode_mapel, namaMapel: r.nama_mapel,
                kelompok: r.kelompok, isDeleted: Number(r.is_deleted) === 1,
                updatedAt: Number(r.updated_at)
            })),

            penugasanList: loadAll("SELECT id, guru_id, mapel_id, kelas_id, tahun_ajaran_id, jadwal_json, is_deleted, updated_at FROM penugasan_guru WHERE is_deleted = 0", {
                id: "", guru_id: "", mapel_id: "", kelas_id: "",
                tahun_ajaran_id: "", jadwal_json: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, guruId: r.guru_id, mapelId: r.mapel_id, kelasId: r.kelas_id,
                tahunAjaranId: r.tahun_ajaran_id, jadwalJson: r.jadwal_json,
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            siswaCoreList: loadAll(`
                SELECT id, nis_lokal, IFNULL(nisn,'') as nisn, nama_lengkap, jenis_kelamin,
                       IFNULL(foto_file_name,'') as foto_file_name, status_siswa,
                       is_deleted, sync_status, updated_at
                FROM siswa_core WHERE is_deleted = 0
            `, {
                id: "", nis_lokal: "", nisn: "", nama_lengkap: "",
                jenis_kelamin: "", foto_file_name: "", status_siswa: "",
                is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, nisLokal: r.nis_lokal, nisn: r.nisn || null,
                namaLengkap: r.nama_lengkap, jenisKelamin: r.jenis_kelamin,
                fotoFileName: r.foto_file_name || null, statusSiswa: r.status_siswa,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            })),

            siswaDetailList: loadAll(`
                SELECT id, siswa_id, nik, no_kk, tempat_lahir, tanggal_lahir, agama, anak_ke, jumlah_saudara,
                       alamat_jalan, rt, rw, desa_kelurahan, kecamatan, kabupaten_kota,
                       IFNULL(kode_pos,'') as kode_pos, status_tempat_tinggal,
                       IFNULL(nik_ayah,'') as nik_ayah, IFNULL(nama_ayah,'') as nama_ayah,
                       IFNULL(tahun_lahir_ayah,'') as tahun_lahir_ayah, IFNULL(status_ayah,'') as status_ayah,
                       IFNULL(pendidikan_ayah,'') as pendidikan_ayah, IFNULL(pekerjaan_ayah,'') as pekerjaan_ayah,
                       IFNULL(penghasilan_ayah,'') as penghasilan_ayah, IFNULL(nik_ibu,'') as nik_ibu,
                       IFNULL(nama_ibu,'') as nama_ibu, IFNULL(tahun_lahir_ibu,'') as tahun_lahir_ibu,
                       IFNULL(status_ibu,'') as status_ibu, IFNULL(pendidikan_ibu,'') as pendidikan_ibu,
                       IFNULL(pekerjaan_ibu,'') as pekerjaan_ibu, IFNULL(penghasilan_ibu,'') as penghasilan_ibu,
                       no_hp_orang_tua_wa, IFNULL(nama_wali,'') as nama_wali,
                       IFNULL(hubungan_wali,'') as hubungan_wali, IFNULL(no_hp_wali,'') as no_hp_wali,
                       IFNULL(no_kip,'') as no_kip, IFNULL(no_kis_pkh,'') as no_kis_pkh,
                       is_deleted, sync_status, updated_at
                FROM siswa_detail WHERE is_deleted = 0
            `, {
                id: "", siswa_id: "", nik: "", no_kk: "", tempat_lahir: "",
                tanggal_lahir: "", agama: "", anak_ke: 0, jumlah_saudara: 0,
                alamat_jalan: "", rt: "", rw: "", desa_kelurahan: "",
                kecamatan: "", kabupaten_kota: "", kode_pos: "", status_tempat_tinggal: "",
                nik_ayah: "", nama_ayah: "", tahun_lahir_ayah: "", status_ayah: "",
                pendidikan_ayah: "", pekerjaan_ayah: "", penghasilan_ayah: "",
                nik_ibu: "", nama_ibu: "", tahun_lahir_ibu: "", status_ibu: "",
                pendidikan_ibu: "", pekerjaan_ibu: "", penghasilan_ibu: "",
                no_hp_orang_tua_wa: "", nama_wali: "", hubungan_wali: "", no_hp_wali: "",
                no_kip: "", no_kis_pkh: "", is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, siswaId: r.siswa_id, nik: r.nik, noKk: r.no_kk,
                tempatLahir: r.tempat_lahir, tanggalLahir: r.tanggal_lahir,
                agama: r.agama || "Islam", anakKe: Number(r.anak_ke || 1),
                jumlahSaudara: Number(r.jumlah_saudara || 0), alamatJalan: r.alamat_jalan,
                rt: r.rt || "001", rw: r.rw || "001", desaKelurahan: r.desa_kelurahan,
                kecamatan: r.kecamatan, kabupatenKota: r.kabupaten_kota,
                kodePos: r.kode_pos || null, statusTempatTinggal: r.status_tempat_tinggal || "Bersama Orang Tua",
                nikAyah: r.nik_ayah || null, namaAyah: r.nama_ayah || null,
                tahunLahirAyah: r.tahun_lahir_ayah || null, statusAyah: r.status_ayah || "Masih Hidup",
                pendidikanAyah: r.pendidikan_ayah || null, pekerjaanAyah: r.pekerjaan_ayah || null,
                penghasilanAyah: r.penghasilan_ayah || null, nikIbu: r.nik_ibu || null,
                namaIbu: r.nama_ibu || null, tahunLahirIbu: r.tahun_lahir_ibu || null,
                statusIbu: r.status_ibu || "Masih Hidup", pendidikanIbu: r.pendidikan_ibu || null,
                pekerjaanIbu: r.pekerjaan_ibu || null, penghasilanIbu: r.penghasilan_ibu || null,
                noHpOrangTuaWa: r.no_hp_orang_tua_wa, namaWali: r.nama_wali || null,
                hubunganWali: r.hubungan_wali || null, noHpWali: r.no_hp_wali || null,
                noKip: r.no_kip || null, noKisPkh: r.no_kis_pkh || null,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            })),

            anggotaRombelList: loadAll("SELECT id, siswa_id, kelas_id, tahun_ajaran_id, nomor_absen, status_keanggotaan, is_deleted, updated_at FROM anggota_rombel WHERE is_deleted = 0", {
                id: "", siswa_id: "", kelas_id: "", tahun_ajaran_id: "",
                nomor_absen: 0, status_keanggotaan: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, siswaId: r.siswa_id, kelasId: r.kelas_id,
                tahunAjaranId: r.tahun_ajaran_id, nomorAbsen: Number(r.nomor_absen),
                statusKeanggotaan: r.status_keanggotaan, isDeleted: Number(r.is_deleted) === 1,
                updatedAt: Number(r.updated_at)
            })),

            kkmList: loadAll("SELECT id, mapel_id, tingkat_kelas, tahun_ajaran_id, nilai_kkm, bobot_harian, bobot_ulangan, bobot_ujian_akhir, is_deleted, updated_at FROM kkm_mapel WHERE is_deleted = 0", {
                id: "", mapel_id: "", tingkat_kelas: 0, tahun_ajaran_id: "",
                nilai_kkm: 0, bobot_harian: 0, bobot_ulangan: 0, bobot_ujian_akhir: 0,
                is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, mapelId: r.mapel_id, tingkatKelas: Number(r.tingkat_kelas),
                tahunAjaranId: r.tahun_ajaran_id, nilaiKkm: Number(r.nilai_kkm),
                bobotHarian: Number(r.bobot_harian), bobotUlangan: Number(r.bobot_ulangan),
                bobotUjianAkhir: Number(r.bobot_ujian_akhir),
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            komponenList: loadAll("SELECT id, penugasan_id, aspek, kategori, judul, tanggal_input, is_deleted, sync_status, updated_at FROM komponen_nilai WHERE is_deleted = 0", {
                id: "", penugasan_id: "", aspek: "", kategori: "",
                judul: "", tanggal_input: "", is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, penugasanId: r.penugasan_id, aspek: r.aspek,
                kategori: r.kategori, judul: r.judul, tanggalInput: r.tanggal_input,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            })),

            nilaiList: loadAll("SELECT id, komponen_id, siswa_id, skor, is_deleted, sync_status, updated_at FROM nilai_siswa WHERE is_deleted = 0", {
                id: "", komponen_id: "", siswa_id: "", skor: 0.0,
                is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, komponenId: r.komponen_id, siswaId: r.siswa_id,
                skor: Number(r.skor), isDeleted: Number(r.is_deleted) === 1,
                syncStatus: "SYNCED", updatedAt: Number(r.updated_at)
            })),

            presensiSesiList: loadAll("SELECT id, penugasan_id, tanggal, jam_ke, IFNULL(materi_pokok,'') as materi_pokok, is_deleted, sync_status, created_at, updated_at FROM presensi_sesi WHERE is_deleted = 0", {
                id: "", penugasan_id: "", tanggal: "", jam_ke: "",
                materi_pokok: "", is_deleted: 0, sync_status: "", created_at: 0, updated_at: 0
            }, r => ({
                id: r.id, penugasanId: r.penugasan_id, tanggal: r.tanggal,
                jamKe: r.jam_ke, materiPokok: r.materi_pokok || null,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                createdAt: Number(r.created_at), updatedAt: Number(r.updated_at)
            })),

            presensiDetailList: loadAll("SELECT id, sesi_id, siswa_id, status_kehadiran, IFNULL(catatan,'') as catatan, is_deleted, sync_status, updated_at FROM presensi_detail WHERE is_deleted = 0", {
                id: "", sesi_id: "", siswa_id: "", status_kehadiran: "",
                catatan: "", is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, sesiId: r.sesi_id, siswaId: r.siswa_id,
                statusKehadiran: r.status_kehadiran, catatan: r.catatan || null,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            }))
        };

        return e.json(200, {
            token: token,
            user: {
                id: userModel.id,
                nip: userModel.nip || null,
                namaLengkap: userModel.nama_lengkap,
                role: userModel.role,
                noWa: userModel.no_wa || null
            },
            initialSnapshot: snapshot
        });
    } catch (err) {
        return e.json(500, { error: "Login gagal: " + err.toString() });
    }
});

// =========================================================================
// 4. ENDPOINT: POST /api/v1/sync/push
// =========================================================================

routerAdd("POST", "/api/v1/sync/push", (e) => {
    try {
        const body = e.requestInfo().body;
        const mutations = body.mutations || [];
        const acceptedIds = [];
        const rejectedIds = [];
        const serverTime = Date.now();

        for (let m of mutations) {
            try {
                const table = m.tableNameTarget;
                const action = (m.actionType || "UPSERT").toUpperCase();
                const payload = JSON.parse(m.payloadJson || "{}");
                const updatedAt = Number(m.updatedAt || serverTime);

                if (action === "DELETE") {
                    $app.db().newQuery(
                        "UPDATE " + table + " SET is_deleted = 1, updated_at = {:up} WHERE id = {:id}"
                    ).bind({ up: updatedAt, id: m.recordId }).execute();
                } else if (table === "presensi_sesi") {
                    $app.db().newQuery(`
                        INSERT OR REPLACE INTO presensi_sesi VALUES 
                        ({:id}, {:penugasanId}, {:tanggal}, {:jamKe}, {:materiPokok}, 0, 'SYNCED', {:createdAt}, {:updatedAt})
                    `).bind({
                        id: payload.id,
                        penugasanId: payload.penugasanId,
                        tanggal: payload.tanggal,
                        jamKe: payload.jamKe,
                        materiPokok: payload.materiPokok || null,
                        createdAt: payload.createdAt || updatedAt,
                        updatedAt: updatedAt
                    }).execute();
                } else if (table === "presensi_detail") {
                    $app.db().newQuery(`
                        INSERT OR REPLACE INTO presensi_detail VALUES 
                        ({:id}, {:sesiId}, {:siswaId}, {:statusKehadiran}, {:catatan}, 0, 'SYNCED', {:updatedAt})
                    `).bind({
                        id: payload.id,
                        sesiId: payload.sesiId,
                        siswaId: payload.siswaId,
                        statusKehadiran: payload.statusKehadiran,
                        catatan: payload.catatan || null,
                        updatedAt: updatedAt
                    }).execute();
                } else if (table === "komponen_nilai") {
                    $app.db().newQuery(`
                        INSERT OR REPLACE INTO komponen_nilai VALUES 
                        ({:id}, {:penugasanId}, {:aspek}, {:kategori}, {:judul}, {:tanggalInput}, 0, 'SYNCED', {:updatedAt})
                    `).bind({
                        id: payload.id,
                        penugasanId: payload.penugasanId,
                        aspek: payload.aspek,
                        kategori: payload.kategori,
                        judul: payload.judul,
                        tanggalInput: payload.tanggalInput,
                        updatedAt: updatedAt
                    }).execute();
                } else if (table === "nilai_siswa") {
                    $app.db().newQuery(`
                        INSERT OR REPLACE INTO nilai_siswa VALUES 
                        ({:id}, {:komponenId}, {:siswaId}, {:skor}, 0, 'SYNCED', {:updatedAt})
                    `).bind({
                        id: payload.id,
                        komponenId: payload.komponenId,
                        siswaId: payload.siswaId,
                        skor: Number(payload.skor),
                        updatedAt: updatedAt
                    }).execute();
                } else if (table === "kkm_mapel") {
                    $app.db().newQuery(`
                        INSERT OR REPLACE INTO kkm_mapel VALUES 
                        ({:id}, {:mapelId}, {:tingkatKelas}, {:tahunAjaranId}, {:nilaiKkm}, {:bobotHarian}, {:bobotUlangan}, {:bobotUjianAkhir}, 0, {:updatedAt})
                    `).bind({
                        id: payload.id,
                        mapelId: payload.mapelId,
                        tingkatKelas: Number(payload.tingkatKelas),
                        tahunAjaranId: payload.tahunAjaranId,
                        nilaiKkm: Number(payload.nilaiKkm),
                        bobotHarian: Number(payload.bobotHarian),
                        bobotUlangan: Number(payload.bobotUlangan),
                        bobotUjianAkhir: Number(payload.bobotUjianAkhir),
                        updatedAt: updatedAt
                    }).execute();
                } else if (table === "siswa_core") {
                    $app.db().newQuery(`
                        INSERT OR REPLACE INTO siswa_core VALUES 
                        ({:id}, {:nisLokal}, {:nisn}, {:namaLengkap}, {:jenisKelamin}, {:fotoFileName}, {:statusSiswa}, 0, 'SYNCED', {:updatedAt})
                    `).bind({
                        id: payload.id,
                        nisLokal: payload.nisLokal,
                        nisn: payload.nisn || null,
                        namaLengkap: payload.namaLengkap,
                        jenisKelamin: payload.jenisKelamin,
                        fotoFileName: payload.fotoFileName || null,
                        statusSiswa: payload.statusSiswa || "AKTIF",
                        updatedAt: updatedAt
                    }).execute();
                } else if (table === "anggota_rombel") {
                    $app.db().newQuery(`
                        INSERT OR REPLACE INTO anggota_rombel VALUES 
                        ({:id}, {:siswaId}, {:kelasId}, {:tahunAjaranId}, {:nomorAbsen}, {:statusKeanggotaan}, 0, {:updatedAt})
                    `).bind({
                        id: payload.id,
                        siswaId: payload.siswaId,
                        kelasId: payload.kelasId,
                        tahunAjaranId: payload.tahunAjaranId,
                        nomorAbsen: Number(payload.nomorAbsen),
                        statusKeanggotaan: payload.statusKeanggotaan || "AKTIF",
                        updatedAt: updatedAt
                    }).execute();
                }

                acceptedIds.push(m.id);
            } catch (err) {
                rejectedIds.push(m.id);
            }
        }

        return e.json(200, {
            acceptedMutationIds: acceptedIds,
            rejectedMutationIds: rejectedIds,
            serverTime: serverTime
        });
    } catch (err) {
        return e.json(500, { error: "Gagal memproses batch outbox: " + err.toString() });
    }
});

// =========================================================================
// 5. ENDPOINT: GET /api/v1/sync/pull
// =========================================================================

routerAdd("GET", "/api/v1/sync/pull", (e) => {
    try {
        const since = Number(e.requestInfo().query.since || 0);
        const cond = since > 0 ? " WHERE updated_at > " + since : "";
        const now = Date.now();

        function loadAll(sql, proto, mapper) {
            const list = arrayOf(new DynamicModel(proto));
            $app.db().newQuery(sql).all(list);
            return list.map(mapper);
        }

        return e.json(200, {
            serverTime: now,
            tahunAjaranList: loadAll("SELECT id, tahun, semester, is_active, tanggal_mulai, tanggal_selesai, is_deleted, updated_at FROM tahun_ajaran" + cond, {
                id: "", tahun: "", semester: "", is_active: 0,
                tanggal_mulai: "", tanggal_selesai: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, tahun: r.tahun, semester: r.semester,
                isActive: Number(r.is_active) === 1, tanggalMulai: r.tanggal_mulai,
                tanggalSelesai: r.tanggal_selesai, isDeleted: Number(r.is_deleted) === 1,
                updatedAt: Number(r.updated_at)
            })),

            guruList: loadAll(`
                SELECT id, IFNULL(nip,'') as nip, IFNULL(npk,'') as npk, IFNULL(nuptk,'') as nuptk,
                       nik, nama_lengkap, IFNULL(gelar_depan,'') as gelar_depan,
                       IFNULL(gelar_belakang,'') as gelar_belakang, jenis_kelamin, no_wa,
                       status_kepegawaian, role, is_deleted, updated_at
                FROM master_guru ` + cond, {
                id: "", nip: "", npk: "", nuptk: "", nik: "",
                nama_lengkap: "", gelar_depan: "", gelar_belakang: "",
                jenis_kelamin: "", no_wa: "", status_kepegawaian: "",
                role: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, nip: r.nip || null, npk: r.npk || null, nuptk: r.nuptk || null,
                nik: r.nik, namaLengkap: r.nama_lengkap, gelarDepan: r.gelar_depan || null,
                gelarBelakang: r.gelar_belakang || null, jenisKelamin: r.jenis_kelamin,
                noWa: r.no_wa, statusKepegawaian: r.status_kepegawaian, role: r.role,
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            kelasList: loadAll(`
                SELECT id, tingkat, nama_rombel, IFNULL(wali_kelas_guru_id,'') as wali_kelas_guru_id,
                       is_deleted, updated_at
                FROM master_kelas ` + cond, {
                id: "", tingkat: 0, nama_rombel: "", wali_kelas_guru_id: "",
                is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, tingkat: Number(r.tingkat), namaRombel: r.nama_rombel,
                waliKelasGuruId: r.wali_kelas_guru_id || null,
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            mapelList: loadAll("SELECT id, kode_mapel, nama_mapel, kelompok, is_deleted, updated_at FROM master_mapel" + cond, {
                id: "", kode_mapel: "", nama_mapel: "", kelompok: "",
                is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, kodeMapel: r.kode_mapel, namaMapel: r.nama_mapel,
                kelompok: r.kelompok, isDeleted: Number(r.is_deleted) === 1,
                updatedAt: Number(r.updated_at)
            })),

            penugasanList: loadAll("SELECT id, guru_id, mapel_id, kelas_id, tahun_ajaran_id, jadwal_json, is_deleted, updated_at FROM penugasan_guru" + cond, {
                id: "", guru_id: "", mapel_id: "", kelas_id: "",
                tahun_ajaran_id: "", jadwal_json: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, guruId: r.guru_id, mapelId: r.mapel_id, kelasId: r.kelas_id,
                tahunAjaranId: r.tahun_ajaran_id, jadwalJson: r.jadwal_json,
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            siswaCoreList: loadAll(`
                SELECT id, nis_lokal, IFNULL(nisn,'') as nisn, nama_lengkap, jenis_kelamin,
                       IFNULL(foto_file_name,'') as foto_file_name, status_siswa,
                       is_deleted, sync_status, updated_at
                FROM siswa_core ` + cond, {
                id: "", nis_lokal: "", nisn: "", nama_lengkap: "",
                jenis_kelamin: "", foto_file_name: "", status_siswa: "",
                is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, nisLokal: r.nis_lokal, nisn: r.nisn || null,
                namaLengkap: r.nama_lengkap, jenisKelamin: r.jenis_kelamin,
                fotoFileName: r.foto_file_name || null, statusSiswa: r.status_siswa,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            })),

            siswaDetailList: loadAll(`
                SELECT id, siswa_id, nik, no_kk, tempat_lahir, tanggal_lahir, agama, anak_ke, jumlah_saudara,
                       alamat_jalan, rt, rw, desa_kelurahan, kecamatan, kabupaten_kota,
                       IFNULL(kode_pos,'') as kode_pos, status_tempat_tinggal,
                       IFNULL(nik_ayah,'') as nik_ayah, IFNULL(nama_ayah,'') as nama_ayah,
                       IFNULL(tahun_lahir_ayah,'') as tahun_lahir_ayah, IFNULL(status_ayah,'') as status_ayah,
                       IFNULL(pendidikan_ayah,'') as pendidikan_ayah, IFNULL(pekerjaan_ayah,'') as pekerjaan_ayah,
                       IFNULL(penghasilan_ayah,'') as penghasilan_ayah, IFNULL(nik_ibu,'') as nik_ibu,
                       IFNULL(nama_ibu,'') as nama_ibu, IFNULL(tahun_lahir_ibu,'') as tahun_lahir_ibu,
                       IFNULL(status_ibu,'') as status_ibu, IFNULL(pendidikan_ibu,'') as pendidikan_ibu,
                       IFNULL(pekerjaan_ibu,'') as pekerjaan_ibu, IFNULL(penghasilan_ibu,'') as penghasilan_ibu,
                       no_hp_orang_tua_wa, IFNULL(nama_wali,'') as nama_wali,
                       IFNULL(hubungan_wali,'') as hubungan_wali, IFNULL(no_hp_wali,'') as no_hp_wali,
                       IFNULL(no_kip,'') as no_kip, IFNULL(no_kis_pkh,'') as no_kis_pkh,
                       is_deleted, sync_status, updated_at
                FROM siswa_detail ` + cond, {
                id: "", siswa_id: "", nik: "", no_kk: "", tempat_lahir: "",
                tanggal_lahir: "", agama: "", anak_ke: 0, jumlah_saudara: 0,
                alamat_jalan: "", rt: "", rw: "", desa_kelurahan: "",
                kecamatan: "", kabupaten_kota: "", kode_pos: "", status_tempat_tinggal: "",
                nik_ayah: "", nama_ayah: "", tahun_lahir_ayah: "", status_ayah: "",
                pendidikan_ayah: "", pekerjaan_ayah: "", penghasilan_ayah: "",
                nik_ibu: "", nama_ibu: "", tahun_lahir_ibu: "", status_ibu: "",
                pendidikan_ibu: "", pekerjaan_ibu: "", penghasilan_ibu: "",
                no_hp_orang_tua_wa: "", nama_wali: "", hubungan_wali: "", no_hp_wali: "",
                no_kip: "", no_kis_pkh: "", is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, siswaId: r.siswa_id, nik: r.nik, noKk: r.no_kk,
                tempatLahir: r.tempat_lahir, tanggalLahir: r.tanggal_lahir,
                agama: r.agama || "Islam", anakKe: Number(r.anak_ke || 1),
                jumlahSaudara: Number(r.jumlah_saudara || 0), alamatJalan: r.alamat_jalan,
                rt: r.rt || "001", rw: r.rw || "001", desaKelurahan: r.desa_kelurahan,
                kecamatan: r.kecamatan, kabupatenKota: r.kabupaten_kota,
                kodePos: r.kode_pos || null, statusTempatTinggal: r.status_tempat_tinggal || "Bersama Orang Tua",
                nikAyah: r.nik_ayah || null, namaAyah: r.nama_ayah || null,
                tahunLahirAyah: r.tahun_lahir_ayah || null, statusAyah: r.status_ayah || "Masih Hidup",
                pendidikanAyah: r.pendidikan_ayah || null, pekerjaanAyah: r.pekerjaan_ayah || null,
                penghasilanAyah: r.penghasilan_ayah || null, nikIbu: r.nik_ibu || null,
                namaIbu: r.nama_ibu || null, tahunLahirIbu: r.tahun_lahir_ibu || null,
                statusIbu: r.status_ibu || "Masih Hidup", pendidikanIbu: r.pendidikan_ibu || null,
                pekerjaanIbu: r.pekerjaan_ibu || null, penghasilanIbu: r.penghasilan_ibu || null,
                noHpOrangTuaWa: r.no_hp_orang_tua_wa, namaWali: r.nama_wali || null,
                hubunganWali: r.hubungan_wali || null, noHpWali: r.no_hp_wali || null,
                noKip: r.no_kip || null, noKisPkh: r.no_kis_pkh || null,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            })),

            anggotaRombelList: loadAll("SELECT id, siswa_id, kelas_id, tahun_ajaran_id, nomor_absen, status_keanggotaan, is_deleted, updated_at FROM anggota_rombel" + cond, {
                id: "", siswa_id: "", kelas_id: "", tahun_ajaran_id: "",
                nomor_absen: 0, status_keanggotaan: "", is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, siswaId: r.siswa_id, kelasId: r.kelas_id,
                tahunAjaranId: r.tahun_ajaran_id, nomorAbsen: Number(r.nomor_absen),
                statusKeanggotaan: r.status_keanggotaan, isDeleted: Number(r.is_deleted) === 1,
                updatedAt: Number(r.updated_at)
            })),

            kkmList: loadAll("SELECT id, mapel_id, tingkat_kelas, tahun_ajaran_id, nilai_kkm, bobot_harian, bobot_ulangan, bobot_ujian_akhir, is_deleted, updated_at FROM kkm_mapel" + cond, {
                id: "", mapel_id: "", tingkat_kelas: 0, tahun_ajaran_id: "",
                nilai_kkm: 0, bobot_harian: 0, bobot_ulangan: 0, bobot_ujian_akhir: 0,
                is_deleted: 0, updated_at: 0
            }, r => ({
                id: r.id, mapelId: r.mapel_id, tingkatKelas: Number(r.tingkat_kelas),
                tahunAjaranId: r.tahun_ajaran_id, nilaiKkm: Number(r.nilai_kkm),
                bobotHarian: Number(r.bobot_harian), bobotUlangan: Number(r.bobot_ulangan),
                bobotUjianAkhir: Number(r.bobot_ujian_akhir),
                isDeleted: Number(r.is_deleted) === 1, updatedAt: Number(r.updated_at)
            })),

            komponenList: loadAll("SELECT id, penugasan_id, aspek, kategori, judul, tanggal_input, is_deleted, sync_status, updated_at FROM komponen_nilai" + cond, {
                id: "", penugasan_id: "", aspek: "", kategori: "",
                judul: "", tanggal_input: "", is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, penugasanId: r.penugasan_id, aspek: r.aspek,
                kategori: r.kategori, judul: r.judul, tanggalInput: r.tanggal_input,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            })),

            nilaiList: loadAll("SELECT id, komponen_id, siswa_id, skor, is_deleted, sync_status, updated_at FROM nilai_siswa" + cond, {
                id: "", komponen_id: "", siswa_id: "", skor: 0.0,
                is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, komponenId: r.komponen_id, siswaId: r.siswa_id,
                skor: Number(r.skor), isDeleted: Number(r.is_deleted) === 1,
                syncStatus: "SYNCED", updatedAt: Number(r.updated_at)
            })),

            presensiSesiList: loadAll("SELECT id, penugasan_id, tanggal, jam_ke, IFNULL(materi_pokok,'') as materi_pokok, is_deleted, sync_status, created_at, updated_at FROM presensi_sesi" + cond, {
                id: "", penugasan_id: "", tanggal: "", jam_ke: "",
                materi_pokok: "", is_deleted: 0, sync_status: "", created_at: 0, updated_at: 0
            }, r => ({
                id: r.id, penugasanId: r.penugasan_id, tanggal: r.tanggal,
                jamKe: r.jam_ke, materiPokok: r.materi_pokok || null,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                createdAt: Number(r.created_at), updatedAt: Number(r.updated_at)
            })),

            presensiDetailList: loadAll("SELECT id, sesi_id, siswa_id, status_kehadiran, IFNULL(catatan,'') as catatan, is_deleted, sync_status, updated_at FROM presensi_detail" + cond, {
                id: "", sesi_id: "", siswa_id: "", status_kehadiran: "",
                catatan: "", is_deleted: 0, sync_status: "", updated_at: 0
            }, r => ({
                id: r.id, sesiId: r.sesi_id, siswaId: r.siswa_id,
                statusKehadiran: r.status_kehadiran, catatan: r.catatan || null,
                isDeleted: Number(r.is_deleted) === 1, syncStatus: "SYNCED",
                updatedAt: Number(r.updated_at)
            }))
        });
    } catch (err) {
        return e.json(500, { error: "Gagal mengambil delta: " + err.toString() });
    }
});

// =========================================================================
// 6. ENDPOINT: GET /api/v1/media/students/{fileName}
// =========================================================================

routerAdd("GET", "/api/v1/media/students/{fileName}", (e) => {
    const fileName = e.request.pathValue("fileName");
    const filePath = "/data/data/com.termux/files/home/project/server/pb_public/media/students/" + fileName;
    try {
        const bytes = $os.readFile(filePath);
        return e.blob(200, "image/webp", bytes);
    } catch (err) {
        return e.json(404, { error: "Foto siswa tidak ditemukan di server." });
    }
});
