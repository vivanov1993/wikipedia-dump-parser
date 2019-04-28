package io.github.vivanov93.wikipedia.dumps.parser

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.*
import java.lang.StringBuilder
import javax.xml.stream.XMLStreamConstants

//todo consider make FullPages analog with all data from xml
//todo basic tests and performance metrics

/**
 * Compact variant of wiki page with only [id] field related to metadata
 */
data class WikiPage(
        val id: Long,
        val title: String,
        val text: String
)


/**
 * Helper fun to get [WikiPage] by [WikiPageIndex]
 */
fun getPageByIndex(dump: File, index: WikiPageIndex): WikiPage {
    return dump.BZip2at(index).use { s -> s.asPagesIterator().asSequence().find { it.id == index.id } }!!
}


/**
 * Iterator over whole dump. It is rather slow due to dump being archive
 * note: dumps should be of relatively new format. Last time functionality checked in april 2019
 * note: closeable!
 *
 * todo performance optimizations
 *
 * @see "https://dumps.wikimedia.org/backup-index.html" for files
 *
 * @param index  relatively small File with titles and pointers, i.e. ruwiki-latest-pages-articles-multistream-index.txt(.bz2)?
 * @param dump - big pages dump File, i.e. ruwiki-latest-pages-articles-multistream.xml.bz2
 */
class WholeDumpPagesIterator(
        index: File,
        /**
         * Reference to main dump file needed for streams recreation
         */
        private val dump: File
) : CloseableIterator<WikiPage> {

    /**
     * Always existing indexes file iterator
     */
    private val indexes = index.asIndexesIterator()
    /**
     * Offset of [currentPagesIterator]
     */
    private var currentIndexOffset: Long = findNextMultiStreamIndexInternal()!!
    /**
     * Next offset or null for [hasNext] correct calculation
     */
    private var nextIndexOffset: Long? = findNextMultiStreamIndexInternal()!!

    /**
     * Current pages source
     */
    private var currentPagesIterator = dump.BZip2at(currentIndexOffset).asPagesIterator()

    override fun hasNext(): Boolean = currentPagesIterator.hasNext() || nextIndexOffset != null

    override fun next(): WikiPage {
        if (!currentPagesIterator.hasNext()) {
            refreshPagesIteratorInternal()
        }
        return currentPagesIterator.next()
    }

    /**
     * Update of [currentPagesIterator], [currentIndexOffset] and [nextIndexOffset] fields
     */
    private fun refreshPagesIteratorInternal() {
        currentPagesIterator.close()
        currentPagesIterator = dump.BZip2at(nextIndexOffset!!).asPagesIterator()
        currentIndexOffset = nextIndexOffset!!
        nextIndexOffset = findNextMultiStreamIndexInternal()
    }

    /**
     * Attempt to fined next offset from indexies stream. It is assumed that offsets are grouped together in indexes file
     */
    private fun findNextMultiStreamIndexInternal(): Long? {
        while (indexes.hasNext()) {
            val offsetFromStart = indexes.next().offsetFromStart
            if (offsetFromStart != currentIndexOffset) {
                return offsetFromStart
            }
        }
        return null
    }

    override fun close() {
        runCatching { indexes.close() } // todo consider to log
        runCatching { currentPagesIterator.close() }
    }
}


/**
 * Helper func to get [BZip2CompressorInputStream] as [Iterator]
 * note: closeable!
 */
fun BZip2CompressorInputStream.asPagesIterator(): CloseableIterator<WikiPage> = WikiPagesIterator(this)

/**
 * Iterator over [BZip2CompressorInputStream] as [WikiPage]s
 * note: stream should be untouched after opening and will not be closed by this class
 */
class WikiPagesIterator(sourceInputStream: BZip2CompressorInputStream)
    : CloseableIterator<WikiPage>, XMLStreamConstants {

    /**
     * Reader responsible for stream bytes processing
     */
    private val reader = BufferedReader(InputStreamReader(sourceInputStream))

    /**
     * Read pages counter to do not try to exceed [MAX_PAGES_PER_STREAM] limit
     */
    private var readPagesCounter = 0

    /**
     * Next page pointer
     */
    private var nextPage = readPageInternal()

    override fun hasNext(): Boolean = readPagesCounter < MAX_PAGES_PER_STREAM && nextPage != null

    override fun next(): WikiPage {
        val currentPage = nextPage
        nextPage = try {
            readPageInternal()
        } catch (e: Exception) {
            null
        }
        readPagesCounter++
        return currentPage!!
    }

    /**
     * Func with actual text parsing
     */
    private fun readPageInternal(): WikiPage? {
        // skip to title tag line
        reader.skipToLineEndingAs("<page>")
        val title = reader.readOneLineTagText()

        // skip to id tag line
        reader.skipToLineEndingAs("</ns>")
        val id = reader.readOneLineTagText().toLong()

        // skip to text tag
        reader.skipToLineEndingAs("</format>")
        val text = reader.readTextTagText()

        return WikiPage(id, title, text)
    }

    override fun close() = reader.close()

    companion object {
        const val MAX_PAGES_PER_STREAM = 100
    }
}

private fun BufferedReader.readOneLineTagText(): String {
    return readLine().substringAfter(">").substringBeforeLast("</")
}

private const val TEXT_TAG_TERMINATOR = "</text>"

private fun BufferedReader.readTextTagText(): String {
    var currentLine = readTrimmed()

    // special case - one-line text
    if (currentLine.endsWith(TEXT_TAG_TERMINATOR)) {
        return currentLine.substringAfter(">").substringBeforeLast(TEXT_TAG_TERMINATOR)
    }

    //
    val builder = StringBuilder(currentLine.substringAfter(">"))
    currentLine = readTrimmed()
    while (!currentLine.endsWith(TEXT_TAG_TERMINATOR)) {
        builder.append(currentLine)
        currentLine = readTrimmed()
    }
    //
    val lastLineContent = currentLine.substringBeforeLast(TEXT_TAG_TERMINATOR)
    builder.append(lastLineContent)

    return builder.toString()
}

/**
 * Read lines from reader to nowhere until read line ending with [ending]
 * note: trimming included
 * note: case sensitive
 */
private fun BufferedReader.skipToLineEndingAs(ending: String) {
    while (!readLine().trim().endsWith(ending));
}