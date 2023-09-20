@file:JvmName("Main")

package com.tfowl.netlogtools.cli

import com.github.ajalt.clikt.core.subcommands
import com.tfowl.netlogtools.cli.commands.ConvertCommand
import com.tfowl.netlogtools.cli.commands.ExtractCommand
import com.tfowl.netlogtools.cli.commands.NetLogCommand

fun main(args: Array<String>) {
    NetLogCommand().subcommands(
        ConvertCommand(),
        ExtractCommand(),
    ).main(args)
}