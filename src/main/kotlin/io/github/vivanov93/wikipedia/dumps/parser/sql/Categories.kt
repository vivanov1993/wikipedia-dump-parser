package io.github.vivanov93.wikipedia.dumps.parser.sql

import io.github.vivanov93.wikipedia.dumps.parser.common.CloseableIterator
import java.io.BufferedReader
import java.io.File
import java.util.*

/**
 * Wiki category from dumps named like ruwiki-latest-category.sql
 *
 * Table is created by script:
 *
 * CREATE TABLE `category` (
 * `cat_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
 * `cat_title` varbinary(255) NOT NULL DEFAULT '',
 * `cat_pages` int(11) NOT NULL DEFAULT '0',
 * `cat_subcats` int(11) NOT NULL DEFAULT '0',
 * `cat_files` int(11) NOT NULL DEFAULT '0',
 * PRIMARY KEY (`cat_id`),
 * UNIQUE KEY `cat_title` (`cat_title`),
 * KEY `cat_pages` (`cat_pages`)
 * )
 */
data class WikiCategory(
        val id: Int,
        /**
         * Spaces usually replaced by underscore
         */
        val title: String,
        /**
         * [io.github.vivanov93.wikipedia.dumps.parser.xml.WikiPage]s in category
         */
        val pagesCount: Int,
        val subcategoriesCount: Int,
        val catFiles: Int
)

/**
 * Helper func returning [File] as [CategoriesIterator]
 */
fun File.asCategoriesIterator(): CloseableIterator<WikiCategory> = CategoriesIterator(this.bufferedReader())

/**
 * Iterator over dumps like ruwiki-latest-category.sql
 * note: closeable
 */
class CategoriesIterator(private val reader: BufferedReader) : CloseableIterator<WikiCategory> {

    private var nextCategories: Queue<WikiCategory> = LinkedList<WikiCategory>()

    init {
        readCategoriesInternal()
    }

    override fun hasNext(): Boolean = nextCategories.isNotEmpty()

    override fun next(): WikiCategory {
        val currentCategory = nextCategories.poll()
        if (nextCategories.isEmpty()) {
            readCategoriesInternal()
        }
        return currentCategory
    }

    private fun readCategoriesInternal() {
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

        fun String.toCategory(): WikiCategory? {
            return try {
                val stringToParse = this.substringBeforeLast(SQL_INSERT_END)
                val id = stringToParse.substringBefore(SQL_VARCHAR_START)
                val title = stringToParse.substring(id.length + 2).substringBefore(SQL_VARCHAR_END)
                val otherNums = stringToParse.substring(id.length + 2 + title.length + 2)
                        .split(SQL_VALUES_DELIMITER)
                        .map { it.toInt() }
                require(otherNums.size == 3)
                WikiCategory(id.toInt(),
                        title,
                        otherNums[0],
                        otherNums[1],
                        otherNums[2]
                )
            } catch (e: Exception) {
                /**
                 * From time to time there is malformed string between two categories like
                 * (608536,'Wikipedia_sockpuppets_of_\');_DROP_TABLE_en_user',2,0,0),(608538,'Massachusetts_local_politicians',17,4,0)
                 * so for now just ignore it
                 */
                e.printStackTrace()// todo slf4j
                null
            }
        }

        val nextInsert = getNextInsert()?.substringAfter(SQL_INSERT_START)
        nextInsert?.split(SQL_ROWS_DELIMITER)
                ?.map { it.toCategory() }
                ?.filter { it != null }
                ?.forEach { nextCategories.add(it) }
    }

    override fun close() = reader.close()

    private companion object {
        const val SQL_INSERT_START = "INSERT INTO `category` VALUES ("
        const val SQL_INSERT_END = ");"

        const val SQL_ROWS_DELIMITER = "),("

        const val SQL_VARCHAR_START = ",'"
        const val SQL_VARCHAR_END = "',"
        const val SQL_VALUES_DELIMITER = ","
    }
}