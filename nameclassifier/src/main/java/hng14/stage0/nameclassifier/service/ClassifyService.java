package hng14.stage0.nameclassifier.service;

import hng14.stage0.nameclassifier.dto.response.ClassifyDataResponse;

public interface ClassifyService {
    ClassifyDataResponse classify(String name);
}
