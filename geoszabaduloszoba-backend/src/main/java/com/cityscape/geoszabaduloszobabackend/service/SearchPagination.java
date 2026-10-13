package com.cityscape.geoszabaduloszobabackend.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class SearchPagination {
    private SearchPagination() {}

    public static PageRequest request(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A lap sorszáma nem lehet negatív, a lapméret 1 és 100 közötti legyen.");
        }
        return PageRequest.of(page, size, Sort.by("id"));
    }
}
