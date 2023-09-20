@file:JvmName("Main")

package com.tfowl

import com.github.ajalt.clikt.core.subcommands
import com.tfowl.commands.ConvertCommand
import com.tfowl.commands.ExtractCommand
import com.tfowl.commands.NetLogCommand
import com.tfowl.extractor.HttpTransaction
import com.tfowl.extractor.extractHttpTransactions
import com.tfowl.formats.JsonOutputFormat
import com.tfowl.formats.OutputFormat
import com.tfowl.netlog.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.encodeToBufferedSink
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import okio.*
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*
import kotlin.system.exitProcess

typealias HttpTransactionFilter = (HttpTransaction) -> Boolean

val GOOGLE_FILTER: HttpTransactionFilter = { it.request.url.host.contains(Regex("""google|gstatic|googleapis""")) }

fun main(args: Array<String>) {
    NetLogCommand().subcommands(
        ConvertCommand(),
        ExtractCommand(),
    ).main(args)
}