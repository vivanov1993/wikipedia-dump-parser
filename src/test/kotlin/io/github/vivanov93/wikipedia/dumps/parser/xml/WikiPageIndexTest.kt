package io.github.vivanov93.wikipedia.dumps.parser.xml

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

/**
 * If should print read indexes to stdout
 */
private const val DO_PRINT = false

@Suppress("ConstantConditionIf")
internal class WikiPageIndexTest {

    @Test
    internal fun testIndexesReadFromSampleFile() {
        val sampleFile: File = Paths.get(".", "src", "test", "resources", "sample_index.txt").toFile()
        val sampleFileRowsCount = 91
        readIndexesFile(sampleFile, sampleFileRowsCount)
    }

    @Test
    internal fun testIndexesReadFromBZ2File() {
        readIndexesFile(File(BZ2_MULTISTREAM_INDEX_FILE)) // ~ 1.5 min ruwiki
    }

    @Test
    internal fun testIndexesReadFromXMLFile() {
        readIndexesFile(File(UNPACKED_MULTISTREAM_INDEX_FILE)) // ~ 3s ruwiki, ~ 13.5s enwiki
    }


    private fun readIndexesFile(file: File, expectedRowsCount: Int = -1) {
        val fileName = file.name
        var rowsCounter = 0
        file.asIndexesIterator().use { iter ->
            iter.forEach {
                rowsCounter++
                if (DO_PRINT) {
                    println("$fileName $rowsCounter: $it")
                }
            }
        }
        if (expectedRowsCount != -1) {
            assert(rowsCounter == expectedRowsCount)
        }
    }

}