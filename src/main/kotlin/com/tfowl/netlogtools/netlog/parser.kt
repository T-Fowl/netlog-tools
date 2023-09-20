package com.tfowl.netlogtools.netlog

import com.tfowl.netlogtools.swap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.contextual
import okio.BufferedSource
import okio.buffer
import okio.source
import java.nio.file.Path


class NetLogEnumSerialiser<E>(
    private val values: Map<String, Int>,
    private val constructor: (String) -> E,
    private val destructor: (E) -> String,
) : KSerializer<E> {
    private val lookup = values.swap()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NetLogEnum", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): E {
        return constructor(lookup[decoder.decodeInt()]!!)
    }

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeInt(values[destructor(value)]!!)
    }
}

fun loadNetLog(path: Path): NetLog {
    return path.source().buffer().use(::loadNetLog)
}

fun loadNetLog(source: BufferedSource): NetLog {
    val baseJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val line = source.peek().readUtf8LineStrict()
    return when {
        line.startsWith("""{"constants":""") && line.endsWith("""},""") -> {
            loadNetLogSequentially(baseJson, source)
        }

        else                                                            -> {
            loadNetLogCompletely(baseJson, source)
        }
    }
}

private fun SerializersModuleBuilder.addConstants(constants: Constants) {
    contextual(NetLogEnumSerialiser(constants.logEventTypes, ::EventType, EventType::name))
    contextual(NetLogEnumSerialiser(constants.logSourceType, ::SourceType, SourceType::name))
    contextual(NetLogEnumSerialiser(constants.logEventPhase, ::Phase, Phase::name))
}

@OptIn(ExperimentalSerializationApi::class)
private fun loadNetLogCompletely(baseJson: Json, source: BufferedSource): NetLog {
    val root = baseJson.decodeFromBufferedSource<JsonObject>(source)
    val constants = baseJson.decodeFromJsonElement<Constants>(root["constants"]!!)

    val json = Json(baseJson) {
        serializersModule = SerializersModule {
            addConstants(constants)
        }
    }

    return json.decodeFromJsonElement<NetLog>(root)
}

private fun loadNetLogSequentially(baseJson: Json, source: BufferedSource): NetLog {
    val line = source.readUtf8LineStrict()

    val constants = baseJson.decodeFromString<Constants>(
        line.removeSurrounding("""{"constants":""", """,""")
    )

    val json = Json(baseJson) {
        serializersModule = SerializersModule {
            addConstants(constants)
        }
    }


    val eventsStart = source.peek().readUtf8Line()
    assert(eventsStart == """"events": [""") { "Expected events start but found $eventsStart" }

    // Consume the events key
    source.readUtf8Line()

    // Doing this line-by-line was measurably faster than loading the whole file at once
    val events = mutableListOf<Event>()
    while (!source.exhausted()) {
        val line = source.readUtf8LineStrict()

        val event = json.decodeFromString<Event>(
            line.removeSuffix(",").removeSuffix("]")
        )

        events.add(event)

        if (line.endsWith("],")) break
    }

    val polledData = json.decodeFromString<JsonElement>(
        source.readUtf8Line()!!.removeSurrounding(""""polledData": """, "")
    )

    assert("}" == source.readUtf8Line())


    return NetLog(constants, events, polledData)
}