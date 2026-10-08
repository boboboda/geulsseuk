# 글쓱 (geulsseuk)

사진 몇 장과 짧은 메모를 넣으면 AI가 네이버 블로그 글 초안을 써 주는 안드로이드 앱입니다.
서버 없이 휴대폰에서만 동작하고, API 키는 사용자가 앱 설정에 직접 넣습니다. 올리기 전에 사람이 읽고 고치는 **반자동** 방식입니다.

## 흐름
사진 + 메모 → AI(Gemini / Claude / OpenRouter / Groq / OpenAI / 직접 입력) → 글 초안(제목·문단·사진 위치·태그) → 앱에서 수정 → 네이버 블로그 글쓰기 API로 업로드 (또는 전체 복사)

## 구조
- `core/` 순수 Kotlin(JVM). AI 호출, 응답 파싱, 네이버 XML-RPC, HTML 생성. 가짜 전송기로 단위 테스트 가능.
- `app/` Jetpack Compose 화면, OkHttp 전송, 사진 줄이기, Keystore 암호화 저장.
- `tools/api_test/` 네이버 글쓰기 API가 **지금도 되는지** 먼저 확인하는 파이썬 도구와 모의 서버.

## 시작 순서
1. **네이버 API 확인(가장 먼저)**: 블로그 관리 > 글쓰기 API 연결에서 'API 연결 암호'를 만든 뒤
   `NAVER_API_PW=암호 python tools/api_test/naver_api_test.py --id 내아이디 --image 사진.jpg`
   기본은 공개하지 않는 글로 올립니다. 올라간 글이 비공개인지 임시저장인지 네이버에서 확인하세요. 안 되면 앱의 '전체 복사'만으로 쓰게 되니 계획을 바꿔야 합니다.
2. Android Studio로 열기 (JDK 17). 처음 한 번 터미널에서 `gradle wrapper` 로 `gradlew`를 생성합니다.
3. 실행 후 설정에서 AI 선택, API 키, 네이버 ID, API 연결 암호를 입력 → '연결 확인'.
4. GitHub Actions(`.github/workflows/ci.yml`)가 core 테스트와 디버그 APK 빌드를 자동으로 돌립니다.

## 배포용 서명
저장소 루트에 `keystore.properties`(git에 올라가지 않음)를 만듭니다.
```
storeFile=geulsseuk.jks
storePassword=...
keyAlias=...
keyPassword=...
```
서명 키 파일(.jks)은 잃어버리면 업데이트를 못 하니 따로 백업하세요.

## 보안·개인정보 원칙
- API 키와 네이버 API 연결 암호는 Android Keystore(AES-GCM)로 암호화해 기기에만 저장하고, 백업(allowBackup)은 끕니다.
- 사진·메모는 사용자가 고른 AI 서비스로만 전송됩니다. 일부 무료 한도는 입력이 모델 개선에 쓰일 수 있어 첫 실행 때 안내합니다.
- 하루 AI 호출 30회 제한으로 실수로 인한 과다 사용을 막습니다.

## 아직 확인되지 않은 점
- 네이버 글쓰기 API(XML-RPC)의 현재 동작 여부, `publish=false`의 실제 의미(비공개/임시저장). 이미 올린 글의 수정은 지원되지 않습니다.
- 이 저장소의 Kotlin 코드는 작성 환경에서 컴파일하지 못했습니다. 첫 CI 실행 결과로 오류를 잡아야 합니다.
- `applicationId`(com.buyoungsilcoding.geulsseuk)는 스토어 출시 후 바꿀 수 없으니 배포 전에 확정하세요.
- 기본 모델 이름은 바뀔 수 있습니다. 설정에서 직접 고칠 수 있습니다.
