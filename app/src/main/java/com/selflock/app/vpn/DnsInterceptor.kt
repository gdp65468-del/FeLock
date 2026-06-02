package com.selflock.app.vpn

import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsInterceptor @Inject constructor(
    private val blocklistManager: BlocklistManager
) {
    private val upstreamDns = InetAddress.getByName("8.8.8.8")
    private val upstreamPort = 53

    fun handleDnsQuery(packet: IPPacket, tunOutputStream: FileOutputStream, protectSocket: (DatagramSocket) -> Unit = {}) {
        val dnsPacket = DnsPacket.parse(packet.payload) ?: return

        if (!dnsPacket.isQuery) return

        val domain = dnsPacket.queriedDomain
        if (domain.isEmpty()) return

        if (blocklistManager.isDomainBlocked(domain) || DoHBlocklist.isDoHDomain(domain)) {
            val dnsResponse = DnsResponseBuilder.buildSinkholeResponse(dnsPacket)
            val responseIpPacket = buildDnsResponsePacket(packet, dnsResponse)
            tunOutputStream.write(responseIpPacket)
            tunOutputStream.flush()
        } else {
            forwardDnsQuery(packet, dnsPacket, tunOutputStream, protectSocket)
        }
    }

    private fun forwardDnsQuery(originalPacket: IPPacket, dnsPacket: DnsPacket, tunOutputStream: FileOutputStream, protectSocket: (DatagramSocket) -> Unit) {
        try {
            val socket = DatagramSocket()
            protectSocket(socket)
            socket.soTimeout = 5000
            val queryBytes = dnsPacket.rawBytes
            val request = DatagramPacket(queryBytes, queryBytes.size, upstreamDns, upstreamPort)
            socket.send(request)

            val responseBuffer = ByteArray(4096)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(response)
            socket.close()

            val dnsResponseBytes = responseBuffer.copyOf(response.length)
            val responseIpPacket = buildDnsResponsePacket(originalPacket, dnsResponseBytes)
            tunOutputStream.write(responseIpPacket)
            tunOutputStream.flush()
        } catch (_: Exception) {
        }
    }

    private fun buildDnsResponsePacket(originalPacket: IPPacket, dnsResponse: ByteArray): ByteArray {
        val ipHeaderSize = 20
        val udpHeaderSize = 8
        val totalLength = ipHeaderSize + udpHeaderSize + dnsResponse.size
        val buffer = ByteBuffer.allocate(totalLength)

        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0)
        buffer.putShort(0x4000.toShort())
        buffer.put(64.toByte())
        buffer.put(17.toByte())
        buffer.putShort(0)
        buffer.put(originalPacket.destIp.address)
        buffer.put(originalPacket.sourceIp.address)

        buffer.putShort(originalPacket.destPort.toShort())
        buffer.putShort(originalPacket.sourcePort.toShort())
        buffer.putShort((udpHeaderSize + dnsResponse.size).toShort())
        buffer.putShort(0)

        buffer.put(dnsResponse)

        val result = buffer.array()
        val checksum = calculateIpChecksum(result, ipHeaderSize)
        result[10] = (checksum shr 8).toByte()
        result[11] = checksum.toByte()

        return result
    }

    private fun calculateIpChecksum(header: ByteArray, headerLength: Int): Int {
        var sum = 0
        for (i in 0 until headerLength step 2) {
            if (i == 10) continue
            sum += ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
