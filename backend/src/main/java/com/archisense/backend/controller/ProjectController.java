package com.archisense.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @GetMapping
    public List<Map<String, String>> getProjects() {

        return List.of(
            Map.of(
                "id", "ant",
                "name", "Apache Ant",
                "description", "Java build automation project",
                "status", "AVAILABLE"
            )
        );
    }
}