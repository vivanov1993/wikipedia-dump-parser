package io.github.vivanov93.wikipedia.dumps.parser.xml

import java.io.File


/**
 * Helper fun to get [WikiPage] by [WikiPageIndex]
 * note: unpacked file random access is ~3 magnitudes slower
 */
fun getPageByIndex(dump: File, index: WikiPageIndex): WikiPage {
    val id = index.id
    return if (dump.name.endsWith(".bz2")) {
        dump.BZip2at(index).use { s -> s.asPagesIterator().asSequence().find { it.id == id } }!!
    } else {
        UnpackedDumpIterator(dump).use { iter -> iter.asSequence().find { it.id == id } }!!
    }
}


/**
 * Helper fun to get all [WikiPage]s which [WikiPageIndex] match [condition] with redirects being followed
 */
fun findNonRedirectingPagesByIndexCondition(
        mulistreamDump: File,
        indexes: File,
        condition: (WikiPageIndex) -> Boolean
): Set<WikiPage> {

    // first iteration by multistreams - find targeted pages and redirects
    val firstIterationResult = findPagesByIndexCondition(mulistreamDump, indexes, condition)
    val (redirects, normalPages) = firstIterationResult.partition { it.isRedirectPage() }

    // second iteration - follow redirects (assuming no redirects to redirects presented)
    val redirectTargets = redirects.map { it.getRedirectPageTitle() }.toSet()
    val redirected = findPagesByIndexCondition(mulistreamDump, indexes) { redirectTargets.contains(it.title) }

    return normalPages.toMutableSet().also { it.addAll(redirected) }.toSet()
}


/**
 * Helper fun to get all [WikiPage]s which [WikiPageIndex] match [condition] with redirects being ignored
 */
fun findPagesByIndexCondition(
        mulistreamDump: File,
        indexes: File,
        condition: (WikiPageIndex) -> Boolean
): Set<WikiPage> {
    val result = mutableSetOf<WikiPage>()

    /**
     * Indexes matching input predicate
     */
    val indexesOfInterest = indexes.asIndexesIterator().use {
        it.asSequence().filter(condition).toList()
    }
    /**
     * Indexes grouped by multistreams for possible fastest iteration - query one stream one time
     */
    val indexesByMultistreams = indexesOfInterest.groupBy { it.offsetFromStart }

    indexesByMultistreams.entries.asSequence().forEach { multistreamOfInterest ->
        val idsOfInterest = multistreamOfInterest.value.map { it.id }.toSet()

        mulistreamDump.BZip2at(multistreamOfInterest.key).asPagesIterator().use { pages ->
            pages.asSequence()
                    .filter { page -> idsOfInterest.contains(page.id) }
                    .forEach { page -> result.add(page) }
        }
    }

    return result
}