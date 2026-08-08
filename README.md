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
- MySQL (배포)

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

## 배포 프로필

`prod` 프로필은 MySQL 접속 정보를 환경 변수로 받습니다.

```text
DB_URL=jdbc:mysql://host:3306/database
DB_USERNAME=...
DB_PASSWORD=...
SPRING_PROFILES_ACTIVE=prod
```
