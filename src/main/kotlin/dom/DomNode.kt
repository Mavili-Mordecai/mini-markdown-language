package dom

/**
 *
 * @property element
 * @property endOffset The index of the symbol in sanitized markup, which follows the closing square bracket of the tag.
 * For example, `[h][p][/p][/h]`. The `h` endOffset will have 14, and the `p` endOffset will have 10.
 * @property children
 */
data class DomNode(
    val element: DomElement,
    val endOffset: Int,
    val children: List<DomNode> = emptyList()
) {
    override fun hashCode(): Int {
        var result = endOffset
        result = 31 * result + element.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DomNode

        if (element != other.element) return false
        if (children != other.children) return false
        if (endOffset != other.endOffset) return false

        return true
    }
}