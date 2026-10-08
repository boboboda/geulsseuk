package com.buyoungsilcoding.geulsseuk.core.naver

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmlRpcTest {

    @Test
    fun buildsCallWithEscapingAndBase64() {
        val xml = XmlRpc.buildCall(
            "metaWeblog.newPost",
            listOf(
                XmlRpcValue.Str("a<b>&\"c\""),
                XmlRpcValue.Bool(false),
                XmlRpcValue.Struct(linkedMapOf("bits" to XmlRpcValue.Bytes(byteArrayOf(1, 2, 3)))),
            ),
        )
        assertTrue("<methodName>metaWeblog.newPost</methodName>" in xml)
        assertTrue("<string>a&lt;b&gt;&amp;&quot;c&quot;</string>" in xml)
        assertTrue("<boolean>0</boolean>" in xml)
        assertTrue("<base64>AQID</base64>" in xml)
    }

    @Test
    fun stripsControlCharsButKeepsEmoji() {
        val s = XmlRpc.escape("가\u0000나\u0008다\n😀라\uD800")
        assertEquals("가나다\n😀라", s) // 제어문자·짝 없는 서로게이트 제거, 이모지 유지
        // 결과 XML이 실제로 파싱 가능한지도 확인
        val xml = "<methodResponse><params><param><value><string>$s</string></value></param></params></methodResponse>"
        assertEquals(XmlRpcValue.Str(s), XmlRpc.parseResponse(xml))
    }

    @Test
    fun parsesStructWithUrl() {
        val xml = """<?xml version="1.0"?><methodResponse><params><param><value><struct>
            <member><name>url</name><value><string>https://blogfiles.example/a.jpg</string></value></member>
            </struct></value></param></params></methodResponse>"""
        val v = XmlRpc.parseResponse(xml) as XmlRpcValue.Struct
        assertEquals(XmlRpcValue.Str("https://blogfiles.example/a.jpg"), v.members["url"])
    }

    @Test
    fun parsesUntypedStringAndIntAndBase64() {
        val untyped = "<methodResponse><params><param><value>12345</value></param></params></methodResponse>"
        assertEquals(XmlRpcValue.Str("12345"), XmlRpc.parseResponse(untyped))

        val int = "<methodResponse><params><param><value><int>77</int></value></param></params></methodResponse>"
        assertEquals(XmlRpcValue.Int32(77), XmlRpc.parseResponse(int))

        val b64 = "<methodResponse><params><param><value><base64>AQID\n</base64></value></param></params></methodResponse>"
        assertContentEquals(byteArrayOf(1, 2, 3), (XmlRpc.parseResponse(b64) as XmlRpcValue.Bytes).bytes)
    }

    @Test
    fun faultBecomesException() {
        val xml = """<methodResponse><fault><value><struct>
            <member><name>faultCode</name><value><int>-32602</int></value></member>
            <member><name>faultString</name><value><string>invalid password</string></value></member>
            </struct></value></fault></methodResponse>"""
        val e = assertFailsWith<XmlRpcFault> { XmlRpc.parseResponse(xml) }
        assertEquals(-32602, e.code)
        assertEquals("invalid password", e.faultString)
    }

    @Test
    fun rejectsDoctypeAndGarbage() {
        val xxe = """<?xml version="1.0"?><!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]>
            <methodResponse><params><param><value><string>&e;</string></value></param></params></methodResponse>"""
        assertFailsWith<XmlRpcProtocolException> { XmlRpc.parseResponse(xxe) }
        assertFailsWith<XmlRpcProtocolException> { XmlRpc.parseResponse("<html>not xmlrpc</html>") }
        assertFailsWith<XmlRpcProtocolException> { XmlRpc.parseResponse("") }
        assertFalse(runCatching { XmlRpc.parseResponse(xxe) }.isSuccess)
    }
}
