package com.sharecycle.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OperatorTest {

    @Test
    void testConstructors() {
        User baseUser = new User();
        Operator op1 = new Operator();
        assertEquals("OPERATOR", op1.getRole());

        Operator op2 = new Operator(baseUser);
        assertEquals("OPERATOR", op2.getRole());

        Operator op3 = new Operator("Name", "Addr", "email", "username", "hash", "token");
        assertEquals("OPERATOR", op3.getRole());
        assertEquals("Name", op3.getFullName());
    }
}
