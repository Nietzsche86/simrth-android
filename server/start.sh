#!/usr/bin/env bash
SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SERVER_DIR"

if [ -f "server.pid" ] && kill -0 $(cat server.pid) 2>/dev/null; then
    echo "⚠️  SIMRTH PocketBase server sudah berjalan (PID: $(cat server.pid))."
    exit 0
fi

echo "🚀 Menjalankan SIMRTH PocketBase Server pada port 8090..."
nohup pocketbase serve \
    --dir "$SERVER_DIR/pb_data" \
    --hooksDir "$SERVER_DIR/pb_hooks" \
    --publicDir "$SERVER_DIR/pb_public" \
    --http "0.0.0.0:8090" > "$SERVER_DIR/server.log" 2>&1 &

echo $! > "$SERVER_DIR/server.pid"
disown $!
sleep 2

if kill -0 $(cat server.pid) 2>/dev/null; then
    echo "✅ Server berhasil berjalan! (PID: $(cat server.pid))"
    echo ""
    echo "=========================================================="
    echo "🌐 URL Koneksi untuk Aplikasi SIMRTH Android:"
    echo "   - Dari perangkat HP yang sama : http://127.0.0.1:8090"
    LAN_IP=$(python3 -c "import socket; s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.connect(('8.8.8.8', 80)); print(s.getsockname()[0]); s.close()" 2>/dev/null || echo "192.168.x.x")
    echo "   - Dari HP lain di Wi-Fi sama   : http://${LAN_IP}:8090"
    echo "=========================================================="
    echo "🔑 Kredensial Login:"
    echo "   A. Dashboard Web PocketBase (http://127.0.0.1:8090/_/):"
    echo "      - Email    : admin@simrth.local"
    echo "      - Password : admin123456"
    echo "   B. Aplikasi Android SIMRTH (di HP):"
    echo "      - Admin : Username = admin  | Password = admin123"
    echo "      - Guru  : Username = ahmad  | Password = guru123"
    echo "=========================================================="
    echo "Log server dapat dipantau via: tail -f $SERVER_DIR/server.log"
else
    echo "❌ Server gagal berjalan. Silakan periksa log di: $SERVER_DIR/server.log"
    cat "$SERVER_DIR/server.log"
fi
