package io.github.vivanov93.wikipedia.dumps.parser.sql

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * If should print read categories to stdout
 */
private const val DO_PRINT = false

internal class CategoriesTest {

    @Test
    internal fun readTest() {
        var iteratedCounter = 0
        val time = measureTimeMillis {
            File(UNPACKED_CATEGORIES_DUMP_FILE).asCategoriesIterator().use { iter ->
                iter.forEach { category ->
                    iteratedCounter++
                    if (DO_PRINT) {
                        println(category)
                    }
                }
            }
        }
        println("$iteratedCounter categories were iterated over in $time millis")
    }
}