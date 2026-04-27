package hng14.stage0.nameclassifier.utils;

import hng14.stage0.nameclassifier.dto.payload.ParsedSearchQuery;
import hng14.stage0.nameclassifier.enums.AgeGroup;
import hng14.stage0.nameclassifier.exception.BadRequestException;
import hng14.stage0.nameclassifier.exception.UnprocessableEntityException;
import org.springframework.data.domain.Sort;

import java.util.Map;
import static java.util.Map.entry;

public final class Helpers {
    public static final Map<String, String> COUNTRY_MAP = Map.ofEntries(
            entry("afghanistan", "AF"), entry("albania", "AL"), entry("algeria", "DZ"), entry("american samoa", "AS"),
            entry("andorra", "AD"), entry("angola", "AO"), entry("anguilla", "AI"), entry("antarctica", "AQ"),
            entry("antigua and barbuda", "AG"), entry("argentina", "AR"), entry("armenia", "AM"), entry("aruba", "AW"),
            entry("australia", "AU"), entry("austria", "AT"), entry("azerbaijan", "AZ"), entry("bahamas", "BS"),
            entry("bahrain", "BH"), entry("bangladesh", "BD"), entry("barbados", "BB"), entry("belarus", "BY"),
            entry("belgium", "BE"), entry("belize", "BZ"), entry("benin", "BJ"), entry("bermuda", "BM"),
            entry("bhutan", "BT"), entry("bolivia", "BO"), entry("bosnia and herzegovina", "BA"), entry("botswana", "BW"),
            entry("brazil", "BR"), entry("british indian ocean territory", "IO"), entry("brunei darussalam", "BN"), entry("bulgaria", "BG"),
            entry("burkina faso", "BF"), entry("burundi", "BI"), entry("cabo verde", "CV"), entry("cambodia", "KH"),
            entry("cameroon", "CM"), entry("canada", "CA"), entry("cayman islands", "KY"), entry("central african republic", "CF"),
            entry("chad", "TD"), entry("chile", "CL"), entry("china", "CN"), entry("christmas island", "CX"),
            entry("cocos (keeling) islands", "CC"), entry("colombia", "CO"), entry("comoros", "KM"), entry("congo", "CG"),
            entry("congo (democratic republic)", "CD"), entry("cook islands", "CK"), entry("costa rica", "CR"), entry("croatia", "HR"),
            entry("cuba", "CU"), entry("curaçao", "CW"), entry("cyprus", "CY"), entry("czechia", "CZ"),
            entry("denmark", "DK"), entry("djibouti", "DJ"), entry("dominica", "DM"), entry("dominican republic", "DO"),
            entry("ecuador", "EC"), entry("egypt", "EG"), entry("el salvador", "SV"), entry("equatorial guinea", "GQ"),
            entry("eritrea", "ER"), entry("estonia", "EE"), entry("eswatini", "SZ"), entry("ethiopia", "ET"),
            entry("falkland islands", "FK"), entry("faroe islands", "FO"), entry("fiji", "FJ"), entry("finland", "FI"),
            entry("france", "FR"), entry("french guiana", "GF"), entry("french polynesia", "PF"), entry("gabon", "GA"),
            entry("gambia", "GM"), entry("georgia", "GE"), entry("germany", "DE"), entry("ghana", "GH"),
            entry("gibraltar", "GI"), entry("greece", "GR"), entry("greenland", "GL"), entry("grenada", "GD"),
            entry("guadeloupe", "GP"), entry("guam", "GU"), entry("guatemala", "GT"), entry("guernsey", "GG"),
            entry("guinea", "GN"), entry("guinea-bissau", "GW"), entry("guyana", "GY"), entry("haiti", "HT"),
            entry("holy see", "VA"), entry("honduras", "HN"), entry("hong kong", "HK"), entry("hungary", "HU"),
            entry("iceland", "IS"), entry("india", "IN"), entry("indonesia", "ID"), entry("iran", "IR"),
            entry("iraq", "IQ"), entry("ireland", "IE"), entry("isle of man", "IM"), entry("israel", "IL"),
            entry("italy", "IT"), entry("jamaica", "JM"), entry("japan", "JP"), entry("jersey", "JE"),
            entry("jordan", "JO"), entry("kazakhstan", "KZ"), entry("kenya", "KE"), entry("kiribati", "KI"),
            entry("north korea", "KP"), entry("south korea", "KR"), entry("kuwait", "KW"), entry("kyrgyzstan", "KG"),
            entry("laos", "LA"), entry("latvia", "LV"), entry("lebanon", "LB"), entry("lesotho", "LS"),
            entry("liberia", "LR"), entry("libya", "LY"), entry("liechtenstein", "LI"), entry("lithuania", "LT"),
            entry("luxembourg", "LU"), entry("macao", "MO"), entry("madagascar", "MG"), entry("malawi", "MW"),
            entry("malaysia", "MY"), entry("maldives", "MV"), entry("mali", "ML"), entry("malta", "MT"),
            entry("marshall islands", "MH"), entry("martinique", "MQ"), entry("mauritania", "MR"), entry("mauritius", "MU"),
            entry("mayotte", "YT"), entry("mexico", "MX"), entry("micronesia", "FM"), entry("moldova", "MD"),
            entry("monaco", "MC"), entry("mongolia", "MN"), entry("montenegro", "ME"), entry("montserrat", "MS"),
            entry("morocco", "MA"), entry("mozambique", "MZ"), entry("myanmar", "MM"), entry("namibia", "NA"),
            entry("nauru", "NR"), entry("nepal", "NP"), entry("netherlands", "NL"), entry("new caledonia", "NC"),
            entry("new zealand", "NZ"), entry("nicaragua", "NI"), entry("niger", "NE"), entry("nigeria", "NG"),
            entry("niue", "NU"), entry("norfolk island", "NF"), entry("north macedonia", "MK"), entry("northern mariana islands", "MP"),
            entry("norway", "NO"), entry("oman", "OM"), entry("pakistan", "PK"), entry("palau", "PW"),
            entry("palestine", "PS"), entry("panama", "PA"), entry("papua new guinea", "PG"), entry("paraguay", "PY"),
            entry("peru", "PE"), entry("philippines", "PH"), entry("pitcairn", "PN"), entry("poland", "PL"),
            entry("portugal", "PT"), entry("puerto rico", "PR"), entry("qatar", "QA"), entry("réunion", "RE"),
            entry("romania", "RO"), entry("russian federation", "RU"), entry("rwanda", "RW"), entry("saint barthélemy", "BL"),
            entry("saint helena", "SH"), entry("saint kitts and nevis", "KN"), entry("saint lucia", "LC"), entry("saint martin", "MF"),
            entry("saint pierre and miquelon", "PM"), entry("saint vincent and the grenadines", "VC"), entry("samoa", "WS"), entry("san marino", "SM"),
            entry("sao tome and principe", "ST"), entry("saudi arabia", "SA"), entry("senegal", "SN"), entry("serbia", "RS"),
            entry("seychelles", "SC"), entry("sierra leone", "SL"), entry("singapore", "SG"), entry("sint maarten", "SX"),
            entry("slovakia", "SK"), entry("slovenia", "SI"), entry("solomon islands", "SB"), entry("somalia", "SO"),
            entry("south africa", "ZA"), entry("south georgia", "GS"), entry("south sudan", "SS"), entry("spain", "ES"),
            entry("sri lanka", "LK"), entry("sudan", "SD"), entry("suriname", "SR"), entry("svalbard and jan mayen", "SJ"),
            entry("sweden", "SE"), entry("switzerland", "CH"), entry("syria", "SY"), entry("taiwan", "TW"),
            entry("tajikistan", "TJ"), entry("tanzania", "TZ"), entry("thailand", "TH"), entry("timor-leste", "TL"),
            entry("togo", "TG"), entry("tokelau", "TK"), entry("tonga", "TO"), entry("trinidad and tobago", "TT"),
            entry("tunisia", "TN"), entry("turkey", "TR"), entry("turkmenistan", "TM"), entry("turks and caicos islands", "TC"),
            entry("tuvalu", "TV"), entry("uganda", "UG"), entry("ukraine", "UA"), entry("united arab emirates", "AE"),
            entry("united kingdom", "GB"), entry("united states", "US"), entry("uruguay", "UY"), entry("uzbekistan", "UZ"),
            entry("vanuatu", "VU"), entry("venezuela", "VE"), entry("viet nam", "VN"), entry("virgin islands (british)", "VG"),
            entry("virgin islands (u.s.)", "VI"), entry("wallis and futuna", "WF"), entry("western sahara", "EH"), entry("yemen", "YE"),
            entry("zambia", "ZM"), entry("zimbabwe", "ZW")
    );

    public static AgeGroup resolveAgeGroup(int age) {
        if (age <= 12) return AgeGroup.child;
        if (age <= 19) return AgeGroup.teenager;
        if (age <= 59) return AgeGroup.adult;
        return AgeGroup.senior;
    }

    public static ParsedSearchQuery parse(String query){
        if (query == null || query.trim().isEmpty()) {
            throw new BadRequestException("Missing or empty parameter");
        }

        String normalized = query.trim().toLowerCase();

        String gender = parseGender(normalized);
        AgeGroup ageGroup = parseAgeGroup(normalized);
        Integer minAge = parseMinAge(normalized);
        Integer maxAge = parseMaxAge(normalized);
        String countryId = parseCountry(normalized);

        if (normalized.contains("young")) {
            minAge = 16;
            maxAge = 24;
        }

        boolean hasMeaningfulFilter =
                gender != null ||
                        ageGroup != null ||
                        minAge != null ||
                        maxAge != null ||
                        countryId != null;

        if (!hasMeaningfulFilter) {
            throw new UnprocessableEntityException("Unable to interpret query");
        }

        return new ParsedSearchQuery(gender, ageGroup, minAge, maxAge, countryId);
    }

    public static AgeGroup parseAgeGroupParam(String ageGroup) {
        if (ageGroup == null || ageGroup.isBlank()) {
            return null;
        }

        try {
            return AgeGroup.valueOf(ageGroup.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
    }

    public static String parseGender(String query) {
        if (query == null || query.isBlank()) return null;

        String[] tokens = query.toLowerCase().split("\\W+");

        boolean hasMale = false;
        boolean hasFemale = false;

        for (String token : tokens) {
            if (token.equals("male") || token.equals("males")) {
                hasMale = true;
            }
            if (token.equals("female") || token.equals("females")) {
                hasFemale = true;
            }
        }

        if (hasMale && hasFemale) return null;
        if (hasMale) return "male";
        if (hasFemale) return "female";

        return null;
    }

    public static AgeGroup parseAgeGroup(String query) {
        if (query.contains("child") || query.contains("children")) {
            return AgeGroup.child;
        }
        if (query.contains("teenager") || query.contains("teenagers")) {
            return AgeGroup.teenager;
        }
        if (query.contains("adult") || query.contains("adults")) {
            return AgeGroup.adult;
        }
        if (query.contains("senior") || query.contains("seniors")) {
            return AgeGroup.senior;
        }
        return null;
    }

    public static Integer parseMinAge(String query) {
        String[] tokens = query.split("\\s+");

        for (int i = 0; i < tokens.length - 1; i++) {
            if ((tokens[i].equals("above") || tokens[i].equals("over")) && isInteger(tokens[i + 1])) {
                return Integer.parseInt(tokens[i + 1]);
            }
        }

        return null;
    }

    public static Integer parseMaxAge(String query) {
        String[] tokens = query.split("\\s+");

        for (int i = 0; i < tokens.length - 1; i++) {
            if ((tokens[i].equals("below") || tokens[i].equals("under")) && isInteger(tokens[i + 1])) {
                return Integer.parseInt(tokens[i + 1]);
            }
        }

        return null;
    }

    public static String parseCountry(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String normalized = query.toLowerCase();
        return COUNTRY_MAP.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                .filter(entry -> {
                        String countryName = java.util.regex.Pattern.quote(entry.getKey().toLowerCase());
                        return normalized.matches(".*\\bfrom\\s+" + countryName + "\\b.*")
                                || normalized.matches(".*\\b" + countryName + "\\b.*");
                        })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static Sort buildSort(String sortBy, String order) {
        String normalizedSortBy = (sortBy == null || sortBy.isBlank())
                ? "created_at"
                : sortBy.trim().toLowerCase();

        String normalizedOrder = (order == null || order.isBlank())
                ? "asc"
                : order.trim().toLowerCase();

        Sort.Direction direction = switch (normalizedOrder) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new UnprocessableEntityException("Invalid query parameters");
        };

        String sortField = switch (normalizedSortBy) {
            case "age" -> "age";
            case "created_at" -> "createdAt";
            case "gender_probability" -> "genderProbability";
            default -> throw new UnprocessableEntityException("Invalid query parameters");
        };

        return Sort.by(direction, sortField);
    }

    public static void validateQueryParams(
            Integer minAge,
            Integer maxAge,
            Double minGenderProbability,
            Double minCountryProbability,
            Integer page,
            Integer limit
    ) {
        if (minAge != null && minAge < 0) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
        if (maxAge != null && maxAge < 0) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
        if (minGenderProbability != null && (minGenderProbability < 0 || minGenderProbability > 1)) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
        if (minCountryProbability != null && (minCountryProbability < 0 || minCountryProbability > 1)) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
        if (page != null && page < 1) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
        if (limit != null && (limit < 1 || limit > 50)) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }
    }
}
