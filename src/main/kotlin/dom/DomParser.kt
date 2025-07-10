package dom

import exceptions.MarkdownSyntaxException

class DomParser {
    fun parse(markdown: String): MutableList<DomNode> {
        val sanitizedMarkdown = sanitize(markdown)
        if (sanitizedMarkdown.isBlank()) return mutableListOf()

        val nodes: MutableList<DomNode> = mutableListOf()
        var lp = 0

        while (lp < sanitizedMarkdown.length) {
            val nextNode = getNextNode(sanitizedMarkdown, lp)
            if (nextNode == null) break
            lp = nextNode.endOffset
            nodes.add(nextNode)
        }

        return nodes
    }

    private fun getNextNode(markdown: String, offset: Int): DomNode? {
        if (markdown.isBlank()) return null

        val name = StringBuilder()
        val content = StringBuilder()
        var attributes: Map<String, String> = emptyMap()
        val children: MutableList<DomNode> = mutableListOf()

        val closedTag = StringBuilder()
        val attributesLine = StringBuilder()

        // Ищем начало тега
        var lp = getNextNonEmptyIndex(markdown, offset)

        if (markdown[lp] != '[')
            throw MarkdownSyntaxException("The opening tag was expected.")

        lp++

        if (markdown[lp] == '/')
            throw MarkdownSyntaxException("An open tag was expected, but a closing tag was encountered.")

        // Собираем название тега
        while (markdown[lp] != ' ' && markdown[lp] != ']') {
            if (isKeySymbolInvalid(markdown[lp]))
                throw MarkdownSyntaxException("Invalid character: ${markdown[lp]}")
            name.append(markdown[lp++])
        }

        // Если тег не закрыт, тогда собираем и парсим атрибуты
        if (markdown[lp] != ']') {
            // Собираем атрибуты
            while (markdown[lp] != ']') attributesLine.append(markdown[lp++])

            attributes = parseAttributes(attributesLine.toString())
        }

        lp++

        // Собираем содержимое тега
        while (lp < markdown.length && markdown[lp] != '[') content.append(markdown[lp++])

        var closedTagStartIndex = lp

        // Собираем закрывающийся тег
        while (lp < markdown.length && markdown[lp] != ']') closedTag.append(markdown[lp++])

        if (lp >= markdown.length)
            throw MarkdownSyntaxException("There is no closing tag for the opening `$name` tag.")

        closedTag.append(markdown[lp])

        // Ищем нужные закрывающий тег и собираем дочерние узлы
        while (lp < markdown.length - 1 && closedTag.toString() != "[/$name]") {
            if (closedTag[1] == '/')
                throw MarkdownSyntaxException("There is no closing tag for the opening `$name` tag.")

            val node = getNextNode(markdown, closedTagStartIndex)

            if (node == null) throw MarkdownSyntaxException("There is no closing tag for the opening `$name` tag.")

            children.add(node)

            lp = node.endOffset
            lp = getNextNonEmptyIndex(markdown, lp)

            closedTag.clear()

            closedTagStartIndex = lp

            // Собираем закрывающийся тег
            while (lp < markdown.length && markdown[lp] != ']') closedTag.append(markdown[lp++])

            if (lp >= markdown.length)
                throw MarkdownSyntaxException("There is no closing tag for the opening `$name` tag.")

            closedTag.append(markdown[lp])
        }

        if (lp >= markdown.length - 1 && closedTag.toString() != "[/$name]")
            throw MarkdownSyntaxException("There is no closing tag for the opening `$name` tag.")

        lp++

        return DomNode(DomElement(name.toString(), content.trim().toString(), attributes), lp, children)
    }

    private fun parseAttributes(attributesLine: String): Map<String, String> {
        val attributes = hashMapOf<String, String>()

        var lp = 0
        val key = StringBuilder()
        val value = StringBuilder()

        while (lp < attributesLine.length) {
            if (attributesLine[lp] != ' ')
                throw MarkdownSyntaxException("An indentation between attributes was expected.")

            lp = getNextNonEmptyIndex(attributesLine, lp)

            // Ищем знак =
            while (lp < attributesLine.length && attributesLine[lp] != '=' && attributesLine[lp] != ' ') {
                if (isKeySymbolInvalid(attributesLine[lp]))
                    throw MarkdownSyntaxException("Invalid character: ${attributesLine[lp]}")

                key.append(attributesLine[lp++])
            }

            lp = getNextNonEmptyIndex(attributesLine, lp)

            if (lp >= attributesLine.length || attributesLine[lp] != '=')
                throw MarkdownSyntaxException("An assignment symbol was expected.")

            lp++

            // Ищем открывающие кавычки
            lp = getNextNonEmptyIndex(attributesLine, lp)

            if (lp > attributesLine.length - 1 || (attributesLine[lp] != '\'' && attributesLine[lp] != '"'))
                throw MarkdownSyntaxException("The quotation mark character was expected.")

            val quoteMark = attributesLine[lp++]

            // Ищем закрывающие кавычки
            while (lp < attributesLine.length && attributesLine[lp] != quoteMark)
                value.append(attributesLine[lp++])

            if (lp >= attributesLine.length || attributesLine[lp] != quoteMark)
                throw MarkdownSyntaxException("Unclosed quotation marks.")

            attributes.put(key.toString().lowercase(), value.toString())
            key.clear()
            value.clear()

            lp++
        }

        return attributes
    }

    private fun getNextNonEmptyIndex(markdown: String, offset: Int): Int {
        var lp = offset
        while (lp < markdown.length && markdown[lp].isWhitespace()) lp++
        return lp
    }

    private fun sanitize(markdown: String): String = markdown.trim()

    private fun isKeySymbolInvalid(ch: Char): Boolean {
        return !(ch.isLetterOrDigit() || ch == '-' || ch == '_')
    }
}