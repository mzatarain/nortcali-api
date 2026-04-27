package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.UnitRequest;
import com.nortcali.api.dto.response.UnitResponse;
import com.nortcali.api.entity.Unit;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.UnitMapper;
import com.nortcali.api.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class UnitServiceImplTest {

    @Mock UnitRepository repo;
    @Mock UnitMapper mapper;

    @InjectMocks
    UnitServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private Unit unit(Long id, String name, String abbreviation) {
        Unit u = new Unit();
        u.setId(id);
        u.setName(name);
        u.setAbbreviation(abbreviation);
        return u;
    }

    private UnitRequest request(String name, String abbreviation) {
        UnitRequest r = new UnitRequest();
        r.setName(name);
        r.setAbbreviation(abbreviation);
        return r;
    }

    // ── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_devuelveTodas() {
        Unit u1 = unit(1L, "Kilogramo", "kg");
        Unit u2 = unit(2L, "Litro", "l");
        when(repo.findAll()).thenReturn(List.of(u1, u2));
        when(mapper.toResponse(u1)).thenReturn(mock(UnitResponse.class));
        when(mapper.toResponse(u2)).thenReturn(mock(UnitResponse.class));

        List<UnitResponse> result = service.getAll();

        assertThat(result).hasSize(2);
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_unidadExistente_devuelveResponse() {
        Unit u = unit(1L, "Kilogramo", "kg");
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(mapper.toResponse(u)).thenReturn(mock(UnitResponse.class));

        service.getById(1L);

        verify(mapper).toResponse(u);
    }

    @Test
    void getById_noEncontrada_lanzaResourceNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_unidadNueva_seGuarda() {
        UnitRequest req = request("Gramo", "g");
        Unit entity = unit(null, "Gramo", "g");
        Unit saved = unit(3L, "Gramo", "g");

        when(repo.findByNameIgnoreCase("Gramo")).thenReturn(Optional.empty());
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repo.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(mock(UnitResponse.class));

        service.create(req);

        verify(repo).save(entity);
    }

    @Test
    void create_nombreDuplicado_lanzaDuplicateResourceException() {
        when(repo.findByNameIgnoreCase("Kilogramo"))
                .thenReturn(Optional.of(unit(1L, "Kilogramo", "kg")));

        assertThatThrownBy(() -> service.create(request("Kilogramo", "kg")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Kilogramo");

        verify(repo, never()).save(any());
    }

    @Test
    void create_nombreDuplicadoCaseInsensitive_lanzaDuplicateResourceException() {
        when(repo.findByNameIgnoreCase("kilogramo"))
                .thenReturn(Optional.of(unit(1L, "Kilogramo", "kg")));

        assertThatThrownBy(() -> service.create(request("kilogramo", "kg")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_soloAbreviatura_noBuscaDuplicadoDeNombre() {
        Unit existing = unit(1L, "Kilogramo", "kg");
        UnitRequest req = request("Kilogramo", "KG"); // mismo nombre, distinta abreviatura

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(mock(UnitResponse.class));

        service.update(1L, req);

        verify(repo, never()).findByNameIgnoreCase(any());
        verify(mapper).updateEntity(req, existing);
    }

    @Test
    void update_cambiaNombre_verificaDuplicadoYActualiza() {
        Unit existing = unit(1L, "Kilogramo", "kg");
        UnitRequest req = request("Gramo", "g");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByNameIgnoreCase("Gramo")).thenReturn(Optional.empty());
        when(repo.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(mock(UnitResponse.class));

        service.update(1L, req);

        verify(repo).findByNameIgnoreCase("Gramo");
        verify(mapper).updateEntity(req, existing);
    }

    @Test
    void update_nuevoNombreDuplicado_lanzaDuplicateResourceException() {
        Unit existing = unit(1L, "Kilogramo", "kg");
        UnitRequest req = request("Litro", "l");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByNameIgnoreCase("Litro"))
                .thenReturn(Optional.of(unit(2L, "Litro", "l")));

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Litro");

        verify(repo, never()).save(any());
    }

    @Test
    void update_unidadNoEncontrada_lanzaResourceNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("X", "x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── sin DELETE: las unidades son referenciadas por insumos y recetas ───────

    @Test
    void noExisteMetodoDeactivate_lasUnidadesNoSeBorran() {
        // Las unidades no tienen is_active; eliminarlas rompería FKs en supplies
        // y recipe_ingredients. Por diseño solo hay GET, POST y PUT.
        assertThat(UnitServiceImpl.class.getMethods())
                .extracting("name")
                .doesNotContain("deactivate", "delete");
    }
}
