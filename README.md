# **Profissu - Connecting Clients and Professionals**

[![Profissu Logo](src/main/resources/static/images/profissu.jpeg)](src/main/resources/static/images/profissu.jpeg)

**Profissu** is an innovative platform designed to efficiently and transparently connect clients with professionals from various fields. We simplify the search for qualified specialists, enabling clients to find the ideal professional for their needs while allowing professionals to showcase their services and reach new clients.

## **Key Features**

* **Simple Registration:** Clients and professionals can quickly and easily sign up, creating complete profiles with relevant information about their skills, experience, and services.
* **Detailed Profiles:** Professional profiles display comprehensive details about their qualifications, past projects, client testimonials, and ratings, helping clients make informed decisions.
* **Direct Communication:** The platform facilitates direct communication between clients and professionals, allowing them to discuss projects, request quotes, and schedule services efficiently.
* **Ratings and Testimonials:** A transparent rating and review system enables clients and professionals to evaluate their experiences, fostering trust and reputation within the community.
* **Secure Authentication:** We implement a robust authentication system using JWT (JSON Web Tokens) to ensure data security and user privacy.

## **Technologies Used**

Profissu is built with modern and robust technologies, ensuring high performance, scalability, and maintainability:

* **Backend:**
    * **Spring Boot:** A powerful Java framework for building microservices and web applications, offering features like dependency injection, auto-configuration, and embedded servers.
    * **Spring REST:** Simplifies the creation of RESTful APIs, enabling efficient communication between the frontend and backend.
    * **Spring Security:** Ensures application security with comprehensive authentication, authorization, and protection against common vulnerabilities.
    * **Spring Data JPA:** Abstracts database access, simplifying persistence operations and reducing boilerplate code.

* **Database:**
    * **MySQL:** A reliable and widely used relational database for persistent data storage.
    * **Liquibase:** A database migration management tool that provides flexibility and control over schema changes.

* **Infrastructure:**
    * **Docker:** A containerization platform that packages the application and its dependencies into isolated containers, ensuring deployment consistency across different environments.
    * **Docker Compose:** A tool for defining and running multi-container Docker applications, simplifying the management of Profissu and its dependencies.

* **API Documentation:**
    * **OpenAPI/Swagger:** Provides interactive API documentation, making it easier to understand and integrate with the Profissu platform.

* **Testing:**
    * **JUnit:** A widely used testing framework for writing and running unit tests.
    * **Mockito:** A mocking framework for creating mock objects in tests.

* **Logging and Monitoring:**
    * **Logback:** A flexible and configurable logging framework for capturing application events and errors.
    * **Spring Boot Actuator:** Provides monitoring and management features for production applications, including health checks, metrics, and logging.

## **System Architecture**

Profissu follows a layered architecture, promoting maintainability, scalability, and testability:

* **Presentation Layer (Controllers):** Handles HTTP requests, validates inputs, and interacts with the Application Layer.
* **Application Layer (Services):** Contains business logic, orchestrating interactions between components.
* **Persistence Layer (Repositories):** Abstracts database access, simplifying data persistence operations.
* **Domain Layer (Domain Model):** Defines core entities and their relationships, representing the application's business concepts.

## **Development Practices**

* **Agile Development:** We follow agile methodologies, prioritizing iterative development, frequent feedback, and continuous improvement.
* **Continuous Integration/Continuous Deployment (CI/CD):** We aim to implement CI/CD pipelines to automate the build, test, and deployment processes.

## **How to Run the Application**

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/VitorSantosCruz/profissu.git
   ```

2. **Navigate to the Project Directory:**
   ```bash
   cd profissu
   ```

3. **Start the Application with Docker Compose:**
   ```bash
   docker compose up -d
   ```
