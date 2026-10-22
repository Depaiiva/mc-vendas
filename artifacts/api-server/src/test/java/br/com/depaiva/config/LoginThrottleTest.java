package br.com.depaiva.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class LoginThrottleTest {
    @Test
    void blocksTheFifthFailureForForwardedClientIpAndClearsOnSuccess() {
        LoginThrottle throttle = new LoginThrottle();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.4");
        request.addHeader("X-Forwarded-For", "198.51.100.27, 10.0.0.4");

        for (int attempt = 1; attempt < 5; attempt++) {
            assertFalse(throttle.recordFailure(request));
        }
        assertTrue(throttle.recordFailure(request));
        assertTrue(throttle.isBlocked(request));

        throttle.clear(request);
        assertFalse(throttle.isBlocked(request));
    }
}
