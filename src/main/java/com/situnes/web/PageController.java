package com.situnes.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Thymeleaf page routes. Each returns a template name; the model attributes
 * listed in TODOs are what the template needs.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index"; // landing: hero + "find your sound" CTA
    }

    @GetMapping("/genres")
    public String genres(Model model) {
        // TODO: model.addAttribute("genres", List.of("hiphop","rnb","afrobeats","pop","rock","latin"))
        return "genres";
    }

    @GetMapping("/matchup")
    public String matchup(@RequestParam String genre, Model model) {
        // TODO: model.addAttribute("pair", matchupService.nextPair(currentUser, genre))
        return "matchup";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        // TODO: model.addAttribute("profile", tasteProfileService.buildProfile(currentUser, genre))
        // TODO: model.addAttribute("topSongs", ...), ("pinnedReply", ...)
        return "profile";
    }

    @GetMapping("/community/{archetypeKey}")
    public String community(@PathVariable String archetypeKey, Model model) {
        // TODO: model.addAttribute("community", ...), ("posts", page of posts)
        return "community";
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        // TODO: model.addAttribute("communities", communityService.findAll())
        return "explore";
    }
}
