package hng14.stage0.nameclassifier.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hng14.stage0.nameclassifier.dto.payload.SeedProfileDto;
import hng14.stage0.nameclassifier.dto.response.AgeGroup;
import hng14.stage0.nameclassifier.entities.Profile;
import hng14.stage0.nameclassifier.repositories.ProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDatabase(ProfileRepository repository, ObjectMapper objectMapper) {
        return args -> {

            // Prevent reseeding
            if (repository.count() > 0) {
                return;
            }

            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("data/seed_profiles.json");

            if (inputStream == null) {
                throw new IllegalStateException("Seed file not found: data/seed_profiles.json");
            }

            List<SeedProfileDto> profiles = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<SeedProfileDto>>() {}
            );

            List<Profile> entities = profiles.stream()
                    .map(dto -> new Profile(
                            dto.id(),
                            dto.name().toLowerCase(), // consistent with your design
                            dto.gender(),
                            dto.gender_probability(),
                            dto.age(),
                            AgeGroup.valueOf(dto.age_group().toLowerCase()),
                            dto.country_id(),
                            dto.country_name(),
                            dto.country_probability(),
                            Instant.parse(dto.created_at())
                    ))
                    .toList();

            repository.saveAll(entities);
        };
    }
}