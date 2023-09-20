package com.tfowl.netlogtools

import io.ktor.http.*
import java.nio.file.Path

fun <T> List<T>.indexOfFirst(startingIndex: Int, predicate: (T) -> Boolean): Int {
    for (index in startingIndex until size) {
        if (predicate(get(index)))
            return index
    }

    return -1
}

fun <T> List<T>.subRanges(pStart: (T) -> Boolean, pFinish: (T) -> Boolean): Sequence<List<T>> {
    return sequence {
        var index = 0
        while (true) {
            val start = this@subRanges.indexOfFirst(index, pStart).takeIf { it >= 0 } ?: break
            val finish = this@subRanges.indexOfFirst(start, pFinish).takeIf { it > start } ?: break

            yield(this@subRanges.subList(start, finish + 1))

            index = finish + 1
        }
    }
}

fun <K, V> Map<K, V>.swap(): Map<V, K> = map { (k, v) -> Pair(v, k) }.toMap()

fun Url.withoutParameters(): Url = URLBuilder(this).apply { parameters.clear() }.build()

fun Path.resolve(segments: List<String>): Path = segments.fold(this, Path::resolve)