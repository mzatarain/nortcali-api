package com.nortcali.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nortcali.api.entity.State;
import com.nortcali.api.repository.StateRepository;

@RestController
@RequestMapping("/api/states")
public class StateController {
	private final StateRepository repo;
	
	public StateController(StateRepository repo) {
        this.repo = repo;
    }
	
	@GetMapping
    public List<State> all() {
        return repo.findAll();
    }

    @PostMapping
    public State create(@RequestBody State s) {
        return repo.save(s);
    }
}
