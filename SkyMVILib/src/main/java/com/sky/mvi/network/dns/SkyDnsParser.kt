package com.sky.mvi.network.dns

import com.sky.mvi.SkyMVILib
import com.sky.mvi.ext.util.logI
import com.sky.mvi.ext.util.logW
import okhttp3.Dns
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer

/**
 * @Class: SkyDnsParser
 * @Author: Henry
 * @Date: 2026/6/30
 * @Description: 自定义DNS解析器，系统DNS失败后自动切换备用DNS服务器
 */

class SkyDnsParser(
    private val fallbackServers: List<InetAddress> = listOf(
        InetAddress.getByName("8.8.8.8"),       // Google DNS
        InetAddress.getByName("8.8.4.4"),       // Google DNS
        InetAddress.getByName("114.114.114.114"), // 国内 DNS
    )
) : Dns {

    init {
        SkyMVILib.requireInit()
    }

    companion object {
        private const val TAG = "SkyDnsParser"
        private const val DNS_PORT = 53
        private const val DNS_TIMEOUT_MS = 3000
    }

    override fun lookup(hostname: String): List<InetAddress> {
        // 优先使用系统 DNS
        try {
            return Dns.SYSTEM.lookup(hostname)
        } catch (e: UnknownHostException) {
            "系统 DNS 解析失败，启用备用 DNS: $hostname".logW(TAG)
        }

        // 系统 DNS 失败，依次尝试备用 DNS 服务器
        for (dnsServer in fallbackServers) {
            try {
                val addresses = resolveByUdp(hostname, dnsServer)
                if (addresses.isNotEmpty()) {
                    "备用 DNS [${dnsServer.hostAddress}] 解析成功: $hostname".logI(TAG)
                    return addresses
                }
            } catch (e: Exception) {
                "备用 DNS [${dnsServer.hostAddress}] 解析失败: ${e.message}".logW(TAG)
            }
        }

        throw UnknownHostException("所有 DNS 服务器均无法解析: $hostname")
    }

    /**
     * 通过 UDP 向指定 DNS 服务器发送解析请求
     */
    private fun resolveByUdp(hostname: String, dnsServer: InetAddress): List<InetAddress> {
        val socket = DatagramSocket()
        try {
            socket.soTimeout = DNS_TIMEOUT_MS

            val query = buildDnsQuery(hostname)
            val sendPacket = DatagramPacket(query, query.size, dnsServer, DNS_PORT)
            socket.send(sendPacket)

            val response = ByteArray(512)
            val receivePacket = DatagramPacket(response, response.size)
            socket.receive(receivePacket)

            return parseDnsResponse(response, receivePacket.length)
        } finally {
            socket.close()
        }
    }

    /**
     * 构建标准 DNS 查询报文
     */
    private fun buildDnsQuery(hostname: String): ByteArray {
        val buffer = ByteBuffer.allocate(512)

        // Header
        buffer.putShort(0x1234.toShort())  // Transaction ID
        buffer.putShort(0x0100.toShort())  // Flags: standard query, recursion desired
        buffer.putShort(1)                  // Questions: 1
        buffer.putShort(0)                  // Answer RRs: 0
        buffer.putShort(0)                  // Authority RRs: 0
        buffer.putShort(0)                  // Additional RRs: 0

        for (part in hostname.split(".")) {
            val bytes = part.toByteArray()
            buffer.put(bytes.size.toByte())
            buffer.put(bytes)
        }
        buffer.put(0.toByte())
        buffer.putShort(1)
        buffer.putShort(1)

        val result = ByteArray(buffer.position())
        buffer.rewind()
        buffer.get(result)
        return result
    }

    /**
     * 解析 DNS 响应报文，提取 A 记录中的 IP 地址
     */
    private fun parseDnsResponse(response: ByteArray, length: Int): List<InetAddress> {
        if (length < 12) return emptyList()

        val buffer = ByteBuffer.wrap(response, 0, length)
        val flags = buffer.getShort(2).toInt() and 0xFFFF
        val rcode = flags and 0x000F
        if (rcode != 0) return emptyList() // 非 NOERROR 响应

        val questionCount = buffer.getShort(4).toInt() and 0xFFFF
        val answerCount = buffer.getShort(6).toInt() and 0xFFFF

        // 跳过 header (12 bytes)
        var pos = 12

        // 跳过所有 Question 段
        for (i in 0 until questionCount) {
            pos = skipDnsName(response, pos)
            pos += 4 // QTYPE (2) + QCLASS (2)
        }

        // 解析 Answer 段中的 A 记录
        val addresses = mutableListOf<InetAddress>()
        for (i in 0 until answerCount) {
            if (pos >= length) break

            pos = skipDnsName(response, pos)
            if (pos + 10 > length) break

            val type = ((response[pos].toInt() and 0xFF) shl 8) or (response[pos + 1].toInt() and 0xFF)
            val rdLength = ((response[pos + 8].toInt() and 0xFF) shl 8) or (response[pos + 9].toInt() and 0xFF)
            pos += 10

            if (type == 1 && rdLength == 4) { // A record
                val ipBytes = ByteArray(4)
                System.arraycopy(response, pos, ipBytes, 0, 4)
                addresses.add(InetAddress.getByAddress(ipBytes))
            }
            pos += rdLength
        }

        return addresses
    }

    /**
     * 跳过 DNS 报文中的域名段
     */
    private fun skipDnsName(data: ByteArray, startPos: Int): Int {
        var pos = startPos
        while (pos < data.size) {
            val labelLen = data[pos].toInt() and 0xFF
            if (labelLen == 0) {
                pos++
                break
            }
            if (labelLen and 0xC0 == 0xC0) {
                pos += 2
                break
            }
            pos += 1 + labelLen
        }
        return pos
    }
}