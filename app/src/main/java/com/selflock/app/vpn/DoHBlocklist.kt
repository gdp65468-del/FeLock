package com.selflock.app.vpn

object DoHBlocklist {
    val domains: Set<String> = setOf(
        "dns.google", "dns.google.com", "google-public-dns-a.google.com", "google-public-dns-b.google.com",
        "cloudflare-dns.com", "one.one.one.one", "1.1.1.1", "1.0.0.1",
        "dns.quad9.net", "dns9.quad9.net", "dns10.quad9.net", "dns11.quad9.net",
        "dns.nextdns.io", "doh.nextdns.io",
        "doh.opendns.com", "dns.opendns.com",
        "dns.adguard.com", "doh.adguard.com",
        "dns.dnslify.com", "doh.dnslify.com",
        "mozilla.cloudflare-dns.com",
        "dns.cloudflare.com",
        "doh.cleanbrowsing.org",
        "dns.dnssb.cn", "doh.dnssb.cn",
        "doh.pub", "dns.pub",
        "dns.alidns.com", "doh.alidns.com",
        "doh.360.cn", "dot.360.cn",
        "dns.twnic.tw", "101.101.101.101",
        "doh.libredns.gr",
        "dns.switch.ch",
        "doh.centraleu.pi-dns.com", "doh.northeu.pi-dns.com",
        "doh.westus.pi-dns.com", "doh.eastus.pi-dns.com",
        "ordns.he.net"
    )

    val ips: Set<String> = setOf(
        "8.8.8.8", "8.8.4.4",
        "1.1.1.1", "1.0.0.1",
        "9.9.9.9", "149.112.112.112",
        "208.67.222.222", "208.67.220.220",
        "185.228.168.9", "185.228.169.9",
        "76.76.2.0", "76.76.10.0",
        "94.140.14.14", "94.140.15.15"
    )

    fun isDoHDomain(domain: String): Boolean {
        val normalized = domain.lowercase().removeSuffix(".")
        return domains.any { normalized == it || normalized.endsWith(".$it") }
    }

    fun isDoHIp(ip: String): Boolean = ip in ips
}
