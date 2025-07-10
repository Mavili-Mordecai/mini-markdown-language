import dom.DomElement
import dom.DomNode
import dom.DomParser
import exceptions.MarkdownSyntaxException
import printers.ConsoleDomPrinterImpl
import printers.DomPrinter
import kotlin.time.DurationUnit
import kotlin.time.measureTime

fun main() {
    val testCases: Map<String, List<DomNode>?> = mapOf(
        // ---------------------------
        // Валидные базовые случаи
        // ---------------------------
        "[p][/p]" to listOf(
            DomNode(
                DomElement("p", ""),
                endOffset = 6,
                children = emptyList()
            )
        ),

        "[text]Hello[/text]" to listOf(
            DomNode(
                DomElement("text", "Hello"),
                endOffset = 15,
                children = emptyList()
            )
        ),

        // Вложенные теги
        "[div][p]Test[/p][/div]" to listOf(
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
        ),

        // Теги с атрибутами
        """[link url="https://example.com" target="_blank"]Click[/link]""" to listOf(
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
        ),

        // Пустые теги
        "[img][/img]" to listOf(
            DomNode(
                DomElement("img", ""),
                endOffset = 9,
                children = emptyList()
            )
        ),

        // ---------------------------
        // Граничные случаи
        // ---------------------------
        // Минимальный тег
        "[]" to null,

        // Очень длинные атрибуты
        """[tag ${"a".repeat(1000)}="value"]""" to null,

        // Спецсимволы в контенте
        "[p]Line 1\nLine 2\tTab[/p]" to listOf(
            DomNode(
                DomElement("p", "Line 1\nLine 2\tTab"),
                endOffset = 22,
                children = emptyList()
            )
        ),

        // Unicode в тегах и атрибутах
        "[тег attr=\"значение\"]Привет[/тег]" to listOf(
            DomNode(
                DomElement(
                    "тег", "Привет",
                    attributes = mapOf("attr" to "значение")
                ),
                endOffset = 32,
                children = emptyList()
            )
        ),

        // ---------------------------
        // Невалидные случаи
        // ---------------------------
        // Незакрытый тег
        "[p]Test" to null,

        // Неправильная вложенность
        "[div][p]Text[/div][/p]" to null,

        // Дублирование атрибутов
        """[tag a="1" a="2"]""" to null,

        // Неэкранированные кавычки
        """[tag attr="value][/tag]""" to null,

        // Неправильные символы в имени тега
        "[tag!][/tag!]" to null,

        // Пробелы в имени тега
        "[my tag][/my tag]" to null,

        // ---------------------------
        // Стресс-тесты
        // ---------------------------
        // Большой документ
        "[root]${" [item]Test[/item]".repeat(1000)}[/root]" to run {
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
        },

        "[root]${" [parent]${" [child]X[/child]".repeat(1000)}[/parent]".repeat(1000)}[/root]" to run {
            val rootOpenTagLength = 6 // [root]
            val parentTagLength = 17 // [parent][/parent] + пробелы
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
            // Общее число узлов: 1 (root) + 1000 (parents) + 1_000_000 (children) = 1_001_001
        },

        "[root]${" [item]${" [sub]X[/sub]".repeat(100)}[/item]".repeat(100)}[/root]" to run {
            val itemTagLength = 13 // [item][/item] + пробелы
            val subTagLength = 12 // [sub]X[/sub]

            // Строим дерево:
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
            // Общее число узлов: 1 (root) + 100 (items) + 10_000 (subs) = 10_101
        }
    )

    val parser = DomParser()
    var number = 1
    var numberOfFailed = 0

    testCases.forEach { testCase ->
        var nodes: List<DomNode>?

        val time = measureTime {
            nodes = try {
                parser.parse(testCase.key)
            } catch (ex: MarkdownSyntaxException) {
                null
            }
        }

        val isPass = nodes == testCase.value

        numberOfFailed += if (isPass) 0 else 1
        println("\n===Test case #${number++}===")
        println("Result: \t${(if (isPass) "Success" else "Failed")}")
        println("Time: ${time.toString(DurationUnit.MILLISECONDS)}")

        println("Expected: ${testCase.value?.size}")
        //printer.print(testCase.value ?: emptyList(), 1)
        println("Received: ${nodes?.size}")
        //printer.print(nodes ?: emptyList(), 1)
    }

    println("\nTotal failed test cases: $numberOfFailed")
}
