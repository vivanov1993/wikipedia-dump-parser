package io.github.vivanov93.wikipedia.dumps.parser

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import javax.xml.stream.XMLStreamConstants

//todo consider make FullPages analog with all data from xml

/**
 * Compact variant of wiki page with only [id] field related to metadata
 */
data class WikiPage(
        val id: Long,
        val title: String,
        val text: String
)

/**
 * Iterator over [BZip2CompressorInputStream] as [WikiPage]s
 * note: stream should be untouched after opening and will not be closed by this class
 */
class WikiPagesIterator(
        /**
         * Closeable reader to do low level read operations
         */
        private val reader: CloseableIterator<String>

) : CloseableIterator<WikiPage>, XMLStreamConstants {

    /**
     * Next page pointer
     */
    private var nextPage = readPageInternal()

    override fun hasNext(): Boolean = nextPage != null

    override fun next(): WikiPage {
        val currentPage = nextPage
        nextPage = try {
            readPageInternal()
        } catch (e: Exception) {
            null
        }
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
}

private fun Iterator<String>.readOneLineTagText(): String {
    return next().substringAfter(">").substringBeforeLast("</")
}

private const val TEXT_TAG_TERMINATOR = "</text>"
private const val EMPTY_TEXT = "<text xml:space=\"preserve\" />"

private fun Iterator<String>.readTextTagText(): String {
    var currentLine = readTrimmed()

    // special case - no text
    if (currentLine == EMPTY_TEXT) {
        return ""
    }

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
internal fun Iterator<String>.skipToLineEndingAs(ending: String) {
    while (!next().trim().endsWith(ending));
}


/**
 * Check if this [this] just stub or real article
 *
 * @return true if this page just a stub, false if this is real article
 */
fun WikiPage.isRedirectPage(): Boolean = text.length < 144 && text.toLowerCase().contains("redirect")

/**
 * Find [WikiPage.title] on which this [this] redirecting to
 * Typical redirect text is:
 * #REDIRECT [[This is title]]
 */
fun WikiPage.getRedirectPageTitle(): String {
    if (isRedirectPage()) {
        return text.substringAfter("[[").substringBefore("]]")
    } else {
        throw  IllegalArgumentException("This is not redirecting page")
    }
}