package hng14.stage0.nameclassifier.entities;

import hng14.stage0.nameclassifier.dto.response.AgeGroup;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "gender_probability", nullable = false)
    private double genderProbability;

    @Column(name = "sample_size", nullable = false)
    private long sampleSize;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false)
    private AgeGroup ageGroup;

    @Column(name = "country_id", nullable = false)
    private String countryId;

    @Column(name = "country_probability", nullable = false)
    private double countryProbability;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public Profile() {}

    public Profile(String id, String name, String gender, double genderProbability, long sampleSize, Integer age, AgeGroup ageGroup, String countryId, double countryProbability, String createdAt) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.genderProbability = genderProbability;
        this.sampleSize = sampleSize;
        this.age = age;
        this.ageGroup = ageGroup;
        this.countryId = countryId;
        this.countryProbability = countryProbability;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", genderProbability=" + genderProbability +
                ", sampleSize=" + sampleSize +
                ", age=" + age +
                ", ageGroup=" + ageGroup +
                ", countryId='" + countryId + '\'' +
                ", countryProbability=" + countryProbability +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
