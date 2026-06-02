package com.selflock.app.vpn

import java.net.InetAddress
import java.nio.ByteBuffer

data class IPPacket(
    val version: Int,
    val headerLength: Int,
    val totalLength: Int,
    val protocol: Int,
    val sourceIp: InetAddress,
    val destIp: InetAddress,
    val sourcePort: Int,
    val destPort: Int,
    val payload: ByteArray,
    val rawBytes: ByteArray
) {
    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17

        fun parse(data: ByteArray, length: Int): IPPacket? {
            if (length < 20) return null
            val buffer = ByteBuffer.wrap(data, 0, length)
            val versionAndIhl = buffer.get().toInt() and 0xFF
            val version = versionAndIhl shr 4
            if (version != 4) return null
            val ihl = versionAndIhl and 0x0F
            val headerLength = ihl * 4
            if (length < headerLength) return null
            val totalLength = buffer.getShort(2).toInt() and 0xFFFF
            val protocol = buffer.get(9).toInt() and 0xFF
            val srcIp = ByteArray(4).also { buffer.position(12); buffer.get(it) }
            val dstIp = ByteArray(4).also { buffer.position(16); buffer.get(it) }

            var srcPort = 0
            var dstPort = 0
            val payload: ByteArray
            if (protocol == PROTOCOL_TCP || protocol == PROTOCOL_UDP) {
                if (length >= headerLength + 4) {
                    srcPort = buffer.getShort(headerLength).toInt() and 0xFFFF
                    dstPort = buffer.getShort(headerLength + 2).toInt() and 0xFFFF
                }
                val payloadStart = headerLength + if (protocol == PROTOCOL_TCP) {
                    if (length >= headerLength + 13) {
                        val dataOffset = ((buffer.get(headerLength + 12).toInt() and 0xFF) shr 4) * 4
                        dataOffset
                    } else 20
                } else 8
                payload = if (length > headerLength + payloadStart) {
                    data.copyOfRange(headerLength + payloadStart, length)
                } else ByteArray(0)
            } else {
                payload = if (length > headerLength) data.copyOfRange(headerLength, length) else ByteArray(0)
            }

            return IPPacket(
                version = version,
                headerLength = headerLength,
                totalLength = totalLength,
                protocol = protocol,
                sourceIp = InetAddress.getByAddress(srcIp),
                destIp = InetAddress.getByAddress(dstIp),
                sourcePort = srcPort,
                destPort = dstPort,
                payload = payload,
                rawBytes = data.copyOf(length)
            )
        }
    }

    fun isDns(): Boolean = destPort == 53 || sourcePort == 53
    fun isTls(): Boolean = destPort == 443
    fun isDoT(): Boolean = destPort == 853
}
