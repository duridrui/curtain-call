<div align="center">

# 🎭 커튼콜 (Curtain Call)

공부하다가 딴짓 화면에 머무르면 커튼이 서서히 닫히는 macOS용 집중 프로그램

![macOS](https://img.shields.io/badge/macOS-only-black?logo=apple)
![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.5-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?logo=gradle)

</div>
<br>


## 🔎 개요 (Overview)
* 프로그램명: **커튼콜 (Curtain Call)**
* 목적: 공부하다가 딴짓 화면에 머무르면 커튼이 서서히 닫혀 딴짓을 알아차리게 함
* 동작 환경: macOS 전용 (Windows, Linux에서는 동작하지 않음)
<br>


## 🔥 주요 기능 (Key Features)
- **예매**: 공연 시간을 정하고 티켓을 찢어 공연을 시작합니다.
- **허용 목록**: 공연마다 허용할 앱과 사이트를 고릅니다.
- **커튼 닫힘**: 딴짓 화면에 머무르면 커튼이 30초 동안 서서히 닫힙니다.
- **커튼콜 비교**: 공연이 끝나면 직전 공연과 비교한 결과를 보여 줍니다.
- **공연 이어보기**: 커튼이 닫혀 끝난 공연은 남은 시간만큼 이어서 볼 수 있습니다.
- **화질 팩**: 커튼 화질은 저화질이 기본으로 들어 있고, 중화질과 고화질은 선택할 때 인터넷에서 내려받습니다.
- **방해금지 (선택)**: 단축어 앱에 단축어를 만들어 두면 공연 중에 방해금지를 켜고 끕니다.
<br>


## 🛠 실행 환경 (Requirements)

| 분류 | 내용 |
|------|------|
| 운영체제 | macOS 전용 |
| JDK | 21 이상 |
| 주요 라이브러리 | JavaFX 21.0.5, JNA 5.14.0 |
| 빌드 도구 | Gradle 9.7.1 (Gradle Wrapper 포함) |
| 네트워크 | 처음 실행할 때 Gradle과 라이브러리를 내려받으므로 인터넷 연결 필요 |

앞에 떠 있는 앱과 브라우저 주소를 macOS의 osascript로 읽고, 커튼 창 설정도 macOS 기능을 써서 Windows와 Linux에서는 동작하지 않습니다.
<br>


## ⚙️ 실행 방법 (Getting Started)

1. 압축을 풀고 터미널에서 압축을 푼 폴더로 이동합니다.
2. 아래 명령을 실행합니다.

```bash
./gradlew run
```

3. 극장 창(로비)이 뜨면 예매에서 공연 시간과 허용할 앱·사이트를 고르고 티켓을 찢어 공연을 시작합니다.

권한 오류(permission denied)로 실행되지 않으면 먼저 아래 명령을 실행한 뒤 다시 실행해 주세요.

```bash
chmod +x gradlew
```

테스트만 돌려 보려면 아래 명령을 실행하면 됩니다.

```bash
./gradlew test
```
<br>


## 🔐 처음 실행할 때 macOS 권한 (Permissions)
<details>
<summary>🔽 권한 안내 펼치기</summary>
<br>

- 처음 예매할 때 "터미널이(가) System Events을(를) 제어하려고 합니다" 창이 뜨면 **허용**을 눌러 주세요. 실행 중인 앱 목록과 앞에 떠 있는 앱 이름을 읽는 데 씁니다.
- Chrome이나 Safari를 쓰면 공연 중에 해당 브라우저를 제어하는 권한도 한 번 묻습니다. 허용해야 사이트 단위로 딴짓을 판정합니다.
- 실수로 거절했다면 **시스템 설정 > 개인정보 보호 및 보안 > 자동화**에서 터미널 아래 항목을 켜 주세요.
- 방해금지 자동 전환은 선택 기능입니다. 단축어 앱에 `커튼콜 방해금지 켜기`, `커튼콜 방해금지 끄기` 단축어를 만들어 두었을 때만 동작하고, 없으면 이 기능만 건너뜁니다.

</details>
<br>


## 📎 참고 (Notes)
- 관람 기록과 허용 목록은 홈 폴더의 `.curtain-call` 폴더에 저장됩니다.
- 실제 동작은 함께 제출한 영상을 참고해 주세요.
