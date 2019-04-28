package io.github.vivanov93.wikipedia.dumps.parser

import java.io.BufferedReader
import java.io.Closeable

/**
 * Two interfaces used everywhere in module sum
 */
interface CloseableIterator<T> : Closeable, Iterator<T>

/**
 * [BufferedReader.readLine] and [String.trim] sum
 */
internal fun BufferedReader.readTrimmed() = readLine().trim()