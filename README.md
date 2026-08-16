# SEBU Backend

학부 연구실 검색 서비스의 백엔드 애플리케이션입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.3
- Gradle 8.14.3 (Wrapper)
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 (로컬 개발)
- MySQL 8.0.19 이상 (배포)

## 실행

Windows에서는 저장소 루트에서 다음 명령을 실행합니다.

```powershell
.\gradlew.bat bootRun
```

기본 프로필은 `local`이며 H2 인메모리 DB를 사용합니다.

- H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:sebu`
- Username: `sa`
- Password: 없음

## 테스트

```powershell
.\gradlew.bat test
```

## 교수 정보 크롤링

교수 정보 크롤러는 일반 서비스 실행과 분리된 일회성 작업입니다. MySQL 연결, 단일 출처 시험 실행, 전체 출처 실행 및 결과 확인 방법은 [교수 정보 1차 크롤링 실행 안내](docs/professor-crawling.md)를 참고합니다.

## 연구실 삭제 보존 정책

- 연구실 삭제 시 `deleted_at`을 기록하여 30일간 복구할 수 있는 상태로 보존합니다.
- 매일 새벽 3시에 30일이 지난 연구실을 물리 삭제합니다.
- 물리 삭제 시 연구 분야 매핑과 북마크는 DB `ON DELETE CASCADE`에 따라 함께 삭제됩니다.
- 보존 기간과 실행 시간은 `app.laboratory-retention` 설정으로 변경할 수 있습니다.

## 배포 프로필

`prod` 프로필은 MySQL 접속 정보를 환경 변수로 받습니다.

```text
DB_URL=jdbc:mysql://host:3306/database
DB_USERNAME=...
DB_PASSWORD=...
SPRING_PROFILES_ACTIVE=prod
```
