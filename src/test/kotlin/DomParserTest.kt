import dom.DomElement
import dom.DomNode
import dom.DomParser
import exceptions.MarkdownSyntaxException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printers.ConsoleDomPrinterImpl
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class DomParserTest {
    private val parser = DomParser()

    @Test
    @DisplayName("Simple valid tags")
    fun testBasicValidTags() {
        assertEquals(
            parser.parse("[p][/p]"),
            listOf(
                DomNode(
                    DomElement("p", ""),
                    endOffset = 7,
                    children = emptyList()
                )
            )
        )

        assertEquals(
            parser.parse("[text]Hello[/text]"),
            listOf(
                DomNode(
                    DomElement("text", "Hello"),
                    endOffset = 18,
                    children = emptyList()
                )
            )
        )

        assertEquals(
            parser.parse("[тег attr=\"значение\"]Привет[/тег]"),
            listOf(
                DomNode(
                    DomElement(
                        "тег", "Привет",
                        attributes = mapOf("attr" to "значение")
                    ),
                    endOffset = 33,
                    children = emptyList()
                )
            )
        )

        assertEquals(
            parser.parse("[img][/img]"),
            listOf(
                DomNode(
                    DomElement("img", ""),
                    endOffset = 11,
                    children = emptyList()
                )
            )
        )
    }

    @Test
    @DisplayName("Nested Tags")
    fun testNestedTags() {
        assertEquals(
            parser.parse("[div][p]Test[/p][/div]"),
            listOf(
                DomNode(
                    DomElement("div", ""),
                    endOffset = 22,
                    children = listOf(
                        DomNode(
                            DomElement("p", "Test"),
                            endOffset = 16,
                            children = emptyList()
                        )
                    )
                )
            )
        )

        assertEquals(
            parser.parse("[div][text]Text[/text] [p]Child[/p][/div]"),
            listOf(
                DomNode(
                    DomElement("div", ""),
                    endOffset = 41,
                    children = listOf(
                        DomNode(DomElement("text", "Text"), 22),
                        DomNode(DomElement("p", "Child"), 35),
                    )
                )
            )
        )
    }

    @Test
    @DisplayName("Tags with attributes")
    fun testTagsWithAttributes() {
        assertEquals(
            parser.parse("""[link url="https://example.com" target="_blank"]Click[/link]"""),
            listOf(
                DomNode(
                    DomElement(
                        "link",
                        "Click",
                        attributes = mapOf(
                            "url" to "https://example.com",
                            "target" to "_blank"
                        )
                    ),
                    endOffset = 60,
                    children = emptyList()
                )
            )
        )

        assertEquals(
            parser.parse("""[link url="https://example.com" target="_blank" url="https://example-two.com"]Click[/link]"""),
            listOf(
                DomNode(
                    DomElement(
                        "link",
                        "Click",
                        attributes = mapOf(
                            "url" to "https://example-two.com",
                            "target" to "_blank"
                        )
                    ),
                    endOffset = 90,
                    children = emptyList()
                )
            )
        )
    }

    @Test
    @DisplayName("Invalid tags")
    fun testInvalidTags() {
        // An unclosed tag
        assertThrows<MarkdownSyntaxException> {
            parser.parse("[p]Test")
        }

        // Incorrect nesting
        assertThrows<MarkdownSyntaxException> {
            parser.parse("[p][link]Test[/link]Error[/p]")
        }

        assertThrows<MarkdownSyntaxException> {
            parser.parse("[div][p]Text[/div][/p]")
        }

        // Duplicate attributes
        assertThrows<MarkdownSyntaxException> {
            parser.parse("""[tag a="1" a="2"]""")
        }

        // An unclosed tag
        assertThrows<MarkdownSyntaxException> {
            parser.parse("[p]Test")
        }

        // Unscreened quotes
        assertThrows<MarkdownSyntaxException> {
            parser.parse("""[tag attr="value][/tag]""")
        }

        // Incorrect characters in tag name
        assertThrows<MarkdownSyntaxException> {
            parser.parse("[tag!][/tag!]")
        }

        // Spaces in tag name
        assertThrows<MarkdownSyntaxException> {
            parser.parse("[my tag][/my tag]")
        }
    }

    @Test
    @DisplayName("Boundary cases")
    fun testEdgeCases() {
        // Minimum tag
        assertThrows<MarkdownSyntaxException> {
            parser.parse("[]")
        }

        // Very long attributes
        assertThrows<MarkdownSyntaxException> {
            parser.parse("""[tag ${"a".repeat(1000)}="value"]""")
        }

        // Special characters in content
        assertEquals(
            parser.parse("[p]Line 1\nLine 2\tTab[/p]"),
            listOf(
                DomNode(
                    DomElement("p", "Line 1\nLine 2\tTab"),
                    endOffset = 24,
                    children = emptyList()
                )
            )
        )
    }

    @Test
    @DisplayName("Large number of children")
    fun testToMuchChildren() {
        val time = measureTime {
            assertEquals(
                parser.parse("[root]${" [item]Test[/item]".repeat(1000)}[/root]"),
                run {
                    val items = List(1000) { i ->
                        DomNode(
                            DomElement("item", "Test"),
                            endOffset = 24 + 18 * i,
                            children = emptyList()
                        )
                    }
                    listOf(
                        DomNode(
                            DomElement("root", ""),
                            endOffset = 18013,
                            children = items
                        )
                    )
                }
            )
        }

        println("Stress test completed in ${time.toString(DurationUnit.MILLISECONDS)}")
    }

    @Test
    @DisplayName("A million dom-nodes")
    fun testMillionNodes() {
        val input = "[root]${" [parent]${" [child]X[/child]".repeat(1000)}[/parent]".repeat(1000)}[/root]"
        var result: List<DomNode> = emptyList()

        val time = measureTime {
            result = parser.parse(input)
        }

        assertEquals(1, result.size)
        assertEquals(1000, result[0].children.size)
        result[0].children.forEach { parent ->
            assertEquals(1000, parent.children.size)
        }

        println("Stress test completed in ${time.toString(DurationUnit.MILLISECONDS)}")
    }

    @Test
    @DisplayName("100 children at 100 nodes")
    fun test100NodesIn100Nodes() {
        val input = "[root]${" [item]${" [sub]X[/sub]".repeat(100)}[/item]".repeat(100)}[/root]"
        var result: List<DomNode> = emptyList()

        val time = measureTime {
            result = parser.parse(input)
        }

        assertEquals(1, result.size)
        assertEquals(100, result[0].children.size)
        result[0].children.forEach { item ->
            assertEquals(100, item.children.size)
        }

        println("Stress test completed in ${time.toString(DurationUnit.MILLISECONDS)}")
    }

    @Test
    @DisplayName("Multiline basic tags")
    fun testMultilineBasicTags() {
        assertEquals(
            parser.parse("""
            [div]
              [p]First line[/p]
              [p]Second line[/p]
            [/div]
        """),
            listOf(
                DomNode(
                    DomElement("div", ""),
                    endOffset = 53,
                    children = listOf(
                        DomNode(
                            DomElement("p", "First line"),
                            endOffset = 25,
                            children = emptyList()
                        ),
                        DomNode(
                            DomElement("p", "Second line"),
                            endOffset = 46,
                            children = emptyList()
                        )
                    )
                )
            )
        )
    }

    @Test
    @DisplayName("Mixed single-line and multiline tags")
    fun testMixedSingleAndMultilineTags() {
        assertEquals(
            parser.parse("""
            [header]
              [title]Document[/title]
            [/header]
            [main][p]Content[/p][/main]
        """),
            listOf(
                DomNode(
                    DomElement("header", ""),
                    endOffset = 44,
                    children = listOf(
                        DomNode(
                            DomElement("title", "Document"),
                            endOffset = 34,
                            children = emptyList()
                        )
                    )
                ),
                DomNode(
                    DomElement("main", ""),
                    endOffset = 72,
                    children = listOf(
                        DomNode(
                            DomElement("p", "Content"),
                            endOffset = 65,
                            children = emptyList()
                        )
                    )
                )
            )
        )
    }

    @Test
    @DisplayName("Complex nested multiline structure")
    fun testComplexNestedMultilineStructure() {
        assertEquals(
            parser.parse("""
            [article]
              [header]
                [h1]Title[/h1]
              [/header]
              [section]
                [p]Paragraph 1[/p]
                [p]
                  [span]Nested[/span]
                  [text]text[/text]
                [/p]
              [/section]
            [/article]
        """),
            listOf(
                DomNode(
                    DomElement("article", ""),
                    endOffset = 177,
                    children = listOf(
                        DomNode(
                            DomElement("header", ""),
                            endOffset = 51,
                            children = listOf(
                                DomNode(
                                    DomElement("h1", "Title"),
                                    endOffset = 39,
                                    children = emptyList()
                                )
                            )
                        ),
                        DomNode(
                            DomElement("section", ""),
                            endOffset = 166,
                            children = listOf(
                                DomNode(
                                    DomElement("p", "Paragraph 1"),
                                    endOffset = 86,
                                    children = emptyList()
                                ),
                                DomNode(
                                    DomElement("p", ""),
                                    endOffset = 153,
                                    children = listOf(
                                        DomNode(
                                            DomElement("span", "Nested"),
                                            endOffset = 120,
                                            children = emptyList()
                                        ),
                                        DomNode(
                                            DomElement("text", "text"),
                                            endOffset = 144,
                                            children = emptyList()
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    @DisplayName("Multiline with attributes")
    fun testMultilineWithAttributes() {
        assertEquals(
            parser.parse("""
            [div class="container"]
              [p align="center"]Centered text[/p]
              [img src="image.jpg"][/img]
            [/div]
        """),
            listOf(
                DomNode(
                    DomElement("div", "", attributes = hashMapOf("class" to "container")),
                    endOffset = 98,
                    children = listOf(
                        DomNode(
                            DomElement("p", "Centered text", attributes = hashMapOf("align" to "center")),
                            endOffset = 61,
                            children = emptyList()
                        ),
                        DomNode(
                            DomElement("img", "", attributes = hashMapOf("src" to "image.jpg")),
                            endOffset = 91,
                            children = emptyList()
                        )
                    )
                )
            )
        )
    }
}