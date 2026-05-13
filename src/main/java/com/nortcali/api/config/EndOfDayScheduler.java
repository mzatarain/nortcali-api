package com.nortcali.api.config;

import com.nortcali.api.dto.response.CloseDayResponse;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class EndOfDayScheduler {

    private final RestaurantRepository restaurantRepo;
    private final OrderService orderService;

    public EndOfDayScheduler(RestaurantRepository restaurantRepo, OrderService orderService) {
        this.restaurantRepo = restaurantRepo;
        this.orderService = orderService;
    }

    @Scheduled(cron = "0 55 23 * * *")
    public void closeAllRestaurantsDay() {
        List<Restaurant> restaurants = restaurantRepo.findByIsActiveTrue();
        int totalClosed = 0;

        for (Restaurant restaurant : restaurants) {
            try {
                CloseDayResponse result = orderService.closeDay(restaurant.getId());
                totalClosed += result.closedCount();
            } catch (Exception e) {
                log.error("[EndOfDay] Error procesando restaurante {}: {}", restaurant.getId(), e.getMessage(), e);
            }
        }

        log.info("[EndOfDay] Procesados {} restaurantes. Órdenes cerradas: {} en total.",
                restaurants.size(), totalClosed);
    }
}
