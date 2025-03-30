# HWP & DOC Converter

A web-based application for converting HWP (Hangul Word Processor) and DOC/DOCX (Microsoft Word) files to various formats.

## Features

- Convert HWP files to TXT and PDF
- Convert DOC/DOCX files to various formats
- User authentication and authorization
- Asynchronous processing with Redis queue
- Task status tracking and notifications
- Responsive web interface using Bootstrap 5

## Technologies Used

- Java 21
- Spring Boot 3.4.4
- Spring Security 6
- Spring Data JPA & Redis
- Thymeleaf
- MySQL/MSSQL for database
- Redis for caching and task queue
- hwplib for HWP file handling
- Bootstrap 5 for front-end

## Prerequisites

- Java 21 JDK
- MySQL or MS SQL Server
- Redis server
- Maven or Gradle

## Setup and Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/hwpconvert.git
   cd hwpconvert
   ```

2. Configure database and Redis:
   Edit `src/main/resources/application.yaml` and update the database and Redis connection settings.

3. Build the application:
   ```bash
   ./gradlew build
   ```

4. Run the application:
   ```bash
   ./gradlew bootRun
   ```
   
   The application will be available at `http://localhost:8080`

## Default Admin Account

The system creates a default admin account on first startup:
- Username: admin
- Password: admin

Make sure to change this password in production environments.

## Project Structure

- `config/` - Configuration classes
- `controller/` - Web controllers
- `model/` - Data models
- `repository/` - Data access interfaces
- `service/` - Business logic
- `util/` - Utility classes
- `resources/templates/` - Thymeleaf templates
- `resources/static/` - Static resources (CSS, JS, images)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [hwplib](https://github.com/neolord0/hwplib) for HWP file processing
- [Spring Boot](https://spring.io/projects/spring-boot) for the framework
- [Bootstrap](https://getbootstrap.com/) for the UI components 