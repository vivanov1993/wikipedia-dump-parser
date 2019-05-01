package io.github.vivanov93.wikipedia.dumps.parser

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedReader
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

/**
 * Helper func to get [BZip2CompressorInputStream] as [Iterator]
 * note: closeable!
 */
fun BZip2CompressorInputStream.asPagesIterator(): CloseableIterator<WikiPage> {
    return WikiPagesIterator(BufferedCloseableStringsIterator(this.bufferedReader()))
}

/**
 * Helper converting File to [BufferedCloseableStringsIterator] func
 */
fun File.toBufferedLinesProvider(): CloseableIterator<String> {
    val stream = if (name.endsWith(".bz2")) {
        BZip2at(0)
    } else {
        inputStream()
    }
    return BufferedCloseableStringsIterator(stream.bufferedReader())
}

/**
 * Fastest known to me, and memory efficient file reading lines iterator
 */
class BufferedCloseableStringsIterator(
        /**
         * Lowest lvl reader
         */
        private val reader: BufferedReader
) : CloseableIterator<String> {

    /**
     * Next line pointer as [BufferedReader.ready] is not exactly same as "have next"
     */
    private var nextLine = reader.readLine()

    override fun hasNext(): Boolean = nextLine != null

    override fun next(): String = nextLine!!.also { nextLine = reader.readLine() }

    override fun close() = reader.close()

}