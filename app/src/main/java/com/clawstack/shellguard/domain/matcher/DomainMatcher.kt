package com.clawstack.shellguard.domain.matcher

import java.net.URI
import java.util.Locale

/**
 * Domain and URI matcher for Android Autofill.
 * Supports eTLD+1 normalization, exact IP/port matching for home labs,
 * prefix matching, and Android app package uri matching (`androidapp://<package_name>`).
 */
object DomainMatcher {

    // Common multi-part second-level domains
    private val MULTI_PART_TLDS = setOf(
        "co.uk", "org.uk", "me.uk", "ltd.uk", "plc.uk", "net.uk",
        "com.au", "net.au", "org.au", "edu.au", "gov.au",
        "com.br", "net.br", "org.br",
        "co.nz", "net.nz", "org.nz",
        "co.za", "org.za", "web.za",
        "co.jp", "ne.jp", "or.jp", "ac.jp", "go.jp",
        "com.cn", "net.cn", "org.cn", "gov.cn",
        "com.mx", "org.mx", "net.mx", "edu.mx",
        "com.sg", "edu.sg", "gov.sg",
        "com.tr", "org.tr", "net.tr",
        "com.ar", "org.ar", "net.ar"
    )

    private val IPV4_REGEX = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")

    /**
     * Checks if a host string is an IPv4 address, IPv6 address, or localhost.
     */
    fun isIpOrLocalhost(host: String): Boolean {
        val cleanHost = host.lowercase(Locale.ROOT).trim()
        if (cleanHost == "localhost" || cleanHost == "127.0.0.1" || cleanHost == "::1") return true
        return IPV4_REGEX.matches(cleanHost) || (cleanHost.startsWith("[") && cleanHost.endsWith("]"))
    }

    /**
     * Extracts effective top-level domain + 1 (eTLD+1).
     * If the host is an IP or localhost, returns the exact host.
     */
    fun getEffectiveDomain(urlOrHost: String): String {
        val host = getHost(urlOrHost)
        if (host.isBlank()) return urlOrHost
        if (isIpOrLocalhost(host)) return host

        val parts = host.split(".")
        if (parts.size <= 2) return host

        // Check for known 2-part ccTLD extensions like co.uk
        val lastTwo = "${parts[parts.size - 2]}.${parts[parts.size - 1]}".lowercase(Locale.ROOT)
        return if (MULTI_PART_TLDS.contains(lastTwo) && parts.size >= 3) {
            "${parts[parts.size - 3]}.${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        } else {
            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        }
    }

    /**
     * Extracts clean hostname from URL string, ignoring scheme, port, query, and path.
     */
    fun getHost(urlOrHost: String): String {
        return try {
            val trimmed = urlOrHost.trim()
            if (trimmed.startsWith("androidapp://")) {
                return trimmed.removePrefix("androidapp://").removeSuffix("/")
            }
            val formattedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else trimmed
            val uri = URI(formattedUrl)
            uri.host?.lowercase(Locale.ROOT) ?: trimmed.lowercase(Locale.ROOT)
        } catch (_: Exception) {
            urlOrHost.lowercase(Locale.ROOT).trim()
        }
    }

    /**
     * Extracts host and port (e.g. "192.168.1.50:8080" or "example.com:443").
     */
    fun getHostAndPort(url: String): String {
        return try {
            val trimmed = url.trim()
            val formattedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else trimmed
            val uri = URI(formattedUrl)
            val host = uri.host?.lowercase(Locale.ROOT) ?: return trimmed
            val port = uri.port
            if (port != -1 && port != 80 && port != 443) {
                "$host:$port"
            } else {
                host
            }
        } catch (_: Exception) {
            url.trim()
        }
    }

    /**
     * Matches a target (requested) URL or package against a stored vault URL.
     * Enforces Postmortem Fix #2: If the host is an IP address or localhost and mode is BASE_DOMAIN,
     * it automatically promotes to EXACT port/host matching to prevent home lab cross-talk.
     */
    fun isMatch(
        vaultUrl: String,
        requestedUrlOrPackage: String,
        matchMode: UriMatchMode = UriMatchMode.BASE_DOMAIN
    ): Boolean {
        if (matchMode == UriMatchMode.NEVER) return false
        val vUrl = vaultUrl.trim()
        val rTarget = requestedUrlOrPackage.trim()
        if (vUrl.isBlank() || rTarget.isBlank()) return false

        // Check for Android App Package matching (e.g. androidapp://com.github.android)
        val vIsApp = vUrl.startsWith("androidapp://")
        val rIsApp = rTarget.startsWith("androidapp://")
        if (vIsApp || rIsApp) {
            if (vIsApp && rIsApp) {
                val vPkg = vUrl.removePrefix("androidapp://").trimEnd('/')
                val rPkg = rTarget.removePrefix("androidapp://").trimEnd('/')
                return vPkg.equals(rPkg, ignoreCase = true)
            }
            // One is an Android app and the other is a web URL - strictly reject cross-matching
            return false
        }

        val requestedHost = getHost(rTarget)
        val isLocalOrIp = isIpOrLocalhost(requestedHost)

        // Postmortem Fix #2: Promote to host/port matching for IP/localhost
        val effectiveMode = if (matchMode == UriMatchMode.BASE_DOMAIN && isLocalOrIp) {
            UriMatchMode.EXACT
        } else {
            matchMode
        }

        return when (effectiveMode) {
            UriMatchMode.BASE_DOMAIN -> {
                val vaultBase = getEffectiveDomain(vUrl)
                val reqBase = getEffectiveDomain(rTarget)
                vaultBase.equals(reqBase, ignoreCase = true)
            }
            UriMatchMode.HOST -> {
                val vaultHost = getHost(vUrl)
                vaultHost.equals(requestedHost, ignoreCase = true)
            }
            UriMatchMode.EXACT -> {
                val vHostPort = getHostAndPort(vUrl)
                val rHostPort = getHostAndPort(rTarget)
                vHostPort.equals(rHostPort, ignoreCase = true)
            }
            UriMatchMode.STARTS_WITH -> {
                rTarget.startsWith(vUrl, ignoreCase = true)
            }
            UriMatchMode.NEVER -> false
        }
    }

    // TODO(jules): Implement matchesAnyUri(primaryUrl, urisJson, requestedUrlOrPackage, matchMode) per .jules/tasks/task-1-attachments-and-multi-uri.md
}
