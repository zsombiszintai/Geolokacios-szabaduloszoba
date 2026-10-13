package com.cityscape.geoszabaduloszobabackend.service;

import java.util.List;

public interface CrudService<Entity> {
    Entity load(Long id);
    Long save(Entity entity);
    List<Entity> list();
    void delete(Long id);
}

