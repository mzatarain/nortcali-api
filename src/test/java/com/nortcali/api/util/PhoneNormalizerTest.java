package com.nortcali.api.util;

import com.nortcali.api.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "6862279910", "686 227 9910", "(686) 227-9910", "686.227.99.10",
            "+52 686 227 9910", "52 6862279910", "+521 686 227 9910"
    })
    void normalize_variousFormats_returnsTenDigits(String raw) {
        assertThat(PhoneNormalizer.normalize(raw)).isEqualTo("6862279910");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "SIN-TELEFONO", "sin-telefono"})
    void normalize_blankOrPlaceholder_returnsPlaceholder(String raw) {
        assertThat(PhoneNormalizer.normalize(raw)).isEqualTo(PhoneNormalizer.NO_PHONE_PLACEHOLDER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"clara", "686453590", "00000000", "68622799101", "1 686 227 9910"})
    void normalize_invalid_throwsBusinessRule(String raw) {
        assertThatThrownBy(() -> PhoneNormalizer.normalize(raw))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("10 dígitos");
    }

    @Test
    void isPlaceholder_onlyMatchesPlaceholder() {
        assertThat(PhoneNormalizer.isPlaceholder(PhoneNormalizer.NO_PHONE_PLACEHOLDER)).isTrue();
        assertThat(PhoneNormalizer.isPlaceholder("6862279910")).isFalse();
    }
}
