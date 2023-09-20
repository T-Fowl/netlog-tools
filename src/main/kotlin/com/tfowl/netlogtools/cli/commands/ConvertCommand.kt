package com.tfowl.netlogtools.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import com.tfowl.netlogtools.extractor.extractHttpTransactions
import com.tfowl.netlogtools.cli.formats.JsonOutputFormat
import com.tfowl.netlogtools.netlog.loadNetLog
import okio.buffer
import okio.sink
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createParentDirectories

enum class OutputFormatOption {
    JSON,
}

class ConvertCommand : CliktCommand() {

    val output: Path by option("-o", "--output")
        .path(canBeDir = false)
        .required()

    val format: OutputFormatOption by option("-f", "--format")
        .enum<OutputFormatOption>()
        .default(OutputFormatOption.JSON)

    val inputs: List<Path> by argument().path(mustExist = true).multiple()


    override fun run() {
        val transactions = inputs.map { path -> loadNetLog(path) }
            .flatMap { log -> extractHttpTransactions(log) }

        val fmt = when (format) {
            OutputFormatOption.JSON -> JsonOutputFormat(prettyPrint = true)
        }

        output.createParentDirectories()

        output.sink(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).buffer().use { sink ->
            fmt.write(sink, transactions)
        }
    }
}