# 공책 RPG

간단한 턴제 던전 크롤러 RPG입니다.

## 다운로드 & 실행 방법

### Windows (Java 설치 불필요)

1. `NotebookRPG-windows.zip` 다운로드
2. 압축 풀기 (우클릭 → 압축 풀기)
3. 풀린 `NotebookRPG` 폴더로 들어가서 `NotebookRPG.exe` 더블클릭

> 처음 실행 시 "Windows에서 PC를 보호했습니다" 경고가 뜰 수 있습니다. 서명되지 않은 개인 제작 프로그램이라 뜨는 정상적인 경고이니, **추가 정보 → 실행**을 누르면 됩니다.
>
> `NotebookRPG.exe`만 따로 빼서 옮기면 실행이 안 됩니다 — `app`, `runtime` 폴더가 같은 위치에 있어야 합니다. 폴더 전체를 그대로 두고 실행하세요.

### Mac / Windows 공용 (Java 필요)

1. Java 11 이상이 설치되어 있어야 합니다. 없다면 [Adoptium](https://adoptium.net) 에서 설치하세요.
2. `NotebookRPG-cross-platform.tar.gz` 다운로드
3. 압축 풀기
   - **Mac**: 더블클릭하면 자동으로 풀립니다.
   - **Windows**: 우클릭 → 압축 풀기 (Windows 11 이상은 기본 지원, 이하 버전은 7-Zip 등 필요)
4. 풀린 폴더에서 실행
   - **Windows**: `run-windows.bat` 더블클릭
   - **Mac**: `run-mac.command` 더블클릭

> Mac에서 `run-mac.command`가 "권한이 없습니다" 또는 실행이 안 되면, 터미널에서 압축 푼 폴더로 이동한 뒤 아래 명령을 한 번 실행하세요.
> ```
> chmod +x run-mac.command
> ```

## 저장 데이터

게임 데이터는 실행 위치와 상관없이 아래 경로에 자동 저장됩니다.

- Windows: `%APPDATA%\NotebookRPG\save.txt`
- Mac / Linux: `~/.notebookrpg/save.txt`

## 조작법

- 마우스 클릭 또는 방향키(↑↓←→) + Enter로 메뉴/버튼을 선택할 수 있습니다.
