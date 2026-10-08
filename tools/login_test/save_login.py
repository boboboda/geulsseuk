"""내 컴퓨터에서 딱 한 번 실행: 크롬 창이 뜨면 네이버에 직접 로그인하세요.
로그인 상태를 naver_state.b64 파일로 저장합니다. (비밀번호는 저장하지 않지만, 이 파일은 계정 열쇠와 같으니
절대 커밋·공유하지 마세요. .gitignore 에 이미 제외돼 있습니다.)

사용: python tools/login_test/save_login.py
"""
import base64
import gzip
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

OUT = Path("naver_state.b64")


def main() -> int:
    with sync_playwright() as p:
        try:
            browser = p.chromium.launch(channel="chrome", headless=False)  # 설치된 진짜 크롬
        except Exception:
            browser = p.chromium.launch(headless=False)
        ctx = browser.new_context()
        page = ctx.new_page()
        page.goto("https://nid.naver.com/nidlogin.login")
        print("열린 창에서 네이버에 로그인하세요. (2단계 인증이 있으면 끝까지 진행)")
        input("로그인이 끝나 블로그가 보이면 여기서 Enter 를 누르세요: ")
        page.goto("https://blog.naver.com/")
        page.wait_for_timeout(1500)
        state = ctx.storage_state()
        browser.close()

    import json

    raw = json.dumps(state, ensure_ascii=False).encode("utf-8")
    packed = base64.b64encode(gzip.compress(raw)).decode("ascii")
    OUT.write_text(packed)
    print(f"저장했어요: {OUT} ({len(packed):,}자)")
    if len(packed) > 45000:
        print("주의: GitHub 시크릿 한도(48KB)에 가까워요. 알려 주세요.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
