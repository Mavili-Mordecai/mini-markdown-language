package dom

data class DomElement(
    var tag: String,
    var content: String,
    var attributes: Map<String, String> = emptyMap()
)