package hng14.stage0.nameclassifier.client;

import hng14.stage0.nameclassifier.dto.external.GenderizeResponse;
import hng14.stage0.nameclassifier.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GenderizeClient {
    private final RestClient restClient;

    private static final Logger log = LoggerFactory.getLogger(GenderizeClient.class);

    public GenderizeClient(RestClient.Builder restClientBuilder, @Value("${genderize.base-url:https://api.genderize.io}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public GenderizeResponse classifyName(String name) {
        try {
            GenderizeResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("name", name).build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new UpstreamServiceException("Failed to fetch data from upstream service");
                    })
                    .body(GenderizeResponse.class);

            if (response == null) {
                throw new UpstreamServiceException("Empty response from upstream service");
            }

            return response;
        } catch (RestClientException ex) {
            log.error("Failed to fetch data from upstream service", ex);
            throw new UpstreamServiceException("Failed to fetch data from upstream service");
        }
    }
}
