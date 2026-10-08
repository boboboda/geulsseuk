"""저장한 로그인 상태로 네이버 글쓰기 화면이 열리는지 확인합니다. (쿠키 내용은 출력하지 않습니다.)

로그인 상태는 환경변수 NAVER_STATE_B64 (GitHub 시크릿) 또는 naver_state.b64 파일에서 읽습니다.
사용: python tools/login_test/check_login.py --id 네이버아이디 [--headed]
"""
import argparse
import base64
import gzip
import json
import os
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

SHOT = Path("login_check.png")


def load_state() -> dict:
    packed = os.environ.get("NAVER_STATE_B64", "").strip()
    if not packed and Path("naver_state.b64").exists():
        packed = Path("naver_state.b64").read_text().strip()
    if not packed:
        sys.exit("로그인 상태가 없어요. save_login.py 를 먼저 실행하거나 NAVER_STATE_B64 를 설정하세요.")
    return json.loads(gzip.decompress(base64.b64decode(packed)))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--id", required=True, help="네이버 ID")
    ap.add_argument("--headed", action="store_true", help="창을 띄워서 확인 (내 컴퓨터용)")
    ap.add_argument("--chrome", action="store_true", help="설치된 크롬으로 확인 (save_login 과 같은 브라우저)")
    args = ap.parse_args()

    state = load_state()
    url = f"https://blog.naver.com/{args.id}?Redirect=Write"
    with sync_playwright() as p:
        if args.chrome:
            browser = p.chromium.launch(channel="chrome", headless=not args.headed)
        else:
            browser = p.chromium.launch(headless=not args.headed)
        ctx = browser.new_context(
            storage_state=state,
            locale="ko-KR",
            timezone_id="Asia/Seoul",
            viewport={"width": 1280, "height": 900},
        )
        page = ctx.new_page()
        page.goto(url, wait_until="domcontentloaded", timeout=60000)
        page.wait_for_timeout(6000)  # 에디터가 뜰 시간
        final_url = page.url
        title = page.title()
        page.screenshot(path=str(SHOT), full_page=False)
        browser.close()

    print(f"최종 주소: {final_url}")
    print(f"페이지 제목: {title}")
    low = final_url.lower()
    if "nid.naver.com" in low or "login" in low:
        print("결과: ✗ 로그인 화면으로 돌아갔어요 (로그인 상태가 통하지 않음)")
        return 2
    if "captcha" in low or "protect" in low or "verify" in low:
        print("결과: ✗ 추가 인증/캡차 화면이 떴어요")
        return 3
    print(f"결과: 로그인 화면은 아니에요. 스크린샷({SHOT})으로 글쓰기 화면인지 직접 확인하세요.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
