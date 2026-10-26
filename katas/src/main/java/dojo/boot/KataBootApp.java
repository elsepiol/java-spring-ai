package dojo.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Spring Boot application of track 07. @WebMvcTest and @SpringBootTest in dojo.boot.* find it
 * by searching up the package tree for a @SpringBootConfiguration. You can also run it:
 *   cd katas && ./mvnw spring-boot:run   (not configured)   or run main() from your IDE,
 * then try:  curl -i localhost:8080/api/books
 */
@SpringBootApplication
public class KataBootApp {

	public static void main(String[] args) {
		SpringApplication.run(KataBootApp.class, args);
	}

}
