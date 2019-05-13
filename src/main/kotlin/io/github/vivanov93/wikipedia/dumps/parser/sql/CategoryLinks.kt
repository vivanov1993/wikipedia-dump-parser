package io.github.vivanov93.wikipedia.dumps.parser.sql

import io.github.vivanov93.wikipedia.dumps.parser.common.CloseableIterator
import java.io.BufferedReader
import java.io.File
import java.sql.Timestamp
import java.util.*

/**
 * Wiki category from ruwiki-latest-categorylinks.sql
 *
 * Table is created by script:
 *
 * CREATE TABLE `categorylinks` (
 * `cl_from` int(8) unsigned NOT NULL DEFAULT '0',
 * `cl_to` varbinary(255) NOT NULL DEFAULT '',
 * `cl_sortkey` varbinary(230) NOT NULL DEFAULT '',
 * `cl_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 * `cl_sortkey_prefix` varbinary(255) NOT NULL DEFAULT '',
 * `cl_collation` varbinary(32) NOT NULL DEFAULT '',
 * `cl_type` enum('page','subcat','file') NOT NULL DEFAULT 'page',
 * PRIMARY KEY (`cl_from`,`cl_to`),
 * KEY `cl_timestamp` (`cl_to`,`cl_timestamp`),
 * KEY `cl_sortkey` (`cl_to`,`cl_type`,`cl_sortkey`,`cl_from`),
 * KEY `cl_collation_ext` (`cl_collation`,`cl_to`,`cl_type`,`cl_from`)
 * )
 */
data class WikiCategoryLink(
        val pageId: Int,
        val categoryTitle: String,
        val timestamp: Timestamp
)


/**
 * Helper func returning [File] as [CategoriesIterator]
 */
fun File.asCategoryLinksIterator(): CloseableIterator<WikiCategoryLink> = CategoryLinksIterator(this.bufferedReader())

/**
 * Iterator over dumps like ruwiki-latest-categorylinks.sql
 * note: closeable
 */
class CategoryLinksIterator(private val reader: BufferedReader) : CloseableIterator<WikiCategoryLink> {

    private var nextCategoryLinks: Queue<WikiCategoryLink> = LinkedList<WikiCategoryLink>()

    init {
        readCategoryLinksInternal()
    }

    override fun hasNext(): Boolean = nextCategoryLinks.isNotEmpty()

    override fun next(): WikiCategoryLink {
        val currentCategory = nextCategoryLinks.poll()
        if (nextCategoryLinks.isEmpty()) {
            readCategoryLinksInternal()
        }
        return currentCategory
    }

    private fun readCategoryLinksInternal() {
        fun getNextInsert(): String? {
            var line = reader.readLine()
            while (line != null) {
                if (line.startsWith(SQL_INSERT_START)) {
                    return line
                }
                line = reader.readLine()
            }
            return null
        }

        fun String.toCategoryLink(): WikiCategoryLink {
            val id = this.substringBefore(SQL_ESCAPED_START)
            val title = this.substring(id.length + 2).substringBefore(SQL_ESCAPED_END)
            val timestamp = this.substring(id.length + 2 + title.length + 4)
                    .substringAfter(BETWEEN_TWO_SQL_ESCAPED)
                    .substringBefore(SQL_ESCAPED_END)
            val timestamp1 = Timestamp.valueOf(timestamp)
            return WikiCategoryLink(
                    id.toInt(),
                    title,
                    timestamp1)
        }

        val nextInsert = getNextInsert()?.substringAfter(SQL_INSERT_START)
        nextInsert?.split(SQL_ROWS_DELIMITER)
                ?.map { it.toCategoryLink() }
                ?.forEach { nextCategoryLinks.add(it) }
    }

    override fun close() = reader.close()

    private companion object {
        const val SQL_INSERT_START = "INSERT INTO `categorylinks` VALUES ("

        const val SQL_ROWS_DELIMITER = "),("

        const val SQL_ESCAPED_START = ",'"
        const val SQL_ESCAPED_END = "',"
        const val BETWEEN_TWO_SQL_ESCAPED = "','"
    }
}