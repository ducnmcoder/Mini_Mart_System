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
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("DifferentPassword@123");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("confirmPassword"));
    }

    @Test
    void acceptsMatchingNewPasswordWithoutCurrentPassword() {
        ProfileUpdateRequest request = validRequest();
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        ProfileUpdateRequest request = validRequest();
        request.setNewPassword("short");
        request.setConfirmPassword("short");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("newPassword"));
    }

    private ProfileUpdateRequest validRequest() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("Nguyễn Văn A");
        request.setAccountName("nguyenvana");
        request.setPhone("0901234567");
        request.setNewPassword("");
        request.setConfirmPassword("");
        return request;
    }
}
