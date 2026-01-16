package com.nortcali.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nortcali.api.entity.Country;
import com.nortcali.api.repository.CountryRepository;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryRepository repo;

    public CountryController(CountryRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Country> all() {
        return repo.findAll();
    }

    @PostMapping
    public Country create(@RequestBody Country c) {
        return repo.save(c);
    }
}