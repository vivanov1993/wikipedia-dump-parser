package io.github.vivanov93.wikipedia.dumps.parser

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.random.Random


/**
 * If should print read pages to stdout
 */
private const val DO_PRINT = true

@Suppress("ConstantConditionIf")
internal class WikiPageTest {

    @Test
    internal fun testIndexesReadFromSampleFile() {
        val sampleFile: File = Paths.get(".", "src", "test", "resources", "samle_pages.xml").toFile()
        var counter = 0
        val sampleFilePagesQuantity = 2
        WikiPagesIterator(sampleFile.toBufferedLinesProvider()).use { iter ->
            while (iter.hasNext()) {
                iter.next()
                counter++
            }
        }
        assert(counter == sampleFilePagesQuantity)
    }

    @Test
    internal fun readRandomPagesByIndex() {
        val dump = File(BZ2_MULTISTREAM_DUMP_FILE)
        var indexesIteratedCounter = 0
        val performance = LinkedList<Long>()

        File(UNPACKED_MULTISTREAM_INDEX_FILE).asIndexesIterator().use { iter ->
            iter.forEach {
                if (Random.nextInt(5000) == 1) {
                    val t0 = System.currentTimeMillis()

                    val page = getPageByIndex(dump, it)

                    performance.add(System.currentTimeMillis() - t0)
                    checkPageIsOk(page)
                    if (DO_PRINT) {
                        println("indexesIteratedCounter=$indexesIteratedCounter pagesQueryCounter=${performance.size}: page ${page.title} with id ${page.id} q_time=${performance.peekLast()}")
                    }
                }
                indexesIteratedCounter++
            }
        }

        println("${performance.size} random pages where checked during run over $indexesIteratedCounter pages")
        println("Average read time: ${performance.average()}ms, min = ${performance.min()}ms, max=${performance.max()}ms")
    }

    @Test
    internal fun iterateOverWholeIndexedMultiStream() {
        var counter = 0
        IndexedDumpIterator(File(UNPACKED_MULTISTREAM_INDEX_FILE), File(BZ2_MULTISTREAM_DUMP_FILE)).use { iter ->
            iter.forEach { page ->
                counter++
                checkPageIsOk(page)
                if (DO_PRINT) {
                    println("page #$counter with title '${page.title}' was checked")
                }
            }
        }
        println("$counter pages were checked")
    }

    @Test
    internal fun iterateOverWholeXMLDump() {
        var counter = 0
        UnpackedDumpIterator(File(UNPACKED_DUMP_FILE)).use { iter ->
            iter.forEach { page ->
                counter++
                checkPageIsOk(page)
                if (DO_PRINT) {
                    println("page #$counter with title ${page.title} was checked")
                }
            }
        }
        println("$counter pages were cheked")
    }

    private fun checkPageIsOk(page: WikiPage) {
        // todo better maybe
        val lazyMessage = { "failed test on:\n$page " }
        require(!page.text.contains("<text>"), lazyMessage)
        require(!page.text.contains("</text>"), lazyMessage)
        require(!page.text.contains("<sha1>"), lazyMessage)
        require(!page.text.contains("</sha1>"), lazyMessage)
        require(!page.text.contains("<ns>"), lazyMessage)
        require(!page.text.contains("</ns>"), lazyMessage)
    }

}