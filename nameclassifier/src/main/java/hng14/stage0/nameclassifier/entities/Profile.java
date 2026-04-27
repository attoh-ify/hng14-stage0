package hng14.stage0.nameclassifier.entities;

import hng14.stage0.nameclassifier.enums.AgeGroup;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false)
    private AgeGroup ageGroup;

    @Column(name = "country_id", nullable = false)
    private String countryId;

    @Column(name = "country_name", nullable = false)
    private String countryName;

    @Column(name = "country_probability", nullable = false)
    private double countryProbability;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Profile() {}

    public Profile(String id, String name, String gender, double genderProbability, Integer age, AgeGroup ageGroup, String countryId, String countryName, double countryProbability, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.genderProbability = genderProbability;
        this.age = age;
        this.ageGroup = ageGroup;
        this.countryId = countryId;
        this.countryName = countryName;
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
                ", age=" + age +
                ", ageGroup=" + ageGroup +
                ", countryId='" + countryId + '\'' +
                ", countryName='" + countryName + '\'' +
                ", countryProbability=" + countryProbability +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
