package com.selflock.app.vpn

import java.nio.ByteBuffer

data class TlsClientHello(
    val sniHostname: String?
) {
    companion object {
        fun parse(data: ByteArray): TlsClientHello? {
            if (data.size < 6) return null
            val contentType = data[0].toInt() and 0xFF
            if (contentType != 0x16) return null

            val buffer = ByteBuffer.wrap(data)
            buffer.position(5)
            if (buffer.remaining() < 4) return null

            val handshakeType = buffer.get().toInt() and 0xFF
            if (handshakeType != 0x01) return null

            buffer.position(buffer.position() + 3)
            buffer.position(buffer.position() + 2)
            buffer.position(buffer.position() + 32)

            if (buffer.remaining() < 1) return null
            val sessionIdLen = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + sessionIdLen)

            if (buffer.remaining() < 2) return null
            val cipherSuitesLen = buffer.getShort().toInt() and 0xFFFF
            buffer.position(buffer.position() + cipherSuitesLen)

            if (buffer.remaining() < 1) return null
            val compMethodsLen = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + compMethodsLen)

            if (buffer.remaining() < 2) return null
            val extensionsLen = buffer.getShort().toInt() and 0xFFFF
            val extensionsEnd = buffer.position() + extensionsLen

            while (buffer.position() < extensionsEnd && buffer.remaining() >= 4) {
                val extType = buffer.getShort().toInt() and 0xFFFF
                val extLen = buffer.getShort().toInt() and 0xFFFF
                if (extType == 0x0000) {
                    val sniListLen = buffer.getShort().toInt() and 0xFFFF
                    if (buffer.remaining() < 3) return null
                    val sniType = buffer.get().toInt() and 0xFF
                    val sniLen = buffer.getShort().toInt() and 0xFFFF
                    if (sniType == 0 && buffer.remaining() >= sniLen) {
                        val hostname = ByteArray(sniLen).also { buffer.get(it) }
                        return TlsClientHello(String(hostname))
                    }
                } else {
                    buffer.position(buffer.position() + extLen)
                }
            }
            return TlsClientHello(null)
        }
    }
}
