package com.tfowl.netlogtools.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.ajalt.clikt.parameters.types.path
import com.tfowl.netlogtools.cli.formats.HarOutputFormat
import com.tfowl.netlogtools.cli.formats.JsonOutputFormat
import com.tfowl.netlogtools.extractor.extractHttpTransactions
import com.tfowl.netlogtools.netlog.loadNetLog
import okio.buffer
import okio.sink
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createParentDirectories

enum class OutputFormatOption {
    JSON,
    HAR,
}

class ConvertCommand : CliktCommand() {

    val filters by FilteringOptions()

    val output: OutputStream by option(
        "-o", "--output",
        help = "Output file"
    )
        .outputStream(createIfNotExist = true, truncateExisting = true)
        .required()

    val format: OutputFormatOption by option(
        "-f", "--format",
        help = "Output file format",
    )
        .enum<OutputFormatOption>()
        .default(OutputFormatOption.JSON)

    val inputs: List<Path> by argument(help = "Netlog files")
        .path(mustExist = true)
        .multiple()


    override fun run() {
        val filter = filters.createFilter()

        val transactions = inputs.map { path -> loadNetLog(path) }
            .flatMap { log -> extractHttpTransactions(log) }
            .filter(filter)

        val fmt = when (format) {
            OutputFormatOption.JSON -> JsonOutputFormat(prettyPrint = true)
            OutputFormatOption.HAR  -> HarOutputFormat().also { System.err.println("HAR file is experimental") }
        }

        output.sink().buffer().use { sink ->
            fmt.write(sink, transactions)
        }
    }
}