package az.freight.factory;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequestFactory {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;

    private PageRequestFactory() {}

    public static PageRequest of(int page, int size) {
        return of(page, size, null, Set.of("id"), "id", Sort.Direction.ASC);
    }

    public static PageRequest of(
            int page,
            int size,
            String sort,
            Set<String> allowed,
            String defaultField,
            Sort.Direction defaultDirection
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);

        String field = defaultField;
        Sort.Direction direction = defaultDirection;

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String requested = parts[0].trim();
            if (allowed.contains(requested)) {
                field = requested;
            }
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            } else if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }

        Sort ordering = "id".equals(field)
                ? Sort.by(direction, "id")
                : Sort.by(new Sort.Order(direction, field), Sort.Order.asc("id"));

        return PageRequest.of(safePage, safeSize, ordering);
    }
}



