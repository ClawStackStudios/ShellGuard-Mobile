package com.clawstack.shellguard.domain.matcher

import kotlinx.serialization.Serializable

@Serializable
enum class UriMatchMode {
    /**
     * Matches base domain (eTLD+1), ignoring subdomains, ports, and paths. Default.
     * Example: https://mail.google.com/ matches https://accounts.google.com/
     */
    BASE_DOMAIN,

    /**
     * Matches full host including subdomains.
     * Example: mail.google.com != accounts.google.com
     */
    HOST,

    /**
     * Requires exact match of protocol, host, port, and path.
     * Essential for local IP home labs (e.g. http://192.168.1.50:8080 != http://192.168.1.50:9000).
     */
    EXACT,

    /**
     * Matches if current page URL starts with the saved URI prefix.
     */
    STARTS_WITH,

    /**
     * Explicitly disables autofill suggestions for this specific URI.
     */
    NEVER
}
