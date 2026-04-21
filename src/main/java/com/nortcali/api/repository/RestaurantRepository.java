package com.nortcali.api.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nortcali.api.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>{
	List<Restaurant> findByCityId(Long cityId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Restaurant r WHERE r.id = :id")
	Optional<Restaurant> findByIdWithLock(@Param("id") Long id);
}
