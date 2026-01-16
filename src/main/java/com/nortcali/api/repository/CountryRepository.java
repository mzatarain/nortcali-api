package com.nortcali.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nortcali.api.entity.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {

}
