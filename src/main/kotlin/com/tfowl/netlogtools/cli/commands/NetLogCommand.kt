package com.tfowl.netlogtools.cli.commands

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class CommonOptions : OptionGroup("Common Options") {
    val filterUrl by option("--filter.url")
    val filterHost by option("--filter.host")
    val filterPath by option("--filter.path")

    val ignoreGoogleRequests by option("--ignore-google-requests").flag()
}

class NetLogCommand : NoOpCliktCommand(name = "netlog")