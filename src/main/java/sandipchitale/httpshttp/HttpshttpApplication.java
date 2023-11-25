package sandipchitale.httpshttp;

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
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@SpringBootApplication
public class HttpshttpApplication {
	private static final Logger LOG = LoggerFactory.getLogger(HttpshttpApplication.class);

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
	public CommandLineRunner clr (RestClientSsl restClientSsl, @Value("${loopback.http.port}") int loopbackHttpPort) {
	    return (String... args) -> {
			System.out.println("Accessing https://server1:8080/ without SslBundle but all trusting TrustManager : ");
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
			SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

			final HttpClientConnectionManager httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder
					.create()
					.setSSLSocketFactory(sslConnectionSocketFactory)
					.build();

			CloseableHttpClient closeableHttpClient = HttpClients.custom()
					.setConnectionManager(httpClientConnectionManager)
					.evictExpiredConnections()
					.build();

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(closeableHttpClient);

			RestClient restClient = RestClient.builder()
					.requestFactory(requestFactory)
					.build();
			System.out.println(restClient
					.get()
					.uri("https://server1:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Accessing https://192.168.56.1:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restClient
					.get()
					.uri("https://192.168.56.1:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Accessing https://127.0.0.1:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restClient
					.get()
					.uri("https://127.0.0.1:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Accessing https://localhost:8080/ without SslBundle but all trusting TrustManager : ");
			System.out.println(restClient
					.get()
					.uri("https://localhost:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Accessing with SslBundle https://server1:8080/ : ");
			restClient = RestClient.builder().apply(restClientSsl.fromBundle("client")).build();
			System.out.println(restClient
					.get()
					.uri("https://server1:8080/")
					.retrieve()
					.body(String.class));

			System.out.println("Accessing http://127.0.0.1:" + loopbackHttpPort + " :");
			restClient = RestClient.builder().build();
			System.out.println(restClient
					.get()
					.uri("http://127.0.0.1:" + loopbackHttpPort)
					.retrieve()
					.body(String.class));
		};
	}

}