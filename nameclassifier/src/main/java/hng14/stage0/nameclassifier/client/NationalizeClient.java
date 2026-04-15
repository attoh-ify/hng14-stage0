package hng14.stage0.nameclassifier.client;

import hng14.stage0.nameclassifier.dto.external.NationalizeResponse;
import hng14.stage0.nameclassifier.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NationalizeClient {
    private static final Logger log = LoggerFactory.getLogger(NationalizeClient.class);

    private final RestClient restClient;

    public NationalizeClient(
            RestClient.Builder restClientBuilder,
            @Value("${nationalize.base-url:https://api.nationalize.io}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public NationalizeResponse nationalizeName(String name) {
        try {
            NationalizeResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("name", name).build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new UpstreamServiceException("Nationalize returned an invalid response");
                    })
                    .body(NationalizeResponse.class);

            if (response == null) {
                throw new UpstreamServiceException("Nationalize returned an invalid response");
            }

            return response;
        } catch (RestClientException ex) {
            log.error("Nationalize call failed", ex);
            throw new UpstreamServiceException("Nationalize returned an invalid response");
        }
    }
}