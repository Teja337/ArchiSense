
package com.archisense.backend.controller;

import com.archisense.backend.service.IssueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/ant/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/class-cycles/{versionIndex}")
    public List<Map<String, Object>> getClassCycles(
            @PathVariable int versionIndex) throws Exception {

        return issueService.getClassCycles(versionIndex);
    }

    @GetMapping("/package-cycles/{versionIndex}")
    public List<Map<String, Object>> getPackageCycles(
            @PathVariable int versionIndex) throws Exception {

        return issueService.getPackageCycles(versionIndex);
    }

    @GetMapping("/hub-dependencies/{versionIndex}")
    public List<Map<String, Object>> getHubDependencies(
            @PathVariable int versionIndex) throws Exception {

        return issueService.getHubDependencies(versionIndex);
    }

    @GetMapping("/unstable-dependencies/{versionIndex}")
    public List<Map<String, Object>> getUnstableDependencies(
            @PathVariable int versionIndex) throws Exception {

        return issueService.getUnstableDependencies(versionIndex);
    }
}
