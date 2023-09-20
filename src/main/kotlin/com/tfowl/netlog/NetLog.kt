package com.tfowl.netlog

import com.tfowl.swap
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import okio.BufferedSource
import okio.buffer
import okio.source
import java.nio.file.Path
import kotlin.io.path.inputStream

@JvmInline
value class EventType(val name: String)

@JvmInline
value class SourceType(val name: String)

@JvmInline
value class Phase(val name: String)

@Serializable
data class NetLog(
    val constants: Constants,
    val events: List<Event>,
    val polledData: JsonElement,
)

@Serializable
data class Constants(
    val activeFieldTrialGroups: List<String>,
    val addressFamily: Map<String, Int>,
    val certPathBuilderDigestPolicy: Map<String, Int>,
    val certStatusFlag: Map<String, Int>,
    val certVerifierFlags: Map<String, Int>,
    val certificateTrustType: Map<String, Int>,
    val clientInfo: JsonObject,
    val dnsQueryType: Map<String, Int>? = null,
    val loadFlag: Map<String, Int>,
    val loadState: Map<String, Int>,
    val logCaptureMode: String? = null, // e.g. "Everything"
    val logEventPhase: Map<String, Int>,
    val logEventTypes: Map<String, Int>,
    val logFormatVersion: Int, // All this is based off 1
    val logSourceType: Map<String, Int>,
    val netError: Map<String, Int>,
    val quicError: Map<String, Int>,
    val quicRstStreamError: Map<String, Int>,
    val secureDnsMode: Map<String, Int>? = null,
    val timeTickOffset: Long,
)


@Serializable
data class Event(
    val time: String,
    @Contextual
    val type: EventType,
    val source: Source,
    @Contextual
    val phase: Phase,
    val params: JsonObject? = null,
) {
    @Serializable
    data class Source(
        val id: Int,
        @Contextual
        val type: SourceType,
        @SerialName("start_time") val startTime: String,
    )
}