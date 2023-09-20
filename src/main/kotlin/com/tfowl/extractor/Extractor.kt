package com.tfowl.extractor

import com.tfowl.netlog.*
import io.ktor.http.*
import io.ktor.util.*
import okio.ByteString.Companion.decodeBase64
import java.time.Instant

val PHASE_BEGIN = Phase("PHASE_BEGIN")
val PHASE_END = Phase("PHASE_END")

val URL_REQUEST = SourceType("URL_REQUEST")
val HTTP_STREAM_CONTROLLER_JOB = SourceType("HTTP_STREAM_CONTROLLER_JOB")
val HTTP_STREAM_JOB = SourceType("HTTP_STREAM_JOB")
val SOCKET = SourceType("SOCKET")

val URL_REQUEST_DELEGATE_RESPONSE_STARTED = EventType("URL_REQUEST_DELEGATE_RESPONSE_STARTED")
val URL_REQUEST_START_JOB = EventType("URL_REQUEST_START_JOB")
val HTTP_TRANSACTION_READ_BODY = EventType("HTTP_TRANSACTION_READ_BODY")
val URL_REQUEST_REDIRECTED = EventType("URL_REQUEST_REDIRECTED")
val HTTP_STREAM_JOB_CONTROLLER_BOUND = EventType("HTTP_STREAM_JOB_CONTROLLER_BOUND")
val HTTP_STREAM_REQUEST_BOUND_TO_JOB = EventType("HTTP_STREAM_REQUEST_BOUND_TO_JOB")
val HTTP_STREAM_JOB_CONTROLLER_PROXY_SERVER_RESOLVED = EventType("HTTP_STREAM_JOB_CONTROLLER_PROXY_SERVER_RESOLVED")
val HTTP_TRANSACTION_SEND_REQUEST = EventType("HTTP_TRANSACTION_SEND_REQUEST")
val REQUEST_ALIVE = EventType("REQUEST_ALIVE")
val HTTP_TRANSACTION_READ_HEADERS = EventType("HTTP_TRANSACTION_READ_HEADERS")
val URL_REQUEST_FAKE_RESPONSE_HEADERS_CREATED = EventType("URL_REQUEST_FAKE_RESPONSE_HEADERS_CREATED")
val URL_REQUEST_JOB_BYTES_READ = EventType("URL_REQUEST_JOB_BYTES_READ")
val SOCKET_POOL_BOUND_TO_SOCKET = EventType("SOCKET_POOL_BOUND_TO_SOCKET")
val TCP_CONNECT = EventType("TCP_CONNECT")
val SSL_CERTIFICATES_RECEIVED = EventType("SSL_CERTIFICATES_RECEIVED")
val HTTP_CACHE_READ_DATA = EventType("HTTP_CACHE_READ_DATA")


fun Event.isSendRequestHeaders(): Boolean = type in setOf(
    EventType("HTTP_TRANSACTION_HTTP2_SEND_REQUEST_HEADERS"),
    EventType("HTTP_TRANSACTION_QUIC_SEND_REQUEST_HEADERS"),
    EventType("HTTP_TRANSACTION_SEND_REQUEST_HEADERS"),
)

fun Event.isReadResponseHeaders(): Boolean = type in setOf(
    EventType("HTTP_TRANSACTION_READ_RESPONSE_HEADERS"),
    EventType("HTTP_TRANSACTION_READ_EARLY_HINTS_RESPONSE_HEADERS"),
    EventType("URL_REQUEST_FAKE_RESPONSE_HEADERS_CREATED"), // TODO: Verify this works
)

fun Event.isReadBytes(): Boolean = type in setOf(
    EventType("URL_REQUEST_JOB_FILTERED_BYTES_READ"),
//    EventType.URL_REQUEST_JOB_BYTES_READ
)

private fun NetLog.structuredEvents(sourcePredicate: (Event.Source) -> Boolean = { true }): Map<Event.Source, List<Event>> =
    buildMap<Event.Source, MutableList<Event>> {
        events.forEach { event ->
            if (sourcePredicate(event.source)) {
                computeIfAbsent(event.source) { mutableListOf() } += event
            }
        }
    }


private fun buildTransactions(
    constants: Constants,
    sourceToEvents: Map<Event.Source, List<Event>>,
    sourceLookup: Map<Int, Event.Source>,
    events: List<Event>,
): List<HttpTransaction> {
    fun dependenciesEvents(event: Event): List<Event> {
        val dependencies = event.decodeParams<Params.Dependencies>() ?: error("Could not read dependencies from $event")
        val source = sourceLookup[dependencies.sourceDependency.id]
            ?: error("Could not find source dependency for $dependencies")
        return sourceToEvents[source] ?: error("Could not find events for source $source")
    }

    val builder = MultipleTransactionsBuilder()

    events.forEach { event ->
        when {
            event.type == HTTP_STREAM_JOB_CONTROLLER_BOUND                    -> {
                val streamJobControllerEvents = dependenciesEvents(event)

                val proxy =
                    streamJobControllerEvents.firstOrNull { it.type == HTTP_STREAM_JOB_CONTROLLER_PROXY_SERVER_RESOLVED }
                        ?.decodeParams<Params.ProxyServerResolved>()
                        ?.proxy

                proxy?.let(builder.current::proxy)
            }

            event.type == HTTP_STREAM_REQUEST_BOUND_TO_JOB                    -> {
                val streamJobEvents = dependenciesEvents(event)

                val socketEvents = streamJobEvents.firstOrNull { it.type == SOCKET_POOL_BOUND_TO_SOCKET }
                    ?.let { evtBoundToSocket -> dependenciesEvents(evtBoundToSocket) }

                if (socketEvents != null) {

                    // TODO: Support quic / udp
                    val tcpConnect = socketEvents.firstOrNull { it.type == TCP_CONNECT && it.phase == PHASE_END }
                        ?.decodeParams<Params.TcpConnectFinished>()

                    tcpConnect?.let { tcp ->
                        builder.current.localAddress(tcp.localAddress).remoteAddress(tcp.remoteAddress)
                    }

                    val certificatesReceived = socketEvents.firstOrNull { it.type == SSL_CERTIFICATES_RECEIVED }
                        ?.decodeParams<Params.CertificatesReceived>()
                    certificatesReceived?.let { received ->
                        builder.current.certificates(received.certificates)
                    }
                }
            }

            event.type == URL_REQUEST_START_JOB && event.phase == PHASE_BEGIN -> {
                builder.next()
                val job = event.decodeParams<Params.UrlRequestStartJob>()!!
                builder.current.request
                    .url(Url(job.url))
                    .method(HttpMethod(job.method))
                    .start(Instant.ofEpochMilli(event.time.toLong() + constants.timeTickOffset))
            }

            event.isSendRequestHeaders()                                      -> {
                builder.current.request.headers(parseHeaders(event.decodeParams<Params.SendHeaders>()!!.headers))
            }

            event.isReadResponseHeaders()                                     -> {
                val headers = event.decodeParams<Params.ReadHeaders>()!!.headers
                headers.firstOrNull()?.let { builder.current.response.statusLine(it) }
                builder.current.response.start(Instant.ofEpochMilli(event.time.toLong() + constants.timeTickOffset))
                builder.current.response.headers(parseHeaders(headers))
            }

            event.type == HTTP_CACHE_READ_DATA                                -> {
                if (builder.current.response.statusLine == null) {
                    // TODO: Http version etc
                    builder.current.response.statusLine("HTTP/1.1 200 (cache)")
                }
            }

            event.isReadBytes()                                               -> {
                builder.current.response.appendContent(event.decodeParams<Params.ReadBytes>()!!.bytes.decodeBase64()!!)
            }
        }
    }

    return builder.build()
}

internal fun parseHeaders(headers: List<String>): StringValues = StringValues.build {
    for (header in headers) {
        if (header.startsWith(":")) continue
        if (header.startsWith("HTTP/")) continue

        if (':' in header) {
            val (key, value) = header.split(':', limit = 2)
            append(key.trim(), value.trim())
        } else {
            append(header.trim(), "")
        }
    }
}

fun extractHttpTransactions(log: NetLog): List<HttpTransaction> {
    val urlRequests = log.structuredEvents { it.type == URL_REQUEST }
    val sourceToEvents = log.structuredEvents()
    val sourceLookup = sourceToEvents.keys.associateBy { it.id }

    return urlRequests.flatMap { (source, events) ->
        buildTransactions(log.constants, sourceToEvents, sourceLookup, events)
    }
}