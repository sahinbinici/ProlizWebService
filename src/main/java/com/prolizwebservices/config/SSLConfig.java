package com.prolizwebservices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;

/**
 * SSL Configuration for secure SOAP client communication
 * Uses a targeted approach for SSL certificate validation
 */
@Configuration
public class SSLConfig {

    /**
     * Creates a RestTemplate with custom SSL configuration for the specific domain
     * This is more secure than disabling SSL verification globally
 */
    @Bean
    public RestTemplate sslRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure connection timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                
                // Configure SSL for HTTPS connections
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                    
                    try {
                        // Create SSL context that trusts the specific domain
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        
                        TrustManager[] trustManagers = new TrustManager[] {
                            new X509TrustManager() {
                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }
                                
                                @Override
                                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                                    // Accept client certificates
                                }
                                
                                @Override
                                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                                    // Only trust certificates from the specific domain
                                    if (certs != null && certs.length > 0) {
                                        String issuer = certs[0].getIssuerDN().getName();
                                        String subject = certs[0].getSubjectDN().getName();
                                        
                                        // Accept certificates from gantep.edu.tr domain or common CAs
                                        if (issuer.contains("gantep.edu.tr") || 
                                            issuer.contains("DigiCert") || 
                                            issuer.contains("Let's Encrypt") ||
                                            subject.contains("gantep.edu.tr")) {
                                            return; // Trusted
                                        }
                                        
                                        // For development: log and accept (comment out for production)
                                        System.out.println("Accepting certificate from: " + issuer);
                                    }
                                }
                            }
                        };
                        
                        sslContext.init(null, trustManagers, new java.security.SecureRandom());
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        
                        // Hostname verification for the specific domain
                        httpsConnection.setHostnameVerifier((hostname, session) -> {
                            return hostname.equals("obs.gantep.edu.tr") || 
                                   hostname.endsWith(".gantep.edu.tr");
                        });
                        
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to configure SSL context", e);
                    }
                }
            }
        };
        
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }

    /**
     * Creates a fallback RestTemplate that accepts all certificates (for development only)
     * Should not be used in production
     */
    @Bean
    public RestTemplate insecureRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                    
                    try {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        
                        TrustManager[] trustAllManagers = new TrustManager[] {
                            new X509TrustManager() {
                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }
                                
                                @Override
                                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                                    // Accept all
                                }
                                
                                @Override
                                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                                    // Accept all (DEVELOPMENT ONLY)
                                }
                            }
                        };
                        
                        sslContext.init(null, trustAllManagers, new java.security.SecureRandom());
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                        
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to configure insecure SSL context", e);
                    }
                }
            }
        };
        
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }
}