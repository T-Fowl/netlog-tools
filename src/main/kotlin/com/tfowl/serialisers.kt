package com.tfowl

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.Sink
import okio.gzip
import java.time.Instant

open class IntoTryFromStringSerialiser<T>(
    val name: String,
    val into: (T) -> String,
    val tryFrom: (String) -> T?,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val str = decoder.decodeString()
        return tryFrom(str) ?: error("Could not decode $name from string \"$str\"")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(into(value))
    }
}

class IsoInstantSerialiser : IntoTryFromStringSerialiser<Instant>(
    "java.time.Instant",
    { it.toString() },
    { Instant.parse(it) }
)

class BufferSerialiser : IntoTryFromStringSerialiser<Buffer>(
    "okio.Buffer",
    { it.copy().readByteString().base64() },
    { it.decodeBase64()?.let { bytes -> Buffer().write(bytes) } }
)

class HttpMethodSerialiser : IntoTryFromStringSerialiser<HttpMethod>(
    "ktor.HttpMethod",
    { it.value },
    { HttpMethod.parse(it) },
)

class UrlSerialiser : IntoTryFromStringSerialiser<Url>(
    "ktor.Url",
    { it.toString() },
    { Url(it) },
)

class HeadersSerialiser : KSerializer<Headers> {
    override val descriptor: SerialDescriptor = serialDescriptor<Map<String, List<String>>>()

    override fun deserialize(decoder: Decoder): Headers {
        val map: Map<String, List<String>> = decoder.decodeSerializableValue(serializer())
        return HeadersImpl(map)

//        val flat = decoder.decodeSerializableValue(serializer<List<List<String>>>())
//        return Headers.build {
//            for ((k, v) in flat) {
//                append(k, v)
//            }
//        }
    }

    override fun serialize(encoder: Encoder, value: Headers) {
        encoder.encodeSerializableValue(serializer(), value.toMap())
//
//        val flat = value.flattenEntries().map { p -> listOf(p.first, p.second) }
//        encoder.encodeSerializableValue(serializer(), flat)
    }
}