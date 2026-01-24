package com.example.csvparser.core.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Collections;
import java.util.Set;

public class BeanValidatorAdapter {
    private final boolean enabled;
    private final Validator validator;

    public BeanValidatorAdapter(boolean enabled) {
        this.enabled = enabled && isValidatorAvailable();
        this.validator = this.enabled ? Validation.buildDefaultValidatorFactory().getValidator() : null;
    }

    public <T> Set<ConstraintViolation<T>> validate(T instance) {
        if (!enabled || validator == null) {
            return Collections.emptySet();
        }
        return validator.validate(instance);
    }

    private boolean isValidatorAvailable() {
        try {
            Class.forName("jakarta.validation.Validation");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
