package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.EmployeeRoleRequest;
import com.nortcali.api.dto.response.EmployeeRoleResponse;
import com.nortcali.api.entity.EmployeeRole;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.EmployeeRoleMapper;
import com.nortcali.api.repository.EmployeeRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeRoleServiceImplTest {

    @Mock EmployeeRoleRepository repo;
    @Mock EmployeeRoleMapper mapper;

    @InjectMocks
    EmployeeRoleServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private EmployeeRole role(Long id, String name, boolean active) {
        EmployeeRole r = new EmployeeRole();
        r.setId(id);
        r.setName(name);
        r.setDescription("Descripción de " + name);
        r.setActive(active);
        return r;
    }

    private EmployeeRoleRequest request(String name, String description) {
        EmployeeRoleRequest r = new EmployeeRoleRequest();
        r.setName(name);
        r.setDescription(description);
        return r;
    }

    // ── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_devuelveSoloActivos_ordenadosPorNombre() {
        EmployeeRole admin = role(1L, "ADMIN", true);
        EmployeeRole waiter = role(2L, "WAITER", true);
        when(repo.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(admin, waiter));
        when(mapper.toResponse(admin)).thenReturn(mock(EmployeeRoleResponse.class));
        when(mapper.toResponse(waiter)).thenReturn(mock(EmployeeRoleResponse.class));

        List<EmployeeRoleResponse> result = service.getAll();

        assertThat(result).hasSize(2);
        verify(repo).findByIsActiveTrueOrderByNameAsc();
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_existente_devuelveResponse() {
        EmployeeRole r = role(1L, "ADMIN", true);
        when(repo.findById(1L)).thenReturn(Optional.of(r));
        when(mapper.toResponse(r)).thenReturn(mock(EmployeeRoleResponse.class));

        service.getById(1L);

        verify(mapper).toResponse(r);
    }

    @Test
    void getById_noEncontrado_lanzaResourceNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_rolNuevo_seGuardaConActivoTrue() {
        EmployeeRoleRequest req = request("SUPERVISOR", "Supervisor de turno");
        EmployeeRole entity = role(null, "SUPERVISOR", false);
        EmployeeRole saved = role(7L, "SUPERVISOR", true);

        when(repo.findByNameIgnoreCase("SUPERVISOR")).thenReturn(Optional.empty());
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repo.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(mock(EmployeeRoleResponse.class));

        service.create(req);

        ArgumentCaptor<EmployeeRole> captor = ArgumentCaptor.forClass(EmployeeRole.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void create_nombreDuplicado_lanzaDuplicateResourceException() {
        when(repo.findByNameIgnoreCase("ADMIN"))
                .thenReturn(Optional.of(role(1L, "ADMIN", true)));

        assertThatThrownBy(() -> service.create(request("ADMIN", "desc")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ADMIN");

        verify(repo, never()).save(any());
    }

    @Test
    void create_nombreDuplicadoCaseInsensitive_lanzaDuplicateResourceException() {
        when(repo.findByNameIgnoreCase("admin"))
                .thenReturn(Optional.of(role(1L, "ADMIN", true)));

        assertThatThrownBy(() -> service.create(request("admin", "desc")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_soloDescripcion_noBuscaDuplicadoDeNombre() {
        EmployeeRole existing = role(1L, "ADMIN", true);
        EmployeeRoleRequest req = request("ADMIN", "Nueva descripción");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(mock(EmployeeRoleResponse.class));

        service.update(1L, req);

        verify(repo, never()).findByNameIgnoreCase(any());
        verify(mapper).updateEntity(req, existing);
    }

    @Test
    void update_cambiaNombre_verificaDuplicadoYActualiza() {
        EmployeeRole existing = role(1L, "ADMIN", true);
        EmployeeRoleRequest req = request("SUPERADMIN", "Super administrador");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByNameIgnoreCase("SUPERADMIN")).thenReturn(Optional.empty());
        when(repo.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(mock(EmployeeRoleResponse.class));

        service.update(1L, req);

        verify(repo).findByNameIgnoreCase("SUPERADMIN");
        verify(mapper).updateEntity(req, existing);
    }

    @Test
    void update_nuevoNombreDuplicado_lanzaDuplicateResourceException() {
        EmployeeRole existing = role(1L, "ADMIN", true);
        EmployeeRoleRequest req = request("MANAGER", "desc");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByNameIgnoreCase("MANAGER"))
                .thenReturn(Optional.of(role(2L, "MANAGER", true)));

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("MANAGER");

        verify(repo, never()).save(any());
    }

    @Test
    void update_noEncontrado_lanzaResourceNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("X", "desc")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deactivate ─────────────────────────────────────────────────────────────

    @Test
    void deactivate_marcaComoInactivo() {
        EmployeeRole entity = role(1L, "WAITER", true);
        when(repo.findById(1L)).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);

        service.deactivate(1L);

        ArgumentCaptor<EmployeeRole> captor = ArgumentCaptor.forClass(EmployeeRole.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void deactivate_noEncontrado_lanzaResourceNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repo, never()).save(any());
    }
}
