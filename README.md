# Configuring MongoDB TLS in Spring Boot Application

This guide explains how to configure TLS (SSL) for MongoDB connections in a Spring Boot application and run a MongoDB instance with TLS enabled in Docker.

## 1. Spring Boot Application Configuration

### 1.1 Dependencies

Ensure you have the following dependencies in your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <!-- Other dependencies -->
</dependencies>
```

### 1.2 Application Properties

In your `application.properties` or `application.yml` file, add:

```properties
spring.data.mongodb.uri=mongodb://localhost:27018/your_database
spring.data.mongodb.database=your_database
```

### 1.3 Certificate Files

Refer [this link](https://github.com/mohammadkhizerkhan/mongo_tls/blob/main/certificateGeneration.md) to generate certificates and jks keys.

Place your certificate files in the `src/main/resources/ssl` directory:

- `client-cert.jks`: Client certificate keystore
- `truststore.jks`: Truststore containing the CA certificate

### 1.4 Java Configuration

Create a configuration class (e.g., `MongoConfig.java`) or use your main application class:

```java
@SpringBootApplication
public class DemoApplication {

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public MongoClient mongoClient() throws Exception {
        // Load client key store
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream keyStoreStream = new ClassPathResource("ssl/client-cert.jks").getInputStream()) {
            keyStore.load(keyStoreStream, "mypassword".toCharArray());
        }

        // Initialize KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "mypassword".toCharArray());

        // Load trust store
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream trustStoreStream = new ClassPathResource("ssl/truststore.jks").getInputStream()) {
            trustStore.load(trustStoreStream, "mypassword".toCharArray());
        }

        // Initialize TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        // Configure MongoDB client settings
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .applyToSslSettings(builder -> builder.context(sslContext))
                .build();

        // Create MongoClient instance
        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, databaseName);
    }
}
```

This configuration:
- Loads the client certificate and truststore
- Initializes SSL context with the loaded certificates
- Configures MongoDB client settings to use SSL

## 2. Running MongoDB with TLS in Docker

Refer [this repo](https://github.com/mohammadkhizerkhan/mongo_tls) to run an instance of mongodb using docker or see the below details write it manually.

### 2.1 Docker Compose File

Create a `docker-compose.yml` file:

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:latest
    container_name: mongodb_tls
    command: ["mongod", "--tlsMode", "requireTLS", "--tlsCertificateKeyFile", "/etc/ssl/server.pem", "--tlsCAFile", "/etc/ssl/ca.crt"]
    ports:
      - "27018:27017"
    volumes:
      - mongodb_data:/data/db
      - ./ssl:/etc/ssl:ro

volumes:
  mongodb_data:
    driver: local
```

### 2.2 SSL Certificates

Ensure you have the following files in your `./ssl` directory:
- `ca.crt`: CA certificate
- `server.pem`: Combined server certificate and private key

### 2.3 Start MongoDB Container

Run the following command to start the MongoDB container:

```bash
docker-compose up -d
```

## 3. Connecting to MongoDB

With the Spring Boot application configured and MongoDB running in Docker, your application should now be able to connect to MongoDB using TLS.

To test the connection, you can create a simple REST controller or use the MongoDB shell with TLS enabled.

### 3.1 Using MongoDB Shell with TLS

To connect to the MongoDB instance using the mongo shell with TLS:

```bash
mongo --tls --tlsCertificateKeyFile ./ssl/client.pem --tlsCAFile ./ssl/ca.crt --host localhost --port 27018
```

## Conclusion

You have now configured your Spring Boot application to connect to MongoDB using TLS and set up a MongoDB instance with TLS enabled in Docker. This setup ensures secure communication between your application and the database.

Remember to keep your certificates and keystores secure and update them before they expire.
