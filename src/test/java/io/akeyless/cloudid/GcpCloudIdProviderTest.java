package io.akeyless.cloudid;

import io.akeyless.cloudid.gcp.GcpCloudIdProvider;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GcpCloudIdProviderTest {

    private static HttpTransport transportReturning(int status, String body) throws Exception {
        HttpTransport http = Mockito.mock(HttpTransport.class);
        Mockito.when(http.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new HttpResponse(status, body, Collections.<String, java.util.List<String>>emptyMap()));
        return http;
    }

    @Test
    public void returnsIdentityToken() throws Exception {
        GcpCloudIdProvider provider = new GcpCloudIdProvider(transportReturning(200, "jwt-token"));
        String cloudId = provider.getCloudId();
        String expected = Base64.getEncoder().encodeToString("jwt-token".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, cloudId);
    }

    @Test
    public void throwsOnNon2xxStatus() throws Exception {
        GcpCloudIdProvider provider = new GcpCloudIdProvider(transportReturning(403, "denied"));
        assertThrows(IOException.class, provider::getCloudId);
    }

    @Test
    public void throwsWhenBodyEmpty() throws Exception {
        GcpCloudIdProvider provider = new GcpCloudIdProvider(transportReturning(200, ""));
        assertThrows(IOException.class, provider::getCloudId);
    }
}


