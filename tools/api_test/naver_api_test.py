#!/usr/bin/env python3
"""네이버 블로그 글쓰기 API(XML-RPC, MetaWeblog) 동작 확인용 테스트 스크립트.

목적: 앱을 만들기 전에 "지금도 이 방식으로 글이 올라가는지"를 먼저 확인합니다.
표준 라이브러리만 사용하므로 별도 설치가 필요 없습니다. (Python 3.8+)

준비
  1) 네이버 블로그 관리 > 글쓰기 API 연결 화면에서 'API 연결 암호'를 발급받습니다.
     주소: https://admin.blog.naver.com/<네이버ID>/config/api
     (로그인 비밀번호가 아니라, 이 화면에서 따로 만드는 암호입니다.)
  2) 암호는 명령줄에 쓰지 말고 환경변수 또는 입력창으로 넘기세요. (기록에 남지 않게)

사용 예
  export NAVER_API_PW='발급받은암호'
  python3 naver_api_test.py --id 내네이버ID                      # 글만 올려보기 (비공개 시도)
  python3 naver_api_test.py --id 내네이버ID --image 사진.jpg      # 사진 업로드 + 글 올려보기
  python3 naver_api_test.py --id 내네이버ID --publish            # 공개로 올리기 (주의)

기본값은 '공개하지 않음(publish=False)'입니다. 이 값이 네이버에서 임시저장/비공개 중
무엇으로 처리되는지는 확인되지 않았으므로, 실행 후 블로그 관리 화면에서 직접 확인하세요.
"""
import argparse
import getpass
import html
import mimetypes
import os
import sys
import xmlrpc.client

DEFAULT_ENDPOINT = "https://api.blog.naver.com/xmlrpc"


def make_client(endpoint: str) -> xmlrpc.client.ServerProxy:
    return xmlrpc.client.ServerProxy(endpoint, encoding="utf-8", allow_none=True)


def upload_image(client, blog_id, password, path):
    """metaWeblog.newMediaObject: 사진 1장 업로드 후 주소를 돌려받습니다."""
    with open(path, "rb") as f:
        data = f.read()
    mime = mimetypes.guess_type(path)[0] or "image/jpeg"
    payload = {
        "name": os.path.basename(path),
        "type": mime,
        "bits": xmlrpc.client.Binary(data),
    }
    result = client.metaWeblog.newMediaObject(blog_id, blog_id, password, payload)
    # 서버에 따라 {'url': '...'} 구조 또는 문자열로 돌아올 수 있어 둘 다 처리합니다.
    if isinstance(result, dict):
        return result.get("url") or result.get("URL") or str(result)
    return str(result)


def create_post(client, blog_id, password, title, body_html, publish, tags=None):
    """metaWeblog.newPost: 글 등록."""
    struct = {"title": title, "description": body_html}
    if tags:
        struct["tags"] = tags  # 네이버가 이 필드를 받는지는 확인 대상입니다.
    return client.metaWeblog.newPost(blog_id, blog_id, password, struct, bool(publish))


def build_html(title, image_urls):
    parts = [
        f"<p>{html.escape(title)} — API 연결 테스트 글입니다.</p>",
        "<p>이 글은 자동 글쓰기 앱 개발 중 동작 확인을 위해 올린 테스트입니다. 확인 후 삭제해도 됩니다.</p>",
    ]
    for url in image_urls:
        parts.append(f'<p><img src="{html.escape(url, quote=True)}" /></p>')
    return "\n".join(parts)


def main() -> int:
    ap = argparse.ArgumentParser(description="네이버 블로그 XML-RPC 글쓰기 테스트")
    ap.add_argument("--id", required=True, help="네이버 ID (블로그 ID)")
    ap.add_argument("--image", action="append", default=[], help="올릴 사진 경로 (여러 번 지정 가능)")
    ap.add_argument("--title", default="API 연결 테스트")
    ap.add_argument("--tags", default="", help="쉼표로 구분한 태그 (선택, 지원 여부 확인용)")
    ap.add_argument("--publish", action="store_true", help="공개로 올리기 (기본: 공개 안 함)")
    ap.add_argument("--endpoint", default=DEFAULT_ENDPOINT, help="테스트용 주소 변경 (모의 서버 검증용)")
    args = ap.parse_args()

    password = os.environ.get("NAVER_API_PW") or getpass.getpass("API 연결 암호: ")
    if not password:
        print("암호가 비어 있습니다.", file=sys.stderr)
        return 2

    client = make_client(args.endpoint)
    print(f"[1/3] 대상 주소: {args.endpoint}")

    image_urls = []
    for i, path in enumerate(args.image, 1):
        print(f"[2/3] 사진 업로드 {i}/{len(args.image)}: {os.path.basename(path)}")
        try:
            url = upload_image(client, args.id, password, path)
        except xmlrpc.client.Fault as e:
            print(f"  ✗ 서버 오류 {e.faultCode}: {e.faultString}")
            return 1
        except Exception as e:  # 네트워크/프로토콜 오류
            print(f"  ✗ 요청 실패: {type(e).__name__}: {e}")
            return 1
        print(f"  ✓ 업로드 성공: {url}")
        image_urls.append(url)
    if not args.image:
        print("[2/3] 사진 없음 (건너뜀)")

    print(f"[3/3] 글 등록 (publish={args.publish})")
    tags = [t.strip() for t in args.tags.split(",") if t.strip()] or None
    try:
        post_id = create_post(
            client, args.id, password, args.title,
            build_html(args.title, image_urls), args.publish, tags,
        )
    except xmlrpc.client.Fault as e:
        print(f"  ✗ 서버 오류 {e.faultCode}: {e.faultString}")
        return 1
    except Exception as e:
        print(f"  ✗ 요청 실패: {type(e).__name__}: {e}")
        return 1

    print(f"  ✓ 등록 성공. 글 번호: {post_id}")
    print()
    print("다음을 직접 확인하세요.")
    print("  - 블로그 관리 화면에서 이 글이 '공개 / 비공개 / 임시저장' 중 어디에 있는지")
    print("  - 사진이 본문에 제대로 보이는지")
    print("  - (--tags를 썼다면) 태그가 붙었는지")
    print("확인 후 테스트 글은 직접 삭제하세요.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
