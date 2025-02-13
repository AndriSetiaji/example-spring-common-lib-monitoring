package id.co.bjj.core.component.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
public class RestTemplateConfig {
    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Autowired
    private Tracer tracer;

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestInterceptor requestInterceptorBefore) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(requestInterceptorBefore);
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestInterceptor requestInterceptorBefore() {
        return new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                                ClientHttpRequestExecution execution) throws IOException {
                Span span = tracer.spanBuilder("rest-template-http-call").setSpanKind(SpanKind.CLIENT).startSpan();
                try (Scope scope = span.makeCurrent()) {
                    // get trace ID, span ID, flag
                    String traceId = span.getSpanContext().getTraceId();
                    String spanId = span.getSpanContext().getSpanId();
                    String traceFlags = span.getSpanContext().isSampled() ? "01" : "00";

                    // Format traceparent as W3C
                    String traceParent = String.format("00-%s-%s-%s", traceId, spanId, traceFlags);

                    // add traceparent to header
                    request.getHeaders().add("traceparent", traceParent);
                } finally {
                    span.end();
                }

                log.info("RestTemplate Request URL: {}", request.getURI());
                log.info("RestTemplate Request Method: {}", request.getMethod());
                log.info("RestTemplate Request Headers: {}", request.getHeaders());
                log.info("RestTemplate Request Body: {}", body);

                return execution.execute(request, body);
            }
        };
    }
}
