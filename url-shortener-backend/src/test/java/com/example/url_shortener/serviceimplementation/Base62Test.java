package com.example.url_shortener.serviceimplementation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Base62Test {

    @Test
    @DisplayName("Should encode zero as '0'")
    void testEncodeZero() {
        assertEquals("0", Base62.encode(0), "Encoding 0 should return '0'");
    }

    @Test
    @DisplayName("Should encode small numbers correctly")
    void testEncodeSmallNumbers() {
        assertEquals("1", Base62.encode(1));
        assertEquals("9", Base62.encode(9));
        assertEquals("A", Base62.encode(10));
        assertEquals("Z", Base62.encode(35));
        assertEquals("a", Base62.encode(36));
        assertEquals("z", Base62.encode(61));
    }

    @Test
    @DisplayName("Should encode larger numbers using Base62 logic")
    void testEncodeLargeNumbers() {
        // 62 in base62 is '10'
        assertEquals("10", Base62.encode(62));
        // 63 in base62 is '11'
        assertEquals("11", Base62.encode(63));
        // 3844 (62^2) should be '100'
        assertEquals("100", Base62.encode(3844));
    }
}
