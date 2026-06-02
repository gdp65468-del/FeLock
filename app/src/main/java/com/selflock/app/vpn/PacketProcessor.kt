package com.selflock.app.vpn

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketProcessor @Inject constructor(
    private val dnsInterceptor: DnsInterceptor,
    private val blocklistManager: BlocklistManager
) {
    @Volatile
    private var running = false

    fun start(tunFd: ParcelFileDescriptor, protectedSocket: (DatagramSocket) -> Unit) {
        running = true
        val inputStream = FileInputStream(tunFd.fileDescriptor)
        val outputStream = FileOutputStream(tunFd.fileDescriptor)
        val buffer = ByteArray(32767)

        while (running) {
            try {
                val length = inputStream.read(buffer)
                if (length <= 0) continue

                val packet = IPPacket.parse(buffer, length) ?: continue

                when {
                    packet.isDns() -> dnsInterceptor.handleDnsQuery(packet, outputStream, protectedSocket)
                    packet.isDoT() -> { /* Drop DoT connections */ }
                    packet.isTls() -> handleTlsPacket(packet, outputStream, protectedSocket)
                    else -> forwardPacket(packet, outputStream, protectedSocket)
                }
            } catch (_: Exception) {
                if (!running) break
            }
        }
    }

    fun stop() {
        running = false
    }

    private fun handleTlsPacket(packet: IPPacket, outputStream: FileOutputStream, protectedSocket: (DatagramSocket) -> Unit) {
        if (packet.payload.isNotEmpty()) {
            val clientHello = TlsClientHello.parse(packet.payload)
            if (clientHello?.sniHostname != null) {
                if (DoHBlocklist.isDoHDomain(clientHello.sniHostname)) {
                    return
                }
            }
        }
        if (DoHBlocklist.isDoHIp(packet.destIp.hostAddress ?: "")) {
            return
        }
        forwardPacket(packet, outputStream, protectedSocket)
    }

    private fun forwardPacket(packet: IPPacket, outputStream: FileOutputStream, protectedSocket: (DatagramSocket) -> Unit) {
        try {
            if (packet.protocol == IPPacket.PROTOCOL_UDP) {
                val socket = DatagramSocket()
                protectedSocket(socket)
                val udpPayload = packet.payload
                val destPacket = java.net.DatagramPacket(
                    udpPayload, udpPayload.size,
                    packet.destIp, packet.destPort
                )
                socket.send(destPacket)
                socket.close()
            }
        } catch (_: Exception) {
        }
    }
}
