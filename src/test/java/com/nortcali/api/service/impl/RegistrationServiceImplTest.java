package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.RegisterRequest;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.Country;
import com.nortcali.api.entity.LegalAcceptance;
import com.nortcali.api.entity.State;
import com.nortcali.api.entity.Tenant;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.EmployeeRepository;
import com.nortcali.api.repository.LegalAcceptanceRepository;
import com.nortcali.api.repository.MenuCategoryRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.repository.SalesSourceRepository;
import com.nortcali.api.repository.TenantRepository;
import com.nortcali.api.repository.UnitRepository;
import com.nortcali.api.service.EmailService;
import com.nortcali.api.util.LegalDocuments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock private TenantRepository tenantRepo;
    @Mock private RestaurantRepository restaurantRepo;
    @Mock private CityRepository cityRepo;
    @Mock private EmployeeRepository employeeRepo;
    @Mock private MenuCategoryRepository menuCategoryRepo;
    @Mock private UnitRepository unitRepo;
    @Mock private SalesSourceRepository salesSourceRepo;
    @Mock private LegalAcceptanceRepository legalAcceptanceRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private RegistrationServiceImpl service;

    private RegisterRequest request(Boolean acceptLegal) {
        RegisterRequest r = new RegisterRequest();
        r.setBusinessName("Pizzería Prueba");
        r.setRestaurantName("Prueba Centro");
        r.setCityId(1L);
        r.setAdminFirstName("Ana");
        r.setAdminLastName("López");
        r.setAdminEmail("ana@example.com");
        r.setAdminPassword("Secreta123!");
        r.setAcceptLegal(acceptLegal);
        return r;
    }

    @Test
    void register_sinAceptarDocumentosLegales_lanzaExcepcionYNoCreaNada() {
        assertThatThrownBy(() -> service.register(request(false), "1.2.3.4", "UA"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Términos y Condiciones");
        assertThatThrownBy(() -> service.register(request(null), "1.2.3.4", "UA"))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(tenantRepo, employeeRepo, legalAcceptanceRepo);
    }

    @Test
    void register_conAceptacion_guardaEvidenciaConVersionesIpYUserAgent() {
        Country country = new Country();
        State state = new State(1L, "Baja California", "BC", country);
        City city = new City(1L, "Mexicali", state);

        when(employeeRepo.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(cityRepo.findById(1L)).thenReturn(Optional.of(city));
        when(tenantRepo.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });
        when(restaurantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepo.findByTenantIdAndUsername(anyLong(), anyString())).thenReturn(Optional.empty());
        when(employeeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        String longUserAgent = "M".repeat(300);
        service.register(request(true), "201.143.224.227", longUserAgent);

        ArgumentCaptor<LegalAcceptance> captor = ArgumentCaptor.forClass(LegalAcceptance.class);
        verify(legalAcceptanceRepo).save(captor.capture());
        LegalAcceptance saved = captor.getValue();
        assertThat(saved.getTenant().getId()).isEqualTo(42L);
        assertThat(saved.getEmployee().getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getTermsVersion()).isEqualTo(LegalDocuments.TERMS_VERSION);
        assertThat(saved.getPrivacyVersion()).isEqualTo(LegalDocuments.PRIVACY_VERSION);
        assertThat(saved.getIpAddress()).isEqualTo("201.143.224.227");
        assertThat(saved.getUserAgent()).hasSize(255);
        verify(emailService).sendWelcomeEmail("ana@example.com", "Ana", "Pizzería Prueba");
    }

    @Test
    void register_correoDuplicado_noGuardaAceptacion() {
        when(employeeRepo.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(new com.nortcali.api.entity.Employee()));

        assertThatThrownBy(() -> service.register(request(true), "1.2.3.4", "UA"))
                .hasMessageContaining("Ya existe una cuenta");
        verify(legalAcceptanceRepo, never()).save(any());
    }
}
