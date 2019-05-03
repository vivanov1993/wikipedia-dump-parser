package io.github.vivanov93.wikipedia.dumps.parser

import java.io.BufferedReader
import java.io.File

/**
 * Wrapper around [WikiPagesIterator] to skip meta-info allowing to parse whole unpacked wiki dump
 */
class UnpackedDumpIterator(dump: File) : CloseableIterator<WikiPage> {

    private val realIterator: CloseableIterator<WikiPage> = dump.bufferedReader()
            .skipToLineEndingAs("</siteinfo>")
            .let { WikiPagesIterator(it) }

    private fun BufferedReader.skipToLineEndingAs(ending: String): BufferedCloseableStringsIterator {
        val iter = BufferedCloseableStringsIterator(this)
        iter.skipToLineEndingAs(ending)
        return iter
    }

    override fun close() = realIterator.close()

    override fun hasNext(): Boolean = realIterator.hasNext()

    override fun next(): WikiPage = realIterator.next()
}

/**
 * Iterator over whole dump. It is rather slow due to dump being archive
 * note: dumps should be of relatively new format. Last time functionality checked in april 2019
 * note: closeable!
 *
 * @see "https://dumps.wikimedia.org/backup-index.html" for files
 *
 * @param index  relatively small File with titles and pointers, i.e. ruwiki-latest-pages-articles-multistream-index.txt(.bz2)?
 * @param dump - big pages dump File, i.e. ruwiki-latest-pages-articles-multistream.xml.bz2
 */
class IndexedDumpIterator(
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
        runCatching { indexes.close() }
        runCatching { currentPagesIterator.close() }
    }
}
