package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.SalesSourceRequest;
import com.nortcali.api.dto.response.SalesSourceResponse;
import com.nortcali.api.entity.SalesSource;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.SalesSourceMapper;
import com.nortcali.api.repository.SalesSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesSourceServiceImplTest {

    @Mock SalesSourceRepository repo;
    @Mock SalesSourceMapper mapper;

    @InjectMocks
    SalesSourceServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private SalesSource source(Long id, String name, BigDecimal pct) {
        SalesSource s = new SalesSource();
        s.setId(id);
        s.setName(name);
        s.setCommissionPct(pct);
        s.setActive(true);
        return s;
    }

    private SalesSourceRequest request(String name, BigDecimal pct) {
        SalesSourceRequest r = new SalesSourceRequest();
        r.setName(name);
        r.setCommissionPct(pct);
        return r;
    }

    // ── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_devuelveSoloActivas() {
        SalesSource s1 = source(1L, "Rappi", new BigDecimal("15.00"));
        SalesSource s2 = source(2L, "Uber Eats", new BigDecimal("20.00"));
        when(repo.findByIsActiveTrue()).thenReturn(List.of(s1, s2));
        when(mapper.toResponse(s1)).thenReturn(mock(SalesSourceResponse.class));
        when(mapper.toResponse(s2)).thenReturn(mock(SalesSourceResponse.class));

        List<SalesSourceResponse> result = service.getAll();

        assertThat(result).hasSize(2);
        verify(repo).findByIsActiveTrue();
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_fuenteNueva_seGuardaConActivoTrue() {
        SalesSourceRequest req = request("Didi Food", new BigDecimal("12.00"));
        SalesSource entity = source(null, "Didi Food", new BigDecimal("12.00"));
        SalesSource saved = source(3L, "Didi Food", new BigDecimal("12.00"));

        when(repo.findByNameIgnoreCase("Didi Food")).thenReturn(Optional.empty());
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repo.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(mock(SalesSourceResponse.class));

        service.create(req);

        ArgumentCaptor<SalesSource> captor = ArgumentCaptor.forClass(SalesSource.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void create_nombreDuplicado_lanzaDuplicateResourceException() {
        SalesSourceRequest req = request("Rappi", new BigDecimal("15.00"));
        when(repo.findByNameIgnoreCase("Rappi")).thenReturn(Optional.of(source(1L, "Rappi", new BigDecimal("15.00"))));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Rappi");

        verify(repo, never()).save(any());
    }

    @Test
    void create_nombreDuplicadoCaseInsensitive_lanzaDuplicateResourceException() {
        SalesSourceRequest req = request("rappi", new BigDecimal("15.00"));
        when(repo.findByNameIgnoreCase("rappi")).thenReturn(Optional.of(source(1L, "Rappi", new BigDecimal("15.00"))));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_cambiaComision_sinDuplicadoDeNombre() {
        SalesSource existing = source(1L, "Rappi", new BigDecimal("15.00"));
        SalesSourceRequest req = request("Rappi", new BigDecimal("18.00")); // mismo nombre, nueva comisión

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        // No se llama a findByNameIgnoreCase porque el nombre no cambió
        when(repo.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(mock(SalesSourceResponse.class));

        service.update(1L, req);

        verify(repo, never()).findByNameIgnoreCase(any());
        verify(repo).save(existing);
    }

    @Test
    void update_cambiaNombre_verificaDuplicadoYActualiza() {
        SalesSource existing = source(1L, "Rappi", new BigDecimal("15.00"));
        SalesSourceRequest req = request("Didi Food", new BigDecimal("12.00"));

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByNameIgnoreCase("Didi Food")).thenReturn(Optional.empty());
        when(repo.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(mock(SalesSourceResponse.class));

        service.update(1L, req);

        verify(repo).findByNameIgnoreCase("Didi Food");
        verify(mapper).updateEntity(req, existing);
    }

    @Test
    void update_nuevoNombreDuplicado_lanzaDuplicateResourceException() {
        SalesSource existing = source(1L, "Rappi", new BigDecimal("15.00"));
        SalesSourceRequest req = request("Uber Eats", new BigDecimal("20.00"));

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByNameIgnoreCase("Uber Eats"))
                .thenReturn(Optional.of(source(2L, "Uber Eats", new BigDecimal("20.00"))));

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Uber Eats");

        verify(repo, never()).save(any());
    }

    @Test
    void update_fuenteNoEncontrada_lanzaResourceNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("X", BigDecimal.ONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deactivate ─────────────────────────────────────────────────────────────

    @Test
    void deactivate_marcaComoInactiva() {
        SalesSource entity = source(1L, "Rappi", new BigDecimal("15.00"));
        when(repo.findById(1L)).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);

        service.deactivate(1L);

        ArgumentCaptor<SalesSource> captor = ArgumentCaptor.forClass(SalesSource.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void deactivate_fuenteNoEncontrada_lanzaResourceNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repo, never()).save(any());
    }
}
