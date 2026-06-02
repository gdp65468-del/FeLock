package com.selflock.app.vpn

import java.nio.ByteBuffer

data class DnsPacket(
    val transactionId: Short,
    val flags: Short,
    val questionCount: Short,
    val answerCount: Short,
    val queriedDomain: String,
    val queryType: Short,
    val rawBytes: ByteArray
) {
    val isQuery: Boolean get() = (flags.toInt() and 0x8000) == 0

    companion object {
        fun parse(data: ByteArray): DnsPacket? {
            if (data.size < 12) return null
            val buffer = ByteBuffer.wrap(data)
            val txId = buffer.getShort(0)
            val flags = buffer.getShort(2)
            val qdCount = buffer.getShort(4)
            val anCount = buffer.getShort(6)

            if (qdCount < 1) return DnsPacket(txId, flags, qdCount, anCount, "", 0, data)

            var pos = 12
            val labels = mutableListOf<String>()
            while (pos < data.size) {
                val len = data[pos].toInt() and 0xFF
                if (len == 0) {
                    pos++
                    break
                }
                if (len > 63 || pos + 1 + len > data.size) break
                labels.add(String(data, pos + 1, len))
                pos += 1 + len
            }
            val domain = labels.joinToString(".")
            val qType = if (pos + 2 <= data.size) buffer.getShort(pos) else 0

            return DnsPacket(txId, flags, qdCount, anCount, domain, qType, data)
        }
    }
}

object DnsResponseBuilder {
    fun buildSinkholeResponse(query: DnsPacket): ByteArray {
        val domain = query.queriedDomain
        val domainBytes = encodeDomain(domain)
        val responseSize = 12 + domainBytes.size + 4 + 16
        val buffer = ByteBuffer.allocate(responseSize)

        buffer.putShort(query.transactionId)
        buffer.putShort(0x8180.toShort())
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putShort(0.toShort())
        buffer.putShort(0.toShort())

        buffer.put(domainBytes)
        buffer.put(0.toByte())
        buffer.putShort(query.queryType)
        buffer.putShort(1.toShort())

        buffer.putShort(0xC00C.toShort())
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(1)
        buffer.putShort(4.toShort())
        buffer.put(0.toByte())
        buffer.put(0.toByte())
        buffer.put(0.toByte())
        buffer.put(0.toByte())

        return buffer.array()
    }

    private fun encodeDomain(domain: String): ByteArray {
        val parts = domain.split(".")
        val result = mutableListOf<Byte>()
        for (part in parts) {
            result.add(part.length.toByte())
            result.addAll(part.toByteArray().toList())
        }
        return result.toByteArray()
    }
}
