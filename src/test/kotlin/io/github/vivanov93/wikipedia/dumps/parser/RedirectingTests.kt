package io.github.vivanov93.wikipedia.dumps.parser

import org.junit.jupiter.api.Test
import java.io.File

/**
 * If should print read pages info to stdout
 */
private const val DO_PRINT = true

internal class RedirectingTests {

    @Test
    internal fun find10kRedirects() {
        var iteratedCounter = 0
        var redirectsCounter = 0
        UnpackedDumpIterator(File(UNPACKED_DUMP_FILE)).use { iter ->
            iter.forEach { page ->
                iteratedCounter++
                if (page.isRedirectPage()) {
                    redirectsCounter++
                    println(page.getRedirectPageTitle())
                    if (DO_PRINT) {
                        println("#$redirectsCounter: Page #${page.title} with title ${page.title} redirecting to ${page.getRedirectPageTitle()}")
                    }
                }
            }
        }
        println("$redirectsCounter were found after iterating over $iteratedCounter pages were")
    }
}