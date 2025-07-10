package printers

import dom.DomNode

class ConsoleDomPrinterImpl : DomPrinter {
    override fun print(dom: List<DomNode>, step: Int) {
        dom.forEach { print(it, step) }
    }

    override fun print(node: DomNode, step: Int) {
        println(" ".repeat(step * 4) + node)
        if (node.children.isNotEmpty()) node.children.forEach { print(it, step + 1) }
    }
}