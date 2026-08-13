# OOP·SRP 기반 구현 Skill

## 작성 정보

- 작성일: 2026-08-13
- Skill 이름: `apply-oop-srp`
- 기능: SEBU 백엔드의 기능 구현과 리팩터링에 객체지향 설계와 단일 책임 원칙을 적용한다.
- 파일명 규칙: 이후 Skill의 상세 기록도 `YYYY-MM-DD-기능명.md` 형식을 사용한다.

## SEBU 백엔드 아키텍처

새로운 동작을 기존 구조에 맞게 배치할 때 이 문서를 참고한다. 구현 전에 항상 현재 코드와 일치하는지 확인한다.

### 기술 스택과 검증

- Java 21
- Spring Boot 3.5.3
- Spring Web, Bean Validation, Spring Data JPA, Flyway
- 로컬 개발과 테스트에서는 H2 사용
- 운영 환경에서는 MySQL 사용
- Gradle Wrapper 검증 명령은 `./gradlew test` 또는 `.\gradlew.bat test`

### 패키지별 책임

#### `api`

HTTP 관련 처리와 외부 표현 형식을 담당한다.

- Controller는 요청을 받고 유스케이스에 처리를 위임한다.
- 요청 검증과 응답 변환은 이 경계에 유지한다.
- `ApiResponse`와 기능별 Response 타입에 비즈니스 판단을 넣지 않는다.
- Controller에서 Repository를 직접 조회하거나 도메인 규칙을 구현하지 않는다.

현재 사례에서 `LaboratoryController`는 `LaboratoryQueryService`에 처리를 위임하고 Application 결과를 `LaboratoriesResponse`로 변환한다.

#### `application`

유스케이스 조정과 트랜잭션 경계를 담당한다.

- Repository, 현재 사용자 정보, 도메인 객체 간의 협력을 조정한다.
- API Response 타입 대신 Application Result 타입을 반환한다.
- 완전한 유스케이스를 하나의 트랜잭션 경계 안에 유지한다.
- 하나의 도메인 객체가 소유해야 할 불변 조건이나 상태 전이를 흡수하지 않는다.

현재 사례는 다음과 같다.

- `LaboratoryQueryService`는 조회 Projection을 조정하고 `LaboratoriesResult`를 구성한다.
- `LaboratoryManagementService`는 연구실, 교수, 학과, 사용자, 즐겨찾기와 관련된 쓰기 유스케이스를 조정한다.

Service의 의존성이 많다는 이유만으로 Service를 분리하지 않는다. 유스케이스의 변경 주체나 의존성 경계가 서로 다르고 요청한 변경에 분리의 실질적인 이점이 있을 때만 나눈다.

#### `domain`

도메인 상태, 불변 조건, 상태 전이, Repository 계약, Aggregate별 조회 Projection을 담당한다.

- JPA Entity는 접근이 제한된 기본 생성자를 사용하고 상태를 캡슐화한다.
- Entity가 일관되게 검증할 충분한 정보를 가지고 있다면 해당 규칙을 Entity에 둔다.
- Aggregate 간 조회와 조정은 Application 계층에 둔다.
- 영속성 조회는 Controller나 Service에 직접 작성하지 않고 관련 Repository에 둔다.

현재 사례에서 `Laboratory.softDelete()`는 연구실 삭제 상태 전이를 담당한다. `LaboratoryManagementService`는 활성 상태인 연구실을 찾고 트랜잭션 경계를 설정한다.

#### Cross-cutting 패키지

- `auth`는 현재 사용자 정보 경계를 담당한다.
- `ratelimit`는 Rate Limit 정책, Key 결정, Interceptor, Configuration, 구현체를 담당한다.
- 기능별 도메인 동작을 이 패키지에 배치하지 않는다.

### 책임 배치 판단 순서

다음 질문을 순서대로 적용한다.

1. HTTP 또는 직렬화에 관한 책임인가? `api`에 둔다.
2. 하나의 완전한 유스케이스나 여러 의존성을 조정하는 책임인가? `application`에 둔다.
3. 하나의 도메인 개념이 소유하는 불변 조건이나 상태 전이인가? 해당 Domain 객체에 둔다.
4. 영속성 조회나 데이터베이스 접근에 관한 책임인가? 관련 Repository 또는 Projection 뒤에 둔다.
5. 인증 또는 Rate Limit에 관한 책임인가? 해당 Cross-cutting 패키지에 유지한다.

어느 경계에도 명확히 해당하지 않으면 가장 가까운 기존 기능을 확인하고, 새로운 아키텍처 개념을 도입하기 전에 기존 경계를 따른다.

### SRP 검토 질문

변경을 마무리하기 전에 다음 질문에 답한다.

- 각 변경 타입을 수정하게 만드는 사용자나 요구사항은 무엇인가?
- 한 타입이 HTTP 변환, 유스케이스 조정, 도메인 정책, 영속성 세부 사항을 섞고 있지 않은가?
- HTTP 요청 없이 비즈니스 규칙을 테스트할 수 있는가?
- 새 추상화가 판단, 정책, 경계를 소유하는가, 아니면 단순히 호출만 전달하는가?
- 추상화를 제거했을 때 중복을 늘리지 않으면서 책임이 더 명확해지는가?
- 하나의 완전한 트랜잭션과 응집도 높은 도메인 동작을 유지하는가?

클래스 수를 줄이거나 늘리는 것보다 응집도와 명확한 책임 소유를 우선한다.
