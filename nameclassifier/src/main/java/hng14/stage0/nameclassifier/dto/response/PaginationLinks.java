package hng14.stage0.nameclassifier.dto.response;

public record PaginationLinks(
        String self,
        String next,
        String prev
) {
}