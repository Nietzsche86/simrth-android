package id.mts.rth.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor keamanan jaringan lokal madrasah.
 * Memastikan request cleartext HTTP DILARANG keras menuju internet publik
 * dan HANYA diizinkan menuju subnet private madrasah (RFC 1918) atau domain .local.
 */
class LanSecurityInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        if (url.scheme.equals("http", ignoreCase = true)) {
            val host = url.host
            if (!isAllowedPrivateLanHost(host)) {
                throw SecurityException(
                    "Akses HTTP (Cleartext) ditolak demi keamanan! Host '$host' bukan IP Private LAN madrasah (RFC 1918) atau domain .local."
                )
            }
        }
        return chain.proceed(request)
    }

    private fun isAllowedPrivateLanHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true) || host == "127.0.0.1") return true
        if (host.endsWith(".local", ignoreCase = true)) return true

        // 10.0.0.0/8
        if (host.startsWith("10.")) return true
        // 192.168.0.0/16
        if (host.startsWith("192.168.")) return true
        // 172.16.0.0/12 (172.16.x.x - 172.31.x.x)
        val rfc172Regex = Regex("""^172\.(1[6-9]|2[0-9]|3[0-1])\..*""")
        if (rfc172Regex.matches(host)) return true

        return false
    }
}
