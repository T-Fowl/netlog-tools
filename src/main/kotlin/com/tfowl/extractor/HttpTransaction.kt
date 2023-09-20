package com.tfowl.extractor

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import okio.Buffer
import okio.ByteString
import java.time.Instant


@Serializable
data class HttpRequest(
    @Contextual
    val start: Instant?,
    @Contextual
    val method: HttpMethod,
    @Contextual
    val url: Url,
    @Contextual
    val headers: Headers,
)

class HttpRequestBuilder {
    var start: Instant? = null
        private set
    var method: HttpMethod? = null
        private set
    var url: Url? = null
        private set
    val headers = HeadersBuilder()

    fun start(start: Instant) = apply { this.start = start }

    fun method(method: HttpMethod) = apply { this.method = method }

    fun url(url: Url) = apply { this.url = url }

    fun headers(headers: StringValues) = apply { this.headers.appendAll(headers) }

    fun build(): HttpRequest = HttpRequest(
        start,
        method ?: error("method not set"),
        url ?: error("url not set"),
        headers.build()
    )
}

@Serializable
data class HttpResponse(
    @Contextual
    val start: Instant?,
    val statusLine: String,
    @Contextual
    val headers: Headers,
    @Contextual
    val content: Buffer?,
)

class HttpResponseBuilder {
    var start: Instant? = null
        private set
    var statusLine: String? = null
        private set
    private val headers = HeadersBuilder()
    private val content = Buffer()

    fun start(start: Instant) = apply { this.start = start }

    fun statusLine(line: String) = apply { this.statusLine = line }

    fun headers(headers: StringValues) = apply { this.headers.appendAll(headers) }

    fun appendContent(bs: ByteString) = apply { this.content.write(bs) }

    fun build() = HttpResponse(
        start,
        statusLine ?: "HTTP/1.1 0 Unknown",
        headers.build(),
        content
    )
}

@Serializable
data class HttpTransaction(
    val request: HttpRequest,
    val response: HttpResponse,
    val proxy: String?,
    val localAddress: String?,
    val remoteAddress: String?,
    val certificates: List<String>?,
)

class HttpTransactionBuilder {
    val request = HttpRequestBuilder()
    val response = HttpResponseBuilder()

    private var proxy: String? = null
    private var localAddress: String? = null
    private var remoteAddress: String? = null
    private var certificates: List<String>? = null

    fun proxy(proxy: String) = apply { this.proxy = proxy }
    fun localAddress(localAddress: String) = apply { this.localAddress = localAddress }
    fun remoteAddress(remoteAddress: String) = apply { this.remoteAddress = remoteAddress }
    fun certificates(certificates: List<String>) = apply { this.certificates = certificates }

    fun build() = HttpTransaction(
        request.build(),
        response.build(),
        proxy,
        localAddress,
        remoteAddress,
        certificates,
    )
}

class MultipleTransactionsBuilder {
    private var _current: HttpTransactionBuilder? = null
    private val builders = mutableListOf<HttpTransactionBuilder>()

    val current: HttpTransactionBuilder
        get() {
            if (_current == null) _current = HttpTransactionBuilder()
            return _current!!
        }

    fun next() {
        _current?.let(builders::add)
        _current = null
    }

    fun build(): List<HttpTransaction> {
        next()
        return builders.map(HttpTransactionBuilder::build)
    }
}