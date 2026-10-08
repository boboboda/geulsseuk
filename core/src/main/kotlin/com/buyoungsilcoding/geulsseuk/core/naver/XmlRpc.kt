package com.buyoungsilcoding.geulsseuk.core.naver

import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

/** XML-RPC에서 쓰는 값 종류 중 이 앱에 필요한 것만 담았습니다. */
sealed interface XmlRpcValue {
    data class Str(val value: String) : XmlRpcValue
    data class Int32(val value: Int) : XmlRpcValue
    data class Bool(val value: Boolean) : XmlRpcValue
    class Bytes(val bytes: ByteArray) : XmlRpcValue
    data class Struct(val members: Map<String, XmlRpcValue>) : XmlRpcValue
    data class ListOf(val items: List<XmlRpcValue>) : XmlRpcValue
}

/** 서버가 "fault"로 거절한 경우 (암호 오류, 권한 오류 등). */
class XmlRpcFault(val code: Int, val faultString: String) :
    Exception("XML-RPC fault $code: $faultString")

/** 응답이 XML-RPC 형식이 아니거나 읽을 수 없는 경우. */
class XmlRpcProtocolException(message: String, cause: Throwable? = null) : Exception(message, cause)

object XmlRpc {

    fun buildCall(method: String, params: List<XmlRpcValue>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<methodCall><methodName>").append(escape(method)).append("</methodName><params>")
        for (p in params) {
            append("<param>")
            appendValue(p)
            append("</param>")
        }
        append("</params></methodCall>")
    }

    private fun StringBuilder.appendValue(v: XmlRpcValue) {
        append("<value>")
        when (v) {
            is XmlRpcValue.Str -> append("<string>").append(escape(v.value)).append("</string>")
            is XmlRpcValue.Int32 -> append("<int>").append(v.value).append("</int>")
            is XmlRpcValue.Bool -> append("<boolean>").append(if (v.value) "1" else "0").append("</boolean>")
            is XmlRpcValue.Bytes -> append("<base64>")
                .append(java.util.Base64.getEncoder().encodeToString(v.bytes)).append("</base64>")
            is XmlRpcValue.Struct -> {
                append("<struct>")
                for ((k, m) in v.members) {
                    append("<member><name>").append(escape(k)).append("</name>")
                    appendValue(m)
                    append("</member>")
                }
                append("</struct>")
            }
            is XmlRpcValue.ListOf -> {
                append("<array><data>")
                v.items.forEach { appendValue(it) }
                append("</data></array>")
            }
        }
        append("</value>")
    }

    /** XML에서 쓸 수 없는 제어 문자는 버리고, 특수문자는 치환합니다. (AI가 만든 글에 섞여도 요청이 깨지지 않게) */
    fun escape(s: String): String {
        val sb = StringBuilder(s.length + 16)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '&' -> sb.append("&amp;")
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '"' -> sb.append("&quot;")
                c == '\'' -> sb.append("&apos;")
                c == '\t' || c == '\n' || c == '\r' -> sb.append(c)
                c < ' ' -> Unit
                c.isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate() -> {
                    sb.append(c).append(s[i + 1])
                    i++
                }
                c.isSurrogate() -> Unit
                c == '￾' || c == '￿' -> Unit
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    /** 성공 응답의 값을 돌려주고, fault 응답이면 [XmlRpcFault]를 던집니다. */
    fun parseResponse(xml: String): XmlRpcValue {
        val root = try {
            newBuilder().parse(InputSource(StringReader(xml))).documentElement
        } catch (e: Exception) {
            throw XmlRpcProtocolException("응답을 XML로 읽지 못했어요.", e)
        }
        if (root.tagName != "methodResponse") throw XmlRpcProtocolException("XML-RPC 응답이 아니에요.")

        root.child("fault")?.let { fault ->
            val struct = fault.child("value")?.let { parseValue(it) } as? XmlRpcValue.Struct
            val code = (struct?.members?.get("faultCode") as? XmlRpcValue.Int32)?.value ?: 0
            val msg = (struct?.members?.get("faultString") as? XmlRpcValue.Str)?.value.orEmpty()
            throw XmlRpcFault(code, msg)
        }

        val value = root.child("params")?.child("param")?.child("value")
            ?: throw XmlRpcProtocolException("응답에 값이 없어요.")
        return parseValue(value)
    }

    private fun parseValue(value: Element): XmlRpcValue {
        val typed = value.children().firstOrNull() ?: return XmlRpcValue.Str(value.textContent.orEmpty())
        return when (typed.tagName) {
            "string" -> XmlRpcValue.Str(typed.textContent.orEmpty())
            "int", "i4", "i8" -> XmlRpcValue.Int32(typed.textContent.trim().toIntOrNull() ?: 0)
            "boolean" -> XmlRpcValue.Bool(typed.textContent.trim() == "1")
            "base64" -> XmlRpcValue.Bytes(java.util.Base64.getMimeDecoder().decode(typed.textContent.trim()))
            "struct" -> XmlRpcValue.Struct(
                typed.children().filter { it.tagName == "member" }.mapNotNull { m ->
                    val name = m.child("name")?.textContent?.trim() ?: return@mapNotNull null
                    val v = m.child("value") ?: return@mapNotNull null
                    name to parseValue(v)
                }.toMap()
            )
            "array" -> XmlRpcValue.ListOf(
                typed.child("data")?.children()?.filter { it.tagName == "value" }?.map { parseValue(it) }.orEmpty()
            )
            else -> XmlRpcValue.Str(typed.textContent.orEmpty())
        }
    }

    private fun Element.children(): List<Element> {
        val out = mutableListOf<Element>()
        var n: Node? = firstChild
        while (n != null) {
            if (n is Element) out += n
            n = n.nextSibling
        }
        return out
    }

    private fun Element.child(name: String): Element? = children().firstOrNull { it.tagName == name }

    /** 외부 엔티티·DOCTYPE를 막아 악의적인 XML 응답으로부터 보호합니다. */
    private fun newBuilder(): DocumentBuilder {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = false
        for ((feature, value) in listOf(
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
        )) {
            try {
                f.setFeature(feature, value)
            } catch (e: Exception) {
                // 일부 안드로이드 파서는 일부 옵션을 지원하지 않습니다. 지원되는 옵션만 적용합니다.
            }
        }
        try {
            f.isXIncludeAware = false
            f.setExpandEntityReferences(false)
        } catch (e: Exception) {
            // 위와 같은 이유로 무시합니다.
        }
        return f.newDocumentBuilder()
    }
}
