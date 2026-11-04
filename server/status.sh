#!/usr/bin/env bash
SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SERVER_DIR"

if [ -f "server.pid" ] && kill -0 $(cat server.pid) 2>/dev/null; then
    echo "✅ SIMRTH PocketBase server aktif (PID: $(cat server.pid))."
    echo "📊 Memeriksa respon health endpoint:"
    curl -s http://127.0.0.1:8090/api/v1/health | python3 -m json.tool || echo "Gagal menghubungi endpoint."
else
    echo "❌ Server sedang TIDAK aktif."
fi
