package dev.meteor.entitlement.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.entitlement.dto.LibraryItemResponse;
import dev.meteor.entitlement.service.LibraryService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public List<LibraryItemResponse> getLibrary(
            @AuthenticationPrincipal MeteorUserPrincipal principal
    ) {
        return libraryService.getLibrary(principal.id());
    }
}
