package hng14.stage0.nameclassifier.service.impl;

import hng14.stage0.nameclassifier.client.GenderizeClient;
import hng14.stage0.nameclassifier.dto.external.GenderizeResponse;
import hng14.stage0.nameclassifier.dto.response.ClassifyDataResponse;
import hng14.stage0.nameclassifier.exception.BadRequestException;
import hng14.stage0.nameclassifier.exception.NoPredictionException;
import hng14.stage0.nameclassifier.exception.UnprocessableEntityException;
import hng14.stage0.nameclassifier.service.ClassifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ClassifyServiceImpl implements ClassifyService {
    private static final Logger log = LoggerFactory.getLogger(ClassifyServiceImpl.class);
    private final GenderizeClient genderizeClient;

    public ClassifyServiceImpl(GenderizeClient genderizeClient) {
        this.genderizeClient = genderizeClient;
    }

    @Override
    public ClassifyDataResponse classify(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.debug("Missing or empty name parameter");
            throw new BadRequestException("Missing or empty name parameter");
        }

        String trimmedName = name.trim();

        if (!trimmedName.matches("^[a-zA-Z]+$")) {
            log.debug("name must be a valid string");
            throw new UnprocessableEntityException("name must be a valid string");
        }

        GenderizeResponse genderizeResponse = genderizeClient.classifyName(name);

        if (genderizeResponse.gender() == null || genderizeResponse.count() == 0) {
            log.debug("No prediction available for the provided name");
            throw new NoPredictionException("No prediction available for the provided name");
        }

        boolean is_confident = genderizeResponse.probability() >= 0.7 && genderizeResponse.count() >= 100;

        return new ClassifyDataResponse(
                genderizeResponse.name(),
                genderizeResponse.gender(),
                genderizeResponse.probability(),
                genderizeResponse.count(),
                is_confident,
                Instant.now().toString()
        );
    }
}
