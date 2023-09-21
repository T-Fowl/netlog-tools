package com.tfowl.netlogtools.cli.commands

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.tfowl.netlogtools.extractor.HttpTransaction

typealias HttpTransactionFilter = (HttpTransaction) -> Boolean

private val GOOGLE_HOST_REGEX = Regex("""google|gstatic|googleapis""")

private val FILTER_IGNORE_GOOGLE_HOSTS: HttpTransactionFilter =
    { !it.request.url.host.contains(GOOGLE_HOST_REGEX) }

class FilteringOptions : OptionGroup("Filtering Options") {
    // TODO: Decide how I want these to work
    // e.g. include / exclude / regex filter
    // multiple include / exclude / filter OR or AND together?
//    val filterUrl by option("--filter.url").multiple()
//    val filterHost by option("--filter.host").multiple()
//    val filterPath by option("--filter.path").multiple()

    // This one is required however because the netlogs from chrome are spammed with google crap
    private val ignoreGoogleRequests by option(
        "--ignore-google-requests",
        help = "Filter out google http transactions. Uses the following regex: $GOOGLE_HOST_REGEX"
    ).flag()

    fun createFilter(): HttpTransactionFilter {
        if (ignoreGoogleRequests)
            return FILTER_IGNORE_GOOGLE_HOSTS
        else
            return { true }
    }
}