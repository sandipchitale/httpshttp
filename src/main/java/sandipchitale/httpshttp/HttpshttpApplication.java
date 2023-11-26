package sandipchitale.httpshttp;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.KeyManagerFactoryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.Connector;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientSsl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@SpringBootApplication
public class HttpshttpApplication {
	private static final Logger LOG = LoggerFactory.getLogger(HttpshttpApplication.class);

	private static HttpComponentsClientHttpRequestFactory requestFactory;
	private final SslBundle insecureSslBundle;

	HttpshttpApplication() {
		SslManagerBundle insecureSslManagerBundle = SslManagerBundle.of(new KeyManagerFactoryWrapper(new KeyManager() {}),
				InsecureTrustManagerFactory.INSTANCE);
		insecureSslBundle = SslBundle.of(null,
				null,
				null,
				null,
				insecureSslManagerBundle);
	}

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
	public CommandLineRunner clrWebClient(WebClientSsl webClientSsl,
										  @Value("${loopback.http.port}") int loopbackHttpPort) {
		return (String... args) -> {
			WebClient webClient;
			webClient = WebClient.builder()
								 .apply(webClientSsl.fromBundle(insecureSslBundle))
								 .build();

			System.out.println("Using WebClient: Accessing with SslBundle insecureSslBundle https://server1:8080/ : ");
			System.out.println(webClient
					.get()
					.uri("https://server1:8080/")
					.retrieve()
					.bodyToMono(String.class)
					.block()
			);

            System.out.println("Using WebClient: Accessing with SslBundle insecureSslBundle https://127.0.0.1:8080/ : ");
			System.out.println(webClient
					.get()
					.uri("https://127.0.0.1:8080/")
					.retrieve()
					.bodyToMono(String.class)
					.block()
			);

            System.out.println("Using WebClient: Accessing with SslBundle insecureSslBundle https://localhost:8080/ : ");
			System.out.println(webClient
					.get()
					.uri("https://localhost:8080/")
					.retrieve()
					.bodyToMono(String.class)
					.block()
			);

			System.out.println("Using WebClient: Accessing with SslBundle 'client' https://server1:8080/ : ");
			webClient = WebClient.builder().apply(webClientSsl.fromBundle("client")).build();
			System.out.println(webClient
					.get()
					.uri("https://server1:8080/")
					.retrieve()
					.bodyToMono(String.class)
					.block()
			);

			System.out.println("Using WebClient: Accessing http://127.0.0.1:" + loopbackHttpPort + " :");
			webClient = WebClient.builder().build();
			System.out.println(webClient
					.get()
					.uri("http://127.0.0.1:" + loopbackHttpPort)
					.retrieve()
					.bodyToMono(String.class)
					.block()
			);
		};
	}

	@Bean
	public CommandLineRunner clrRestClient(RestClientSsl restClientSsl,
										   @Value("${loopback.http.port}") int loopbackHttpPort) {
		return (String... args) -> {
			HttpComponentsClientHttpRequestFactory requestFactory = getHttpComponentsClientHttpRequestFactory();

			RestClient restClient = RestClient.builder()
											  .requestFactory(requestFactory)
											  .build();

			System.out.println("Using RestClient: Accessing https://server1:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restClient
					.get()
					.uri("https://server1:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Using RestClient: Accessing https://127.0.0.1:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restClient
					.get()
					.uri("https://127.0.0.1:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Using RestClient: Accessing https://localhost:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restClient
					.get()
					.uri("https://localhost:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Using RestClient: Accessing with SslBundle 'client' https://server1:8080/ : ");
			restClient = RestClient.builder().apply(restClientSsl.fromBundle("client")).build();
			System.out.println(restClient
					.get()
					.uri("https://server1:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Using RestClient: Accessing http://127.0.0.1:" + loopbackHttpPort + " :");
			restClient = RestClient.builder().build();
			System.out.println(restClient
					.get()
					.uri("http://127.0.0.1:" + loopbackHttpPort)
					.retrieve()
					.body(String.class));
		};
	}

	@Bean
	public CommandLineRunner clrRestTemplate(RestTemplateBuilder restTemplateBuilder,
											 SslBundles sslBundles,
											 @Value("${loopback.http.port}") int loopbackHttpPort) {
		return (String... args) -> {
			HttpComponentsClientHttpRequestFactory requestFactory = getHttpComponentsClientHttpRequestFactory();

			System.out.println("Using RestTemplate: Accessing https://server1:8080/ without SslBundle but all trusting TrustManager : ");
			RestTemplate restTemplate = restTemplateBuilder
					.requestFactory(() -> requestFactory)
					.build();
			System.out.println(restTemplate.getForObject("https://server1:8080/", String.class));

			System.out.println("Using RestTemplate: Accessing https://127.0.0.1:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restTemplate.getForObject("https://127.0.0.1:8080/", String.class));

			System.out.println("Using RestTemplate: Accessing https://localhost:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restTemplate.getForObject("https://localhost:8080/", String.class));

			System.out.println("Using RestTemplate: Accessing with SslBundle 'client' https://server1:8080/ : ");
			restTemplate = restTemplateBuilder.setSslBundle(sslBundles.getBundle("client")).build();
			System.out.println(restTemplate.getForObject("https://server1:8080/", String.class));

			System.out.println("Using RestTemplate: Accessing http://127.0.0.1:" + loopbackHttpPort + " :");
			restTemplate = restTemplateBuilder.build();
			System.out.println(restTemplate.getForObject("http://127.0.0.1:" + loopbackHttpPort, String.class));
		};
	}

	private static HttpComponentsClientHttpRequestFactory getHttpComponentsClientHttpRequestFactory() throws NoSuchAlgorithmException, KeyManagementException {
		if (requestFactory != null) {
			return requestFactory;
		}
		TrustManager[] trustAllCertsTrustManagers = new TrustManager[] {
				new X509ExtendedTrustManager() {
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {}

					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {}

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkClientTrusted(X509Certificate[] certs, String authType) {}

					@Override
					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				}
		};

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustAllCertsTrustManagers, null);
		SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
				new NoopHostnameVerifier());

		final HttpClientConnectionManager httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder
				.create()
				.setSSLSocketFactory(sslConnectionSocketFactory)
				.build();

		CloseableHttpClient closeableHttpClient = HttpClients.custom()
															 .setConnectionManager(httpClientConnectionManager)
															 .evictExpiredConnections()
															 .build();

		requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(closeableHttpClient);
		return requestFactory;
	}
}
