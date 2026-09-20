package com.snhu.inventoryapp.security;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PasswordPolicyTest {

    @Test
    public void acceptsPasswordMeetingAllRules() {
        assertEquals(PasswordPolicy.Result.OK, PasswordPolicy.check("abcdefg1".toCharArray()));
        assertEquals(PasswordPolicy.Result.OK, PasswordPolicy.check("Correct Horse 42".toCharArray()));
    }

    @Test
    public void rejectsNullAndShort() {
        assertEquals(PasswordPolicy.Result.TOO_SHORT, PasswordPolicy.check(null));
        assertEquals(PasswordPolicy.Result.TOO_SHORT, PasswordPolicy.check("".toCharArray()));
        assertEquals(PasswordPolicy.Result.TOO_SHORT, PasswordPolicy.check("abc1234".toCharArray()));
    }

    @Test
    public void rejectsOverLong() {
        char[] huge = new char[PasswordPolicy.MAX_LENGTH + 1];
        java.util.Arrays.fill(huge, 'a');
        huge[0] = '1';
        assertEquals(PasswordPolicy.Result.TOO_LONG, PasswordPolicy.check(huge));
    }

    @Test
    public void boundaryLengthsAreAccepted() {
        char[] min = "abcdefg1".toCharArray();
        assertEquals(PasswordPolicy.MIN_LENGTH, min.length);
        assertEquals(PasswordPolicy.Result.OK, PasswordPolicy.check(min));

        char[] max = new char[PasswordPolicy.MAX_LENGTH];
        java.util.Arrays.fill(max, 'z');
        max[max.length - 1] = '7';
        assertEquals(PasswordPolicy.Result.OK, PasswordPolicy.check(max));
    }

    @Test
    public void requiresLetterAndDigit() {
        assertEquals(PasswordPolicy.Result.NO_LETTER, PasswordPolicy.check("12345678".toCharArray()));
        assertEquals(PasswordPolicy.Result.NO_DIGIT, PasswordPolicy.check("abcdefgh".toCharArray()));
        assertEquals(PasswordPolicy.Result.NO_LETTER, PasswordPolicy.check("!@#$%^&*1".toCharArray()));
    }
}
