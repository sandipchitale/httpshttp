package sandipchitale.httpshttp;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class HttpshttpApplication {


	public static void main(String[] args) {
		SpringApplication.run(HttpshttpApplication.class, args);
	}

	@Bean
	@ConditionalOnNotWarDeployment
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> connectorCustomizer(@Value("${loopback.http.port}") int loopbackHttpPort) {
		return (TomcatServletWebServerFactory tomcat) -> {
			Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
			// only allow loopback address
			connector.setProperty("address", "127.0.0.1");
			connector.setPort(loopbackHttpPort);
			connector.setScheme("http");
            tomcat.addAdditionalTomcatConnectors(connector);
        };
	}

	@RestController
	public static class IndexController {
	    
	    @GetMapping("/")
	    public String index(HttpServletRequest httpServletRequest) {
	        return "Hello " + httpServletRequest.getRequestURL();
	    }
	}

	@Bean
	public CommandLineRunner clr (RestClient.Builder restClientBuilder, RestClientSsl restClientSsl, @Value("${loopback.http.port}") int loopbackHttpPort) {
	    return (String... args) -> {
			System.out.println("Accessing https://server1:8080/ : ");
			RestClient restClient = restClientBuilder.apply(restClientSsl.fromBundle("client")).build();
			System.out.println(restClient
					.get()
					.uri("https://server1:8080/")
					.retrieve()
					.body(String.class));
			System.out.println("Accessing http://127.0.0.1:" + loopbackHttpPort + " :");
			restClient = restClientBuilder
					.build();
			System.out.println(restClient
					.get()
					.uri("http://127.0.0.1:" + loopbackHttpPort)
					.retrieve()
					.body(String.class));
		};
	}
	


}
