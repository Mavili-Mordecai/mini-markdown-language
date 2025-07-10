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

        ConsoleDomPrinterImpl().print(parser.parse("[h][p][/p][/h]"))

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
}