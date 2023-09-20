package com.tfowl.formats

import com.tfowl.*
import com.tfowl.extractor.HttpTransaction
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
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        TODO("Not yet implemented")
    }
}

class WarcOutputFormat : OutputFormat {
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        TODO("Not yet implemented")
    }
}

class CsvOutputFormat : OutputFormat {
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        TODO("Not yet implemented")
    }
}

class SqliteOutputFormat : OutputFormat {
    override fun write(sink: BufferedSink, transactions: List<HttpTransaction>) {
        TODO("Not yet implemented")
    }
}

