package com.example.wingtiptoysdata;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
public class WingtiptoysdataApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WingtiptoysdataApplication.class);

    @Autowired
    private UserRepository repository;
    
    @GetMapping("get")
    public String get() {
      return connectionString;
    }

    @Value("${azure.cosmosdb.uri}")
    private String uri;

    @Value("${azure.cosmosdb.key}")
    private String key;

    @Value("${azure.cosmosdb.database}")
    private String dbName;
    
    @Value("${cosmosdbconfig}")
    private String connectionString;

    public static void main(String[] args) {
        SpringApplication.run(WingtiptoysdataApplication.class, args);
    }
    
    @Bean
    ApplicationRunner applicationRunner(@Value("${cosmosdbconfig}") String connectionString) {
        return args -> {
        	System.out.println(String.format("\nConnection String stored in Azure Key Vault:\n%s\n",connectionString));
        	
        	String[] arrOfStr = null;
        	if (!connectionString.isEmpty()) {
        		arrOfStr = connectionString.split(";");
        		dbName = arrOfStr[0];
            	uri = "https://" + dbName + ".vault.azure.net";
            	key = arrOfStr[1];
        	}
        };
    }

    public void run(String... var1) throws Exception {
    	
        final User testUser = new User("1", "Tasha", "Calderon", "4567 Main St Buffalo, NY 98052");

        LOGGER.info("Saving user: {}", testUser);

        // Save the User class to Azure CosmosDB database.
        final Mono<User> saveUserMono = repository.save(testUser);

        final Flux<User> firstNameUserFlux = repository.findByFirstName("testFirstName");

        //  Nothing happens until we subscribe to these Monos.
        //  findById will not return the user as user is not present.
        final Mono<User> findByIdMono = repository.findById(testUser.getId());
        final User findByIdUser = findByIdMono.block();
        Assert.isNull(findByIdUser, "User must be null");

        final User savedUser = saveUserMono.block();
        Assert.state(savedUser != null, "Saved user must not be null");
        Assert.state(savedUser.getFirstName().equals(testUser.getFirstName()), "Saved user first name doesn't match");

        LOGGER.info("Saved user");

        firstNameUserFlux.collectList().block();

        final Optional<User> optionalUserResult = repository.findById(testUser.getId()).blockOptional();
        Assert.isTrue(optionalUserResult.isPresent(), "Cannot find user.");

        final User result = optionalUserResult.get();
        Assert.state(result.getFirstName().equals(testUser.getFirstName()), "query result firstName doesn't match!");
        Assert.state(result.getLastName().equals(testUser.getLastName()), "query result lastName doesn't match!");

        LOGGER.info("Found user by findById : {}", result);
    }

    /*@PostConstruct
    public void setup() {
        LOGGER.info("Clear the database");
        this.repository.deleteAll().block();
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("Cleaning up users");
        this.repository.deleteAll().block();
    }*/
    
}