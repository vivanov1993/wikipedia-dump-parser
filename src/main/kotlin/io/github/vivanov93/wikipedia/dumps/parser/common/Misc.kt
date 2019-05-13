package io.github.vivanov93.wikipedia.dumps.parser.common

import java.io.Closeable

/**
 * Two interfaces used everywhere in module sum
 */
interface CloseableIterator<T> : Closeable, Iterator<T>

/**
 * [CloseableIterator.next] and [String.trim] sum
 */
internal fun Iterator<String>.readTrimmed(): String = next().trim()