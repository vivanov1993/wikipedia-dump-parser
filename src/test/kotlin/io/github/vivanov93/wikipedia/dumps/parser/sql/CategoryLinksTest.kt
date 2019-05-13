package io.github.vivanov93.wikipedia.dumps.parser.sql

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * If should print read category links to stdout
 */
private const val DO_PRINT = false

internal class CategoryLinksTest {

    @Test
    internal fun readTest() {
        var iteratedCounter = 0
        val time = measureTimeMillis {
            File(UNPACKED_CATEGORIES_LINKS_DUMP_FILE).asCategoryLinksIterator().use { iter ->
                iter.forEach { categoryLink ->
                    iteratedCounter++
                    if (DO_PRINT) {
                        println(categoryLink)
                    }
                }
            }
        }
        println("$iteratedCounter category links were iterated over in $time millis")
    }
}