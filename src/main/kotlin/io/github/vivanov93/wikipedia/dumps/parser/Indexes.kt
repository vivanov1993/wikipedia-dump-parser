package io.github.vivanov93.wikipedia.dumps.parser

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.*


/**
 * Index file single entry
 * note: stream should be untouched after opening and will not be closed by this class
 */
data class WikiPageIndex(
        /**
         * Number of bytes that should be skipped in input stream to get to [BZip2CompressorInputStream] with pages
         * (typically 100 pages per [BZip2CompressorInputStream] stream)
         */
        val offsetFromStart: Long,
        /**
         * Just page id (it is not first field to have structural similarity to index file)
         */
        val id: Long,
        /**
         * Just title (can have multiple words)
         */
        val title: String
)


/**
 * Helper func to get File as [Iterator]
 * note: closeable!
 * note: unpacked index file proceed ~16 times faster
 */
fun File.asIndexesIterator(): IndexesIterator = IndexesIterator(this.toBufferedLinesProvider())

/**
 * Iterator over file with indexes
 * note: closeable!
 */
class IndexesIterator(
        /**
         * Closeable reader to do low level read operations
         */
        private val reader: CloseableIterator<String>
) : CloseableIterator<WikiPageIndex> {

    /**
     * Next index or null for [hasNext] correct calculation
     */
    private var nextIndex: WikiPageIndex? = readIndexInternal()

    override fun hasNext(): Boolean = nextIndex != null

    override fun next(): WikiPageIndex {
        val prevIndex = nextIndex!!
        nextIndex = readIndexInternal()
        return prevIndex
    }

    /**
     * Func with actual text parsing
     * Text example:
     * 3808346070:7863199:Elkies
     * ...
     */
    private fun readIndexInternal(): WikiPageIndex? {
        return if (reader.hasNext()) {
            val split = reader.next().split(':')
            val offsetFromStart = split[0].toLong()
            val id = split[1].toLong()
            val title = split.subList(2, split.size).joinToString(separator = ":")
            return WikiPageIndex(offsetFromStart, id, title)
        } else {
            null
        }
    }

    override fun close() = reader.close()
}

/**
 * Helper converting File to [BufferedCloseableStringsIterator] func
 */
fun File.toBufferedLinesProvider(): CloseableIterator<String> = BufferedCloseableStringsIterator(this)

/**
 * Fastest known to me and memory efficient file reading lines iterator
 */
class BufferedCloseableStringsIterator(file: File) : CloseableIterator<String> {
    /**
     * Lowest lvl reader
     */
    private val reader = BufferedReader(InputStreamReader(file.toInputStream()))

    /**
     * Next line pointer as [BufferedReader.ready] is not exactly same as "have next"
     */
    private var nextLine = reader.readLine()

    override fun hasNext(): Boolean = nextLine != null

    override fun next(): String = nextLine!!.also { nextLine = reader.readLine() }

    override fun close() = reader.close()

    /**
     * Optional bz2 stream wrapper
     */
    private fun File.toInputStream(): InputStream {
        return if (name.endsWith(".bz2")) {
            BZip2at(0)
        } else {
            inputStream()
        }
    }
}