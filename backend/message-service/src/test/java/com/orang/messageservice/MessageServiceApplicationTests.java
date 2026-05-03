package com.orang.messageservice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageServiceApplicationTests {

    @Test
    void packageStructureSanityCheck() {
        assertEquals("com.orang.messageservice", MessageServiceApplication.class.getPackageName());
    }

}
