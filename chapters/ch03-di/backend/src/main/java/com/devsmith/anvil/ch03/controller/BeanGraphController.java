package com.devsmith.anvil.ch03.controller;

import com.devsmith.anvil.ch03.service.BeanGraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BeanGraphController {

    private final BeanGraphService service;

    public BeanGraphController(BeanGraphService service) {
        this.service = service;
    }

    @GetMapping("/api/beans/graph")
    public BeanGraphService.Graph graph() {
        return service.snapshot();
    }
}
