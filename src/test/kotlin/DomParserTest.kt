import dom.DomElement
import dom.DomNode
import dom.DomParser
import exceptions.MarkdownSyntaxException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
                    endOffset = 6,
                    children = emptyList()
                )
            )
        )

        assertEquals(
            parser.parse("[text]Hello[/text]"),
            listOf(
                DomNode(
                    DomElement("text", "Hello"),
                    endOffset = 15,
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
                    endOffset = 32,
                    children = emptyList()
                )
            )
        )

        assertEquals(
            parser.parse("[img][/img]"),
            listOf(
                DomNode(
                    DomElement("img", ""),
                    endOffset = 9,
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
                    endOffset = 20,
                    children = listOf(
                        DomNode(
                            DomElement("p", "Test"),
                            endOffset = 12,
                            children = emptyList()
                        )
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
                    endOffset = 49,
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
                    endOffset = 49,
                    children = emptyList()
                )
            )
        )
    }

    @Test
    @DisplayName("Невалидные теги")
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
                    endOffset = 22,
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
                            endOffset = 13 + 18 * i,
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
        val time = measureTime {
            assertEquals(
                parser.parse("[root]${" [parent]${" [child]X[/child]".repeat(1000)}[/parent]".repeat(1000)}[/root]"),
                run {
                    val rootOpenTagLength = 6 // [root]
                    val parentTagLength = 17 // [parent][/parent] + spaces
                    val childTagLength = 16 // [child]X[/child]

                    // Строим дерево снизу вверх:
                    val grandchildren = List(1000) { grandchildIdx ->
                        DomNode(
                            DomElement("child", "X"),
                            endOffset = rootOpenTagLength +
                                    parentTagLength * 1000 * grandchildIdx +
                                    childTagLength * (grandchildIdx + 1),
                            children = emptyList()
                        )
                    }

                    val children = List(1000) { parentIdx ->
                        DomNode(
                            DomElement("parent", ""),
                            endOffset = rootOpenTagLength + parentTagLength * (parentIdx + 1),
                            children = grandchildren
                        )
                    }

                    listOf(
                        DomNode(
                            DomElement("root", ""),
                            endOffset = rootOpenTagLength + parentTagLength * 1000 + 7, // 7 = [/root]
                            children = children
                        )
                    )
                    // Total number of nodes: 1 (root) + 1000 (parents) + 1_000_000 (children) = 1_001_001
                }
            )
        }

        println("Stress test completed in ${time.toString(DurationUnit.MILLISECONDS)}")
    }

    @Test
    @DisplayName("100 children at 100 nodes")
    fun test100NodesIn100Nodes() {
        val time = measureTime {
            assertEquals(
                parser.parse("[root]${" [item]${" [sub]X[/sub]".repeat(100)}[/item]".repeat(100)}[/root]"),
                run {
                    val itemTagLength = 13 // [item][/item] + spaces
                    val subTagLength = 12 // [sub]X[/sub]

                    // Building a tree:
                    val allSubItems = List(100) { itemIdx ->
                        val subItems = List(100) { subIdx ->
                            DomNode(
                                DomElement("sub", "X"),
                                endOffset = 6 + // [root]
                                        itemTagLength * itemIdx +
                                        subTagLength * (subIdx + 1),
                                children = emptyList()
                            )
                        }

                        DomNode(
                            DomElement("item", ""),
                            endOffset = 6 + itemTagLength * (itemIdx + 1),
                            children = subItems
                        )
                    }

                    listOf(
                        DomNode(
                            DomElement("root", ""),
                            endOffset = 6 + itemTagLength * 100 + 7, // [/root]
                            children = allSubItems
                        )
                    )
                    //  Total number of nodes: 1 (root) + 100 (items) + 10_000 (subs) = 10_101
                }
            )
        }

        println("Stress test completed in ${time.toString(DurationUnit.MILLISECONDS)}")
    }
}