package com.minimart.validation;

import com.minimart.dto.request.ProfileUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUpdateRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsBlankName() {
        ProfileUpdateRequest request = validRequest();
        request.setFullName("   ");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    void acceptsEmptyPhoneBecauseItIsOptional() {
        ProfileUpdateRequest request = validRequest();
        request.setPhone("");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidPhone() {
        ProfileUpdateRequest request = validRequest();
        request.setPhone("not-a-phone");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void rejectsMismatchedPasswordConfirmation() {
        ProfileUpdateRequest request = validRequest();
        request.setCurrentPassword("MiniMart@123");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("DifferentPassword@123");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("confirmPassword"));
    }

    private ProfileUpdateRequest validRequest() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("Nguyễn Văn A");
        request.setPhone("0901234567");
        request.setCurrentPassword("");
        request.setNewPassword("");
        request.setConfirmPassword("");
        return request;
    }
}
