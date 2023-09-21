package com.tfowl.netlogtools.cli.formats

import com.tfowl.netlogtools.*
import com.tfowl.netlogtools.cli.formats.har.toHar
import com.tfowl.netlogtools.extractor.HttpTransaction
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.encodeToBufferedSink
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import okio.BufferedSink

interface OutputFormat {
    fun write(sink: BufferedSink, transactions: List<HttpTransaction>)
}

class JsonOutputFormat(prettyPrint: Boolean = false) : OutputFormat {

    private val json = Json {
        this.prettyPrint = prettyPrint
        serializersModule = SerializersModule {
            contextual(BufferSerialiser())
            contextual(HttpMethodSerialiser())
            contextual(UrlSerialiser())
            contextual(HeadersSerialiser())
            contextual(IsoInstantSerialiser())
        }
    }

    @ExperimentalSerializationApi
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        json.encodeToBufferedSink(transactions, sink)
    }
}

class MitmOutputFormat : OutputFormat {
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        TODO("Not yet implemented")
    }
}

class HarOutputFormat : OutputFormat {
    private val json = Json {
        prettyPrint = true
    }

    @ExperimentalSerializationApi
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        val har = transactions.toHar()
        json.encodeToBufferedSink(har, sink)
    }
}

class WarcOutputFormat : OutputFormat {
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        TODO("Not yet implemented")
    }
}
