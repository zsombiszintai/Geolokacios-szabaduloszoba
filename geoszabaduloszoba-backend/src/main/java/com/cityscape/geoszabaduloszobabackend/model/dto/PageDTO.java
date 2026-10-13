package com.cityscape.geoszabaduloszobabackend.model.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageDTO<T>(List<T> content, int page, int size, long totalElements,
                         int totalPages, boolean first, boolean last) {
    public static <T> PageDTO<T> from(Page<T> source) {
        return new PageDTO<>(source.getContent(), source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages(), source.isFirst(), source.isLast());
    }
}
