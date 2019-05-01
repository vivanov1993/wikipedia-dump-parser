# JVM wikipeadia dumps parser

Here you can find simple Kotlin/Java library to work with wiki .xml(.bz2) dumps from https://dumps.wikimedia.org/enwiki/latest/.
Library is written in Kotlin but can be used from pure java code as well

## Table of content

- [Installation](#installation)
- [Quickstart](#quickstart)   
- [More](#more)   
- [License](#license)

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
    
Note: to work with .BZ2 Files https://mvnrepository.com/artifact/org.apache.commons/commons-compress also required

## Quickstart

### Get dumps
You need to download two *multistream* files from page like https://dumps.wikimedia.org/enwiki/latest/:
1) Index file with name like ```ruwiki-latest-pages-articles-multistream-index.txt.bz2```
2) Pages file with name like ```ruwiki-latest-pages-articles-multistream.xml.bz2```

#### Notes:
- any language should be ok
- lib was tested on april 2019 dumps version
- both packed and unpacked index file can be used. Unpacked index traversal was is ~16 times faster on my average proc/hdd
- as there is work with archives all lib operations may be considered "computations-heavy" 

### Parse dumps

#### Java
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
        final WikiPage javaPageByIndex = PagesKt.getPageByIndex(multiStreamPagesDumpFile, javaArticleIndex);
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

#### Kotlin
```Kotlin
fun main() {
    // files to work with
    val multiStreamIndexesFile = File("P:\\data\\wiki26\\ruwiki-latest-pages-articles-multistream-index.txt")
    val multiStreamPagesDumpFile = File("P:\\data\\wiki26\\ruwiki-latest-pages-articles-multistream.xml.bz2")

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

## More

I want to trade memory for performance

- first option is to read whole (unpacked) indexes XML with CloseableIterator<WikiPageIndex> as shown in example
and keep it in RAM
- second option is unpack whole XML wiki dump (NOT multistream) and iterate over it with UnpackedDumpIterator
(no index-based access available)

## License

Published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0), see LICENSE