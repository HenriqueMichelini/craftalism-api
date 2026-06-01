package io.github.HenriqueMichelini.craftalism.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageResponseDTO<T>(
    List<T> content,
    PageableDTO pageable,
    int totalPages,
    long totalElements,
    boolean last,
    int size,
    int number,
    SortDTO sort,
    int numberOfElements,
    boolean first,
    boolean empty
) {
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(
            page.getContent(),
            PageableDTO.from(page.getPageable()),
            page.getTotalPages(),
            page.getTotalElements(),
            page.isLast(),
            page.getSize(),
            page.getNumber(),
            SortDTO.from(page.getSort()),
            page.getNumberOfElements(),
            page.isFirst(),
            page.isEmpty()
        );
    }

    public record PageableDTO(
        int pageNumber,
        int pageSize,
        SortDTO sort,
        long offset,
        boolean paged,
        boolean unpaged
    ) {
        private static PageableDTO from(Pageable pageable) {
            if (pageable.isUnpaged()) {
                return new PageableDTO(
                    0,
                    0,
                    SortDTO.from(pageable.getSort()),
                    0,
                    false,
                    true
                );
            }

            return new PageableDTO(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                SortDTO.from(pageable.getSort()),
                pageable.getOffset(),
                true,
                false
            );
        }
    }

    public record SortDTO(boolean empty, boolean sorted, boolean unsorted) {
        private static SortDTO from(Sort sort) {
            return new SortDTO(sort.isEmpty(), sort.isSorted(), sort.isUnsorted());
        }
    }
}
