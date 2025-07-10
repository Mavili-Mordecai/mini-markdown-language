package printers

import dom.DomNode

interface DomPrinter {
    fun print(dom: List<DomNode>, step: Int = 0)
    fun print(node: DomNode, step: Int = 0)
}