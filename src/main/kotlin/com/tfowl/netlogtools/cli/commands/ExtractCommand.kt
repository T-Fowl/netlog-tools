package com.tfowl.netlogtools.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.tfowl.netlogtools.extractor.extractHttpTransactions
import com.tfowl.netlogtools.netlog.loadNetLog
import io.ktor.http.*
import okio.ByteString
import okio.buffer
import okio.sink
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

class Asset(
    val url: Url,
    val content: ByteString,
    val contentType: ContentType?,
) {
    val digest by lazy { content.md5() }

    override fun equals(other: Any?): Boolean {
        return (other as? Asset)?.digest?.equals(digest) == true
    }

    override fun hashCode(): Int {
        return digest.hashCode()
    }
}

class ExtractCommand : CliktCommand() {

    val filters by FilteringOptions()

    val output: Path by option(
        "-o", "--output",
        help = "Directory to extract responses within"
    )
        .path(canBeFile = false)
        .required()

    val inputs: List<Path> by argument(help = "Netlog files")
        .path(mustExist = true)
        .multiple()


    override fun run() {
        val filter = filters.createFilter()

        output.createDirectories()

        val urlAssets = inputs.map { path -> loadNetLog(path) }
            .flatMap { log -> extractHttpTransactions(log) }
            .filter(filter)
            .mapNotNull { transaction ->
                transaction.response.content?.takeIf { it.size > 0 }?.let { content ->
                    val type = transaction.response.headers[HttpHeaders.ContentType]?.let(ContentType::parse)
                    Asset(transaction.request.url, content.readByteString(), type)
                }
            }.groupingBy { it.url }
            .aggregate { _, set: MutableSet<Asset>?, asset, _ -> (set ?: mutableSetOf()).apply { add(asset) } }

        urlAssets.forEach { (url, assets) ->
            val hasConflictingAssets = assets.size > 1

            if (hasConflictingAssets) {
                println("Conflicting assets for $url ${assets.map { it.digest }}")
            }

            assets.iterator().withIndex().forEach { (i, asset) ->
                val path = output.resolve(buildString {
                    append(url.host)
                    append(url.encodedPath)

                    if (!url.parameters.isEmpty()) {
                        append(" ")
                        url.parameters.formUrlEncodeTo(this)
                    }

                    if (hasConflictingAssets)
                        append(".$i")

                    asset.contentType?.let { type ->
                        val extension = when {
                            // Override because the ktor mime list has .acgi before .html???
                            type.withoutParameters().match(ContentType.Text.Html) -> "html"
                            else                                                  -> type.fileExtensions().firstOrNull()
                        }

                        extension?.let {
                            append('.')
                            append(extension)
                        }
                    }
                })

                if (path.exists()) {
                    println("Overwriting $path")
                }
                path.createParentDirectories()


                try {
                    path.sink(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).buffer().use { sink ->
                        sink.write(asset.content)
                    }
                } catch (t: Throwable) {
                    println(t.message)
                }
            }
        }
    }
}