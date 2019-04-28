package io.github.vivanov93.wikipedia.dumps.parser

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File

/**
 * Skips stream to given [index]
 */
fun File.BZip2at(index: WikiPageIndex): BZip2CompressorInputStream = BZip2at(index.offsetFromStart)

/**
 * Skips stream by [multiStreamOffset] and returns [BZip2CompressorInputStream] with xml text data of wiki
 */
fun File.BZip2at(multiStreamOffset: Long): BZip2CompressorInputStream {
    val inputStream = inputStream()
    val skip = inputStream.skip(multiStreamOffset)
    require(skip == multiStreamOffset) { "Skipping failed" }
    return BZip2CompressorInputStream(inputStream)
}