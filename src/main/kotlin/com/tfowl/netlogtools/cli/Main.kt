@file:JvmName("Main")

package com.tfowl.netlogtools.cli

import com.github.ajalt.clikt.core.subcommands
import com.tfowl.netlogtools.cli.commands.ConvertCommand
import com.tfowl.netlogtools.cli.commands.ExtractCommand
import com.tfowl.netlogtools.cli.commands.NetLogCommand
import com.tfowl.netlogtools.extractor.HttpTransaction

typealias HttpTransactionFilter = (HttpTransaction) -> Boolean

val GOOGLE_FILTER: HttpTransactionFilter = { it.request.url.host.contains(Regex("""google|gstatic|googleapis""")) }

fun main(args: Array<String>) {
    NetLogCommand().subcommands(
        ConvertCommand(),
        ExtractCommand(),
    ).main(args)
}