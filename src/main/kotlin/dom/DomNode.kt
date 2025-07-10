package dom

data class DomNode(
    val element: DomElement,
    val endOffset: Int,
    val children: List<DomNode> = emptyList()
) {
    override fun toString(): String {
        return "dom.DomNode(tag=${element.tag}, content=${element.content}, attributes=${element.attributes})"
    }

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

        return true
    }
}