#!/usr/bin/env bash
SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SERVER_DIR"

if [ -f "server.pid" ]; then
    PID=$(cat server.pid)
    if kill -0 $PID 2>/dev/null; then
        kill $PID
        echo "🛑 Server SIMRTH PocketBase (PID: $PID) berhasil dihentikan."
    else
        echo "⚠️  Proses server dengan PID $PID sudah tidak aktif."
    fi
    rm -f server.pid
else
    echo "⚠️  Berkas server.pid tidak ditemukan. Mencari proses pocketbase..."
    pkill -f "pocketbase serve.*8090" && echo "🛑 Berhasil menghentikan proses pocketbase." || echo "Tidak ada proses pocketbase yang aktif."
fi
