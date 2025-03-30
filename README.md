# HWP & DOC 변환기

[English Version(영어 버전)](README.en.md)

HWP(한글 워드 프로세서) 및 DOC/DOCX(마이크로소프트 워드) 파일을 다양한 형식으로 변환하는 웹 기반 애플리케이션입니다.

## 주요 기능

- HWP 파일을 TXT 및 PDF로 변환
- DOC/DOCX 파일을 다양한 형식으로 변환
- 사용자 인증 및 권한 부여
- Redis 큐를 사용한 비동기 처리
- 작업 상태 추적 및 알림
- Bootstrap 5를 사용한 반응형 웹 인터페이스

## 사용 기술

- Java 21
- Spring Boot 3.4.4
- Spring Security 6
- Spring Data JPA & Redis
- Thymeleaf
- MySQL/MSSQL (데이터베이스)
- Redis (캐싱 및 작업 큐)
- hwplib (HWP 파일 처리)
- Bootstrap 5 (프론트엔드)

## 필수 요구사항

- Java 21 JDK
- MySQL 또는 MS SQL Server
- Redis 서버
- Maven 또는 Gradle

## 설치 및 설정

1. 저장소 복제:
   ```bash
   git clone https://github.com/yuseok-kim-edushare/hwpconvert.git
   cd hwpconvert
   ```

2. 데이터베이스 및 Redis 구성:
   `src/main/resources/application.yaml` 파일을 수정하여 데이터베이스 및 Redis 연결 설정을 업데이트하세요.

3. 애플리케이션 빌드:
   ```bash
   ./gradlew build
   ```

4. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun
   ```
   
   애플리케이션은 `http://localhost:8080`에서 접속할 수 있습니다.

## 기본 관리자 계정

시스템은 첫 시작 시 기본 관리자 계정을 생성합니다:
- 사용자 이름: admin
- 비밀번호: admin

운영 환경에서는 반드시 이 비밀번호를 변경하세요.

## 프로젝트 구조

- `config/` - 구성 클래스
- `controller/` - 웹 컨트롤러
- `model/` - 데이터 모델
- `repository/` - 데이터 액세스 인터페이스
- `service/` - 비즈니스 로직
- `util/` - 유틸리티 클래스
- `resources/templates/` - Thymeleaf 템플릿
- `resources/static/` - 정적 리소스(CSS, JS, 이미지)

## 라이선스

이 프로젝트는 MIT 라이선스에 따라 라이선스가 부여됩니다. 자세한 내용은 LICENSE 파일을 참조하세요.

## 감사의 말

- HWP 파일 처리를 위한 [hwplib](https://github.com/neolord0/hwplib)
  - 또한 hwp(한글 워드 프로세서)를 개발한 [한컴](https://www.hancom.com)에 감사드립니다.
    본 프로젝트는 한컴의 공개 참조 문서인 hwp/x 형식을 기반으로 한 hwplib에 의존하고 있습니다.
- 프레임워크를 위한 [Spring Boot](https://spring.io/projects/spring-boot)
- UI 컴포넌트를 위한 [Bootstrap](https://getbootstrap.com/) 