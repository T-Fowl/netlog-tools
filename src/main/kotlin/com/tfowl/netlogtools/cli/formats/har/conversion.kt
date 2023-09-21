package com.tfowl.netlogtools.cli.formats.har

import com.tfowl.netlogtools.extractor.HttpRequest
import com.tfowl.netlogtools.extractor.HttpResponse
import com.tfowl.netlogtools.extractor.HttpTransaction
import io.ktor.http.*
import io.ktor.http.Cookie
import io.ktor.util.*
import kotlinx.serialization.json.JsonNull.content
import kotlinx.serialization.json.JsonObject
import okio.ByteString
import org.hildan.har.*
import java.util.UUID

private fun Headers.sentCookies(): List<Cookie> =
    getAll(HttpHeaders.Cookie)?.map { v -> parseServerSetCookieHeader(v) } ?: emptyList()

private fun Headers.cookies(): List<Cookie> =
    getAll(HttpHeaders.SetCookie)?.map { v -> parseServerSetCookieHeader(v) } ?: emptyList()

private fun Headers.toHar(): List<Header> = flattenEntries().map { (k, v) -> Header(k, v) }

private fun Cookie.toHar(): org.hildan.har.Cookie = org.hildan.har.Cookie(
    name = name,
    value = value,
    path = path ?: "",
    domain = domain ?: "",
    expires = expires?.toHttpDate() ?: "",
    httpOnly = httpOnly,
    secure = secure,
)

private fun Parameters.toHar(): List<HarRequest.Param> = flattenEntries().map { (k, v) ->
    HarRequest.Param(k, v)
}

private fun HttpRequest.toHar(): HarRequest = HarRequest(
    method = method.value,
    url = url.toString(),
    httpVersion = "HTTP/1.1", // TODO
    headers = headers.toHar(),
    queryString = url.parameters.toHar(),
    cookies = headers.sentCookies().map(Cookie::toHar),
    headersSize = -1,
    bodySize = -1,
    postData = null, // Always null because netlog doesn't log request bodies
)

private fun contentToHar(bytes: ByteString, type: ContentType?): HarResponse.Content {
    return HarResponse.Content(
        size = bytes.size.toLong(),
        mimeType = type?.toString() ?: "",
        text = bytes.utf8(),
        // TODO: For some reason when base64-encoding the content, chrome & firefox don't decode it?
//        text = bytes.base64(),
//        encoding = "base64",
    )
}

private fun HttpResponse.toHar(): HarResponse = HarResponse(
    status = 200, // TODO
    statusText = "OK", // TODO
    httpVersion = "HTTP/1.1", // TODO
    headers = headers.toHar(),
    cookies = headers.cookies().map(Cookie::toHar),
    content = contentToHar(
        content?.copy()?.readByteString() ?: ByteString.EMPTY,
        headers[HttpHeaders.ContentType]?.let(ContentType::parse)
    ),
    redirectURL = headers[HttpHeaders.Location] ?: "",
    headersSize = -1,
    bodySize = 0L,
    transferSize = -1,
)

private fun HttpTransaction.toHarEntry(): HarEntry = HarEntry(
    initiator = HarEntry.Initiator(""), // TODO
    resourceType = "",
    cache = JsonObject(emptyMap()),
    request = request.toHar(),
    response = response.toHar(),
    timings = HarEntry.Timings(
        blocked = -1.0,
        dns = -1.0,
        ssl = -1.0,
        connect = -1.0,
        send = -1.0,
        wait = -1.0,
        receive = -1.0,
        blockedQueueing = -1.0,
    ),
    // TODO: Should be left out but har lib doesn't make this optional
    pageref = UUID.randomUUID().toString(),
    serverIPAddress = this.remoteAddress?.substringBefore(':') ?: "",
    startedDateTime = request.start?.toString() ?: "",
    time = -1.0,
    priority = null,
)

fun List<HttpTransaction>.toHar(): Har = Har(
    log = HarLog(
        version = "1.2",
        creator = HarLog.Creator("netlogtools", "1.0.0-SNAPSHOT"), // TODO: Inject version
        pages = listOf(),
        entries = map(HttpTransaction::toHarEntry),
    )
)