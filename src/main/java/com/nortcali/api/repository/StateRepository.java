package com.nortcali.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nortcali.api.entity.State;

public interface StateRepository extends JpaRepository<State, Long>{

}
