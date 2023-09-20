package com.tfowl.netlog

import com.tfowl.netlog.Params.eventParamsJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface EventParams

object Params {
    val eventParamsJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class Dependencies(
        @SerialName("source_dependency")
        val sourceDependency: SourceDependency,
    ): EventParams {
        @Serializable
        data class SourceDependency(val id: Int, val type: Int)
    }

    @Serializable
    data class ProxyServerResolved(
        @SerialName("proxy_server")
        val proxy: String
    ): EventParams

    @Serializable
    data class UrlRequestStartJob(
        val initiator: String,
        val method: String,
        @SerialName("request_type")
        val requestType: String? = null,
        val url: String,
    ) : EventParams

    @Serializable
    data class SendHeaders(val headers: List<String>) : EventParams

    @Serializable
    data class ReadHeaders(val headers: List<String>) : EventParams

    @Serializable
    data class ReadBytes(
        @SerialName("byte_count")
        val byteCount: Long,
        val bytes: String,
    ) : EventParams

    @Serializable
    data class TcpConnectFinished(
        @SerialName("local_address")
        val localAddress: String,
        @SerialName("remote_address")
        val remoteAddress: String,
    ) : EventParams

    @Serializable
    data class CertificatesReceived(val certificates: List<String>) : EventParams
}

inline fun <reified P : EventParams> Event.decodeParams(): P? = params?.decodeParams()
inline fun <reified P : EventParams> JsonElement.decodeParams(): P = eventParamsJson.decodeFromJsonElement(this)