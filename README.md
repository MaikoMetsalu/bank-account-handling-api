# Bank account handling api
### Important notes
- This project is a sample implementation of a banking API application.
- It is not intended for production use.
- Security is non-existent. Some features are simplified or omitted due to time restrictions.
- All currency exchange rates are mocked and do not reflect real-world values.

#### Microservice for handling multi-currency bank accounts, transactions, and currency exchange.

### Built with
* Java 25
* Spring Boot 4
* PostgreSQL
* Flyway

### Prerequisites: Docker & Docker Compose
- Install Docker: https://docs.docker.com/get-docker/
- Install Docker Compose: https://docs.docker.com/compose/install/
- Ensure Docker is running: `docker --version` and `docker-compose --version`

### Getting Started
#### Running the Application
1. Clone the repository
2. Navigate to the project directory
3. Run the following command to start the application:
   ```bash
   docker-compose up --build
   ```
4. The application will be accessible at `http://localhost:8080`

#### Accessing the Application
API Documentation (Swagger UI): `http://localhost:8080/swagger-ui.html`

#### Running tests
* Prerequisite: Docker must be running.

To run tests, use the following command:
```bash
./gradlew test
```

#### Example flow
1. Create a new bank account (POST `/accounts`)
2. Deposit funds into the account (POST `/{accountId}/balance/deposit`)
3. Withdraw funds from the account (POST `/{accountId}/balance/withdraw`)
4. Transfer funds between accounts (POST `/balance/transfer`)
5. Check account balance (GET `/{accountId}/balance`)