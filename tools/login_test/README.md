# 네이버 로그인 시험 (GitHub Actions에서 로그인 상태가 통하는지)

1. 내 컴퓨터: `pip install playwright && playwright install chromium`
2. `python tools/login_test/save_login.py` → 뜨는 크롬에서 직접 로그인 → Enter → `naver_state.b64` 생성
3. (선택) 내 컴퓨터에서 먼저 확인: `python tools/login_test/check_login.py --id 아이디 --headed`
4. GitHub 저장소(비공개!) Settings → Secrets and variables → Actions → New repository secret
   이름 `NAVER_STATE_B64`, 값은 `naver_state.b64` 파일 내용 (Mac: `pbcopy < naver_state.b64`)
5. Actions 탭 → "Naver login test" → Run workflow → 네이버 ID 입력
6. 끝나면 Artifacts 의 스크린샷에서 글쓰기 화면이 보이는지 확인 (로그인/인증 화면이면 실패)
7. 시험이 끝나면 시크릿과 로컬 `naver_state.b64`를 지우세요.
