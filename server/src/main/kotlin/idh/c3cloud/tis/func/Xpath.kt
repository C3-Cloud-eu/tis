package idh.c3cloud.tis.func

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

private val builderFactory = DocumentBuilderFactory.newInstance()

fun evalXpathExpr(input: String, expr: String): String {
    val builder = builderFactory.newDocumentBuilder()
    val xmlDocument = builder.parse(ByteArrayInputStream(input.toByteArray()))
    val xpath = XPathFactory.newInstance().newXPath()
    return xpath.compile(expr).evaluate(xmlDocument)
}
