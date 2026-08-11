package io.akeyless.cloudid;

import io.akeyless.cloudid.azure.AzureAdCloudIdProvider;
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

public class AzureAdCloudIdProviderTest {

    private static HttpTransport transportReturning(int status, String body) throws Exception {
        HttpTransport http = Mockito.mock(HttpTransport.class);
        Mockito.when(http.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new HttpResponse(status, body, Collections.<String, java.util.List<String>>emptyMap()));
        return http;
    }

    @Test
    public void returnsAccessToken() throws Exception {
        HttpTransport http = Mockito.mock(HttpTransport.class);
        String url = "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=https%3A%2F%2Fmanagement.azure.com%2F";
        String body = "{\"access_token\":\"abc123\",\"token_type\":\"Bearer\"}";
        Mockito.when(http.get(Mockito.eq(url), Mockito.anyMap(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new HttpResponse(200, body, Collections.<String, java.util.List<String>>emptyMap()));

        AzureAdCloudIdProvider provider = new AzureAdCloudIdProvider(http);
        String cloudId = provider.getCloudId();
        String expected = Base64.getEncoder().encodeToString("abc123".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, cloudId);
    }

    @Test
    public void throwsOnNon2xxStatus() throws Exception {
        AzureAdCloudIdProvider provider = new AzureAdCloudIdProvider(transportReturning(500, "boom"));
        assertThrows(IOException.class, provider::getCloudId);
    }

    @Test
    public void throwsWhenAccessTokenMissing() throws Exception {
        AzureAdCloudIdProvider provider = new AzureAdCloudIdProvider(
                transportReturning(200, "{\"token_type\":\"Bearer\"}"));
        assertThrows(IOException.class, provider::getCloudId);
    }

    @Test
    public void throwsWhenAccessTokenEmpty() throws Exception {
        AzureAdCloudIdProvider provider = new AzureAdCloudIdProvider(
                transportReturning(200, "{\"access_token\":\"\"}"));
        assertThrows(IOException.class, provider::getCloudId);
    }
}


