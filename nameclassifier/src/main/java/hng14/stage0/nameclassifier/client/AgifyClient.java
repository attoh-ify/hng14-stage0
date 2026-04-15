package hng14.stage0.nameclassifier.client;

import hng14.stage0.nameclassifier.dto.external.AgifyResponse;
import hng14.stage0.nameclassifier.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AgifyClient {
    private static final Logger log = LoggerFactory.getLogger(AgifyClient.class);

    private final RestClient restClient;

    public AgifyClient(
            RestClient.Builder restClientBuilder,
            @Value("${agify.base-url:https://api.agify.io}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public AgifyResponse agifyName(String name) {
        try {
            AgifyResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("name", name).build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new UpstreamServiceException("Agify returned an invalid response");
                    })
                    .body(AgifyResponse.class);

            if (response == null) {
                throw new UpstreamServiceException("Agify returned an invalid response");
            }

            return response;
        } catch (RestClientException ex) {
            log.error("Agify call failed", ex);
            throw new UpstreamServiceException("Agify returned an invalid response");
        }
    }
}