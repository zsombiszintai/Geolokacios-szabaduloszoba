package com.cityscape.geoszabaduloszobabackend.service;

import java.util.List;

public interface CrudService<DTO> {
    DTO load(Long id);
    Long save(DTO dto);
    List<DTO> list();
    void delete(Long id);
}

