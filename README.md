# Description

A simple Spring Boot app that configures

- Https on port 8080 listening on 0.0.0.0 
  - without SslBundle, using trustAllCertsTrustManagers and NoopHostnameVerifier.
  - with `SslBundle` named `server` that wraps `server1.jks` keystore.
- Http on port `loopback.http.port` listening on 127.0.0.1 only when running a embedded Tomcat

Accesses each using `RestClient` instances, one of which is configured with `SslBundle` named `client` that wraps `server1-truststore.jks` truststore.

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.0/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.0/gradle-plugin/reference/html/#build-image)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

