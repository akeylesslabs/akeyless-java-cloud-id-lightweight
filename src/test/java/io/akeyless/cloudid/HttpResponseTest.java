package io.akeyless.cloudid;

import io.akeyless.cloudid.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpResponseTest {

    @Test
    public void exposesStatusAndBody() {
        HttpResponse res = new HttpResponse(200, "hello",
                Collections.singletonMap("Content-Type", Collections.singletonList("text/plain")));
        assertEquals(200, res.getStatusCode());
        assertEquals("hello", res.getBody());
        assertEquals("text/plain", res.getHeaders().get("Content-Type").get(0));
    }

    @Test
    public void nullHeadersBecomeEmptyMap() {
        HttpResponse res = new HttpResponse(204, null, null);
        Map<String, List<String>> headers = res.getHeaders();
        assertNotNull(headers, "headers must never be null");
        assertTrue(headers.isEmpty(), "null headers must be normalized to an empty map");
    }
}
