package com.masrouf.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InputValidationTest {
    @Test public void normalizesAndAcceptsOnlyGmailAddresses() {
        assertEquals("amenigharbi@gmail.com",InputValidation.normalizeGmail("  AmeniGharbi@GMAIL.COM  "));
        assertTrue(InputValidation.isStrictGmail("amenigharbi@gmail.com"));
        assertTrue(InputValidation.isStrictGmail("admin.masrouf@gmail.com"));
        assertFalse(InputValidation.isStrictGmail("admin@masrouf.tn"));
        assertFalse(InputValidation.isStrictGmail("contact@yahoo.fr"));
        assertFalse(InputValidation.isStrictGmail("test@gmail"));
        assertFalse(InputValidation.isStrictGmail("user@gmail.com.tn"));
        assertFalse(InputValidation.isStrictGmail("a..b@gmail.com"));
    }

    @Test public void passwordRequiresEveryOneOfTheFiveRules() {
        assertTrue(InputValidation.isValidPassword("Admin@123"));
        assertTrue(InputValidation.isValidPassword("User#1234"));
        assertTrue(InputValidation.isValidPassword("Enfant!123"));
        assertTrue(InputValidation.isValidPassword("Masrouf$2026"));
        assertTrue(InputValidation.isValidPassword("User@1234"));
        assertFalse(InputValidation.isValidPassword("Admin123"));
        assertFalse(InputValidation.isValidPassword("admin@123"));
        assertFalse(InputValidation.isValidPassword("ADMIN@123"));
        assertFalse(InputValidation.isValidPassword("Admin@abc"));
        assertFalse(InputValidation.isValidPassword("Ad@123"));
        assertFalse(InputValidation.isValidPassword("Admin1234"));
    }

    @Test public void passwordRuleFlagsAndAllFiveRegistrationIndicatorsAgree() {
        assertEquals(5,InputValidation.passwordRules("Admin@123").length);
        assertTrue(InputValidation.hasMinimumLength("Admin@123"));
        assertTrue(InputValidation.hasUppercase("Admin@123"));
        assertTrue(InputValidation.hasLowercase("Admin@123"));
        assertTrue(InputValidation.hasNumber("Admin@123"));
        assertTrue(InputValidation.hasSpecialCharacter("Admin@123"));
        assertFalse(InputValidation.hasSpecialCharacter("Admin123"));
        assertFalse(InputValidation.hasSpecialCharacter("Abcdef1ع"));
    }
}
