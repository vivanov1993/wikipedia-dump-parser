# JVM wikipeadia dumps parser

Here you can find simple Kotlin/Java library to work with some of wiki .xml(.bz2) and .sql dumps from https://dumps.wikimedia.org/enwiki/latest/.
Library is written in Kotlin but can be used from pure java code as well

## Table of content

- [Reasoning](#reasoning)
- [Installation](#installation)
- [XML](#xml)   
- [SQL](#sql)   
- [More](#more)   
- [License](#license)

## Reasoning

Wiki dumps is great source of structured information etc. etc., but it seems that there is no dumps parser on JVM which is:
- updated recently
- have OK readme with basic information and examples
- not some console converter app but just lowest lvl library api to read dump files

Intended usage is based on single iteration over dumps and:
1) Storing data in SQL/noSQL storage for further work
2) Fetching something
3) Parsing to csv or other format

## Installation

1) Checkout as gradle project
2) Install artifact in local repo using gradle "maven" plugin
3) Add as dependency in your project
### Add using Maven

Add the following dependency to your pom.xml:

```xml
    <dependency>
        <groupId>io.github.vivanov93</groupId>
        <artifactId>wikipedia-dumps-parser</artifactId>
        <version>0.1</version>
    </dependency>
```

### Add using Gradle

Add the following dependency to your build.gradle file:

    compile "io.github.vivanov93:wikipedia-dumps-parser:0.1"
    

Check that local maven repo is listed in repositories:

    repositories {
        //...
        mavenLocal()
    }
    
Note1: to work with .BZ2 Files https://mvnrepository.com/artifact/org.apache.commons/commons-compress also required
Note2: https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8 also required

## XML

### Get dumps
You need to download two *multistream* files from page like https://dumps.wikimedia.org/enwiki/latest/:
1) Index file with name like ```ruwiki-latest-pages-articles-multistream-index.txt.bz2```
2) Pages file with name like ```ruwiki-latest-pages-articles-multistream.xml.bz2```

#### Notes:
- any language should be ok
- lib was tested on april 2019 dumps version
- both packed and unpacked index file can be used. Unpacked index traversal was is ~16 times faster on my average proc/hdd
- as there is work with archives all lib operations may be considered "computations-heavy" 

### Java
```java
public class Main {
    public static void main(String[] args) {
        // files to work with
        final File multiStreamIndexesFile = new File("INSERT INDEX FILE PATH");
        final File multiStreamPagesDumpFile = new File("INSERT PAGES FILE PATH");

        // use case: find article by title
        WikiPageIndex javaArticleIndex = null;
        try (CloseableIterator<WikiPageIndex> indexesIterator = IndexesKt.asIndexesIterator(multiStreamIndexesFile)) {
            final String titleLike = "Java";
            while (indexesIterator.hasNext()) {
                final WikiPageIndex wikiPageIndex = indexesIterator.next();
                if (wikiPageIndex.getTitle().contains(titleLike)) {
                    javaArticleIndex = wikiPageIndex;
                    break;
                }
            }
        }
        System.out.println("Java page index: " + javaArticleIndex);

        // use case: find page by index
        Objects.requireNonNull(javaArticleIndex);
        final WikiPage javaPageByIndex = TasksKt.getPageByIndex(multiStreamPagesDumpFile, javaArticleIndex);
        System.out.println("Java page by index:\n" + javaPageByIndex);

        // use case: iterate over every page in dump
        WikiPage javaPageFromScan = null;
        try (CloseableIterator<WikiPage> dumpIterator = new IndexedDumpIterator(multiStreamIndexesFile, multiStreamPagesDumpFile)) {
            while (dumpIterator.hasNext()) {
                final WikiPage somePage = dumpIterator.next();
                if (somePage.getId() == javaArticleIndex.getId()) {
                    javaPageFromScan = somePage;
                    break;
                }
            }
        }
        System.out.println("Java page from scan:\n" + javaPageFromScan);
    }
}
```

### Kotlin
```Kotlin
fun main() {
    // files to work with
    val multiStreamIndexesFile = File("INSERT INDEX FILE PATH")
    val multiStreamPagesDumpFile = File("INSERT PAGES FILE PATH")

    // use case: find article by title
    val titleLike = "Java"
    val javaArticleIndex: WikiPageIndex? = multiStreamIndexesFile.asIndexesIterator().use { iter ->
        iter.asSequence().find { it.title.contains(titleLike) }
    }
    println("Java page index: $javaArticleIndex")

    // use case: find page by index
    require(javaArticleIndex != null)
    val javaPageByIndex = getPageByIndex(multiStreamPagesDumpFile, javaArticleIndex)
    println("Java page by index:\n$javaPageByIndex")

    // use case: iterate over every page in dump
    val javaPageFromScan = IndexedDumpIterator(multiStreamIndexesFile, multiStreamPagesDumpFile)
            .use { dumpIterator -> dumpIterator.asSequence().find { it.id == javaArticleIndex.id } }
    println("Java page from scan:\n$javaPageFromScan")
}
```

## SQL

### Get dumps
You need to download two *multistream* files from page like https://dumps.wikimedia.org/enwiki/latest/:
1) Categories file with name like ```ruwiki-latest-category.sql```
2) Category links file with name like ```ruwiki-latest-categorylinks.sql```

#### Notes:
- any language should be ok
- lib was tested on april 2019 dumps version
- only unpacked versions are appropriate

### Java
```java
public class Main {
    public static void main(String[] args) throws IOException {
        final File categoryFile = new File("INSERT CATEGORIES FILE PATH");
        final File categoryLinksFile = new File("INSERT CATEGORY LINKS FILE PATH");

        // use case - find category by title
        final String categoryTitle = "SQL";
        WikiCategory category = null;
        try (final CloseableIterator<WikiCategory> iterator = CategoriesKt.asCategoriesIterator(categoryFile)) {
            while (iterator.hasNext()) {
                final WikiCategory currentCategory = iterator.next();
                if (currentCategory.getTitle().equals(categoryTitle)) {
                    category = currentCategory;
                    break;
                }
            }
        }
        if (category != null) {
            System.out.println("Category: " + category.getTitle() + " is found");
        } else {
            System.out.println("Category: " + categoryTitle + " is not found");
        }

        // use case - count links to category
        if (category != null) {
            int linksCounter = 0;
            try (final CloseableIterator<WikiCategoryLink> iterator = CategoryLinksKt.asCategoryLinksIterator(categoryLinksFile)) {
                while (iterator.hasNext()) {
                    final WikiCategoryLink currentCategory = iterator.next();
                    if (currentCategory.getCategoryTitle().equals(category.getTitle())) {
                        linksCounter++;
                    }
                }
            }
            System.out.println(linksCounter + " links found for category " + category.getTitle());
        }
    }
}
```

### Kotlin
```Kotlin
fun main() {
    val categoryFile = File("INSERT CATEGORIES FILE PATH")
    val categoryLinksFile = File("INSERT CATEGORY LINKS FILE PATH")

    // use case - find category by title
    val categoryTitle = "SQL"
    val category: WikiCategory? =  categoryFile.asCategoriesIterator().use {
        iterator -> iterator.asSequence().find { it.title == categoryTitle }
    }

    if (category != null) {
        println("Category: " + category!!.title + " is found")
    } else {
        println("Category: $categoryTitle is not found")
    }

    // use case - count links to category
    if (category != null) {
        val linksCounter = categoryLinksFile.asCategoryLinksIterator().use {
            iterator -> iterator.asSequence().count { it.categoryTitle == category.title }
        }
        println(linksCounter.toString() + " links found for category " + category!!.title)
    }
}
```

## More

If you want to trade memory for performance in iteration over XMLs:

- first option is to read whole (unpacked) indexes XML with CloseableIterator<WikiPageIndex> as shown in example
and keep it in RAM
- second option is to unpack whole XML wiki dump and iterate over it with UnpackedDumpIterator
(no index-based access available)

## License

Published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0), see LICENSE