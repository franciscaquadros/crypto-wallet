# Crypto-Wallet Java Application

This is a Spring Boot application built with Maven. This guide explains how to run the application from the command line.

---

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- Java 17 (check with `java -version`)
- Maven 3.6+ (check with `mvn -version`)

---

## 🚀 Building and Running the Application

The simplest way to run the application is to use Maven. Navigate to the main folder of the project and run the following command:

```bash
mvn spring-boot:run
```

After that, you can request the following endpoints using curl or Postman:

## 🌐 API Endpoints

### 1. Create Wallet
- **Endpoint**: `POST /crypto-wallet/wallet`
- **Description**: Creates a new wallet for a user based on the provided email.
- **Request Body**: 
```json 
  {
    "email": "user@example.com"
  }
```
- **Response**: 200 OK (Empty Response)

### 2. Add Asset to Wallet
- **Endpoint**: `POST /crypto-wallet/asset/{email}`
- **Description**: Adds an asset to the wallet of a user identified by their email.
- **Request Body**:
```json 
  {
      "symbol": "ETH",
      "price": 10000,
      "quantity": 5
  }
```
- **Response**: 200 OK (Empty Response)

### 3. Show Wallet
- **Endpoint**: `GET /crypto-wallet/wallet/{email}`
- **Description**: Retrieves the details of a user's wallet based on their email address.
- **Path Parameter**: `email` - The email address associated with the wallet.
- **Response Body**:
```json 
  {
  "id": "d60b876e-5eb3-4de7-9c01-7feab717ab81",
  "total": 1056442.35,
  "assets": [
    {
      "symbol": "BTC",
      "quantity": 10.0,
      "price": 104356.30,
      "value": 1043563.00
    },
    {
      "symbol": "ETH",
      "quantity": 5.0,
      "price": 2575.87,
      "value": 12879.35
    }
  ]
}
```
- **Response**: 200 OK

### 4. Evaluate Wallet
- **Endpoint**: `POST /crypto-wallet/wallet/evaluate`
- **Description**: Evaluates the value of assets based on the input and a date (optional)
- **Request Body**:
```json 
  {
  "assets" : [
    {
      "symbol": "BTC",
      "quantity": 0.5,
      "value": 35000
    },
    {
      "symbol": "ETH",
      "quantity": 4.25,
      "value": 15310.71
    }
  ],
  "date": "2025-01-07"
}
```
- **Response Body**:
```json 
  {
  "total": 65914.47,
  "best_asset": "BTC",
  "best_performance": 44.00,
  "worst_asset": "ETH",
  "worst_performance": 1.00
}
```
- **Response**: 200 OK

## 📝 Considerations

- The application uses an in-memory database `(H2)` for simplicity. In a production environment, we should consider using a persistent database.
- The application is built with Spring Boot
- Swagger could be used to better document the API, but due to time constraints, it was not implemented.
- The application is designed to be simple and straightforward, focusing on the core functionality of managing a crypto wallet.
- Ideally the JPA relationships should use `Fetch type Lazy` to avoid loading all the data at once, and then combined with `CriteriaApi/Specifications`
or similar, but for simplicity, `Eager fetching` is used in this implementation.
- Some tests are included, but not all possible scenarios are covered. This is due to time constraints.
- In `CoinCapFeignConfig` we use an interceptor to include the `apiKey` in the header of the request to CoinCap API
- In `TokenFetchConfig` we use the properties `app.token-fetch.interval-seconds`and `app.token-fetch.max-threads-number` to configure the frequency 
of the tokens price update and the maximum number of threads to use for the update. This values can be edited in the `application.properties` file.
- In the current configuration, we use an `ExecutorService` with 3 threads to fetch and update token prices concurrently. This can be adjusted.
- Read-Write Lock: The use of a `ReentrantReadWriteLock` ensures that read operations can happen concurrently, while write operations (updating prices) are serialized.
- When fetching price history from CoinCap API, the interval h12 was chosen after some investigation, as it
provides a good balance between granularity and efficiency, which is helpful for those analyzing cryptocurrency assets over time but not necessarily at the minute level.