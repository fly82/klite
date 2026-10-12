package klite.xml

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node.*
import org.xml.sax.InputSource
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

fun secureDocumentBuilderFactory(block: DocumentBuilderFactory.() -> Unit = {}) = DocumentBuilderFactory.newInstance().apply {
  isNamespaceAware = true
  isXIncludeAware = false
  setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
  setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
  setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
  block()
}

fun DocumentBuilderFactory.parse(xml: String): Document = newDocumentBuilder().parse(InputSource(xml.reader()))

internal fun Element.toXmlElement(): XmlElement {
  val attrs = if (attributes.length == 0) null else (0 until attributes.length).associate { i ->
    val a = attributes.item(i)
    "@${a.localName?.takeIf(String::isNotEmpty) ?: a.nodeName}" to a.nodeValue
  }
  val text = StringBuilder()
  val children = mutableListOf<XmlElement>()
  var n = firstChild
  while (n != null) {
    when (n.nodeType) {
      ELEMENT_NODE -> children += (n as Element).toXmlElement()
      TEXT_NODE, CDATA_SECTION_NODE -> text.append(n.textContent)
    }
    n = n.nextSibling
  }
  return XmlElement(localName?.takeIf(String::isNotEmpty) ?: tagName, attrs,
    text.trim().takeIf { it.isNotEmpty() }?.toString(), children)
}
