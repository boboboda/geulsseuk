#!/usr/bin/env python3
"""로컬 모의 XML-RPC 서버. 테스트 스크립트의 요청 형식과 흐름만 검증합니다.
(실제 네이버 서버의 동작을 보장하지 않습니다.)

실행: python3 mock_server.py [포트=8765]
"""
import sys
from xmlrpc.server import SimpleXMLRPCServer, SimpleXMLRPCRequestHandler

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
EXPECTED_PW = "test-pass"
posts = []


def _auth(blog_id, user, pw):
    if pw != EXPECTED_PW:
        raise Exception("invalid api password")


def new_media_object(blog_id, user, pw, struct):
    _auth(blog_id, user, pw)
    assert {"name", "type", "bits"} <= set(struct), "필수 필드 누락"
    size = len(struct["bits"].data)
    print(f"[mock] media {struct['name']} ({struct['type']}, {size} bytes)", flush=True)
    return {"url": f"https://mock.example/{blog_id}/{struct['name']}"}


def new_post(blog_id, user, pw, struct, publish):
    _auth(blog_id, user, pw)
    assert "title" in struct and "description" in struct, "필수 필드 누락"
    posts.append((struct, publish))
    print(f"[mock] post title={struct['title']!r} publish={publish} "
          f"imgs={struct['description'].count('<img')}", flush=True)
    return str(len(posts))


class Handler(SimpleXMLRPCRequestHandler):
    rpc_paths = ("/xmlrpc",)


with SimpleXMLRPCServer(("127.0.0.1", PORT), requestHandler=Handler,
                        logRequests=False, allow_none=True) as server:
    server.register_function(new_media_object, "metaWeblog.newMediaObject")
    server.register_function(new_post, "metaWeblog.newPost")
    print(f"[mock] listening on 127.0.0.1:{PORT}", flush=True)
    server.serve_forever()
