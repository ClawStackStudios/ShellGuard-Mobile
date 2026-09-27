package com.clawstack.shellguard.domain.matcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMatcherTest {

    @Test
    fun testEffectiveDomain_standardUrls() {
        assertEquals("google.com", DomainMatcher.getEffectiveDomain("https://www.google.com/search?q=test"))
        assertEquals("google.com", DomainMatcher.getEffectiveDomain("accounts.google.com"))
        assertEquals("github.com", DomainMatcher.getEffectiveDomain("https://github.com/login"))
    }

    @Test
    fun testEffectiveDomain_multiPartCctld() {
        assertEquals("amazon.co.uk", DomainMatcher.getEffectiveDomain("https://subdomain.amazon.co.uk/item"))
        assertEquals("service.gov.au", DomainMatcher.getEffectiveDomain("https://my.service.gov.au"))
        assertEquals("globo.com.br", DomainMatcher.getEffectiveDomain("https://noticias.globo.com.br"))
    }

    @Test
    fun testMatchMode_baseDomain() {
        val vaultUrl = "https://accounts.google.com"
        val requestedUrl = "https://myaccount.google.com/security"
        assertTrue(DomainMatcher.isMatch(vaultUrl, requestedUrl, UriMatchMode.BASE_DOMAIN))

        val differentDomain = "https://google.org"
        assertFalse(DomainMatcher.isMatch(vaultUrl, differentDomain, UriMatchMode.BASE_DOMAIN))
    }

    @Test
    fun testMatchMode_host() {
        val vaultUrl = "https://accounts.google.com"
        val matchSameHost = "https://accounts.google.com/signin"
        val differentSubdomain = "https://mail.google.com"

        assertTrue(DomainMatcher.isMatch(vaultUrl, matchSameHost, UriMatchMode.HOST))
        assertFalse(DomainMatcher.isMatch(vaultUrl, differentSubdomain, UriMatchMode.HOST))
    }

    @Test
    fun testMatchMode_ipAndPortSeparation_homeLabProtection() {
        // Enforce Postmortem Fix #2: Port separation on IP addresses even with default BASE_DOMAIN
        val vaultUrl8080 = "http://192.168.1.50:8080/dashboard"
        val request8080 = "http://192.168.1.50:8080/login"
        val request9000 = "http://192.168.1.50:9000/login"

        assertTrue(DomainMatcher.isMatch(vaultUrl8080, request8080, UriMatchMode.BASE_DOMAIN))
        assertFalse("Different ports on local home lab IP must not cross-match",
            DomainMatcher.isMatch(vaultUrl8080, request9000, UriMatchMode.BASE_DOMAIN)
        )
    }

    @Test
    fun testMatchMode_androidAppPackage() {
        val vaultUrl = "androidapp://com.github.android"
        val requestedPkg = "androidapp://com.github.android"
        val otherPkg = "androidapp://com.twitter.android"

        assertTrue(DomainMatcher.isMatch(vaultUrl, requestedPkg))
        assertFalse(DomainMatcher.isMatch(vaultUrl, otherPkg))
    }

    @Test
    fun testMatchMode_never() {
        val vaultUrl = "https://secure.bank.com"
        val reqUrl = "https://secure.bank.com/login"

        assertFalse(DomainMatcher.isMatch(vaultUrl, reqUrl, UriMatchMode.NEVER))
    }
}
