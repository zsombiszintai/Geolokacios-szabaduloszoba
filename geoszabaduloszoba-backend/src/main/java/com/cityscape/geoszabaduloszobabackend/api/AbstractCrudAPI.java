package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.mapper.AbstractMapper;
import com.cityscape.geoszabaduloszobabackend.service.CrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
public abstract class AbstractCrudAPI<DTO, Entity> {

    protected final AbstractMapper<Entity, DTO> mapper;
    protected final CrudService<Entity> service;

    @GetMapping(path = "load")
    public DTO load(@RequestParam("id") Long id) {
        return this.mapper.mapDTO(this.service.load(id));
    }

    @GetMapping(path = "list")
    public List<DTO> list() {
        return this.service.list().stream().map(this.mapper::mapDTO).toList();
    }

    @PostMapping(path = "save")
    public Long save( @RequestBody DTO request) {
        return  this.service.save(this.mapper.mapEntity(request));
    }

    @DeleteMapping(path = "delete")
    public void delete(@RequestParam("id") Long id) {
        this.service.delete(id);
    }
}

