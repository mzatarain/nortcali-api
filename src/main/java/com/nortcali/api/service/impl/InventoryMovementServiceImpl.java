package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.InventoryMovementRequest;
import com.nortcali.api.dto.response.InventoryMovementResponse;
import com.nortcali.api.entity.InventoryMovement;
import com.nortcali.api.entity.Supply;
import com.nortcali.api.entity.enums.MovementType;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.InventoryMovementMapper;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.InventoryMovementRepository;
import com.nortcali.api.repository.SupplyRepository;
import com.nortcali.api.service.InventoryMovementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@Slf4j
public class InventoryMovementServiceImpl implements InventoryMovementService {

    private final InventoryMovementRepository movementRepo;
    private final SupplyRepository supplyRepo;
    private final EmployeeRepository employeeRepo;
    private final InventoryMovementMapper mapper;

    public InventoryMovementServiceImpl(InventoryMovementRepository movementRepo,
                                        SupplyRepository supplyRepo,
                                        EmployeeRepository employeeRepo,
                                        InventoryMovementMapper mapper) {
        this.movementRepo = movementRepo;
        this.supplyRepo = supplyRepo;
        this.employeeRepo = employeeRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> getBySupply(Long supplyId) {
        return movementRepo.findBySupplyIdOrderByCreatedAtDesc(supplyId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public InventoryMovementResponse register(Long supplyId, InventoryMovementRequest request) {
        Supply supply = supplyRepo.findById(supplyId)
                .orElseThrow(() -> new ResourceNotFoundException("Supply", supplyId));

        if (!supply.isActive()) {
            throw new BusinessRuleException("El insumo está inactivo y no puede recibir movimientos");
        }

        MovementType type;
        try {
            type = MovementType.valueOf(request.getMovementType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Tipo de movimiento inválido: " + request.getMovementType()
                    + ". Valores permitidos: entrada, salida, merma, ajuste");
        }

        BigDecimal qty = request.getQuantity();
        BigDecimal newStock = switch (type) {
            case ENTRADA -> supply.getCurrentStock().add(qty);
            case SALIDA, MERMA -> {
                BigDecimal result = supply.getCurrentStock().subtract(qty);
                if (result.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessRuleException(
                            "Stock insuficiente para el movimiento. Stock actual: " + supply.getCurrentStock());
                }
                yield result;
            }
            case AJUSTE -> qty;
        };

        supply.setCurrentStock(newStock);
        supplyRepo.save(supply);

        // Advertencia si cae por debajo del mínimo
        if (newStock.compareTo(supply.getMinimumStock()) < 0) {
            log.warn("ALERTA: insumo '{}' (id={}) está por debajo del stock mínimo. Stock: {}, Mínimo: {}",
                    supply.getName(), supply.getId(), newStock, supply.getMinimumStock());
        }

        var employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        InventoryMovement movement = new InventoryMovement();
        movement.setSupply(supply);
        movement.setEmployee(employee);
        movement.setMovementType(type);
        movement.setQuantity(qty);

        log.info("Movimiento {} de {} unidades en insumo {}", type, qty, supplyId);
        return mapper.toResponse(movementRepo.save(movement));
    }
}
