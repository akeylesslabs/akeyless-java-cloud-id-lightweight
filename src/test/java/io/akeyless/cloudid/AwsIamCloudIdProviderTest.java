package io.akeyless.cloudid;

import com.fasterxml.jackson.jr.ob.JSON;
import io.akeyless.cloudid.aws.AwsCredentialResolver;
import io.akeyless.cloudid.aws.AwsIamCloudIdProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AwsIamCloudIdProviderTest {

    private static AwsCredentialResolver fixedResolver(final AwsCredentialResolver.AwsCredentials creds) {
        return new AwsCredentialResolver() {
            @Override
            public AwsCredentials resolve() {
                return creds;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> decodeHeaders(String cloudId) throws Exception {
        String json = new String(Base64.getDecoder().decode(cloudId), StandardCharsets.UTF_8);
        Map<String, Object> root = (Map<String, Object>) JSON.std.mapFrom(json);
        String headersJson = new String(
                Base64.getDecoder().decode((String) root.get("sts_request_headers")), StandardCharsets.UTF_8);
        return (Map<String, Object>) JSON.std.mapFrom(headersJson);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildsBase64EncodedStsRequestBundle() throws Exception {
        AwsCredentialResolver.AwsCredentials creds = new AwsCredentialResolver.AwsCredentials(
                "AKIAEXAMPLE", "SECRETKEYEXAMPLE", "SESSIONTOKEN");
        AwsCredentialResolver resolver = new AwsCredentialResolver() {
            @Override
            public AwsCredentials resolve() {
                return creds;
            }
        };

        AwsIamCloudIdProvider provider = new AwsIamCloudIdProvider(resolver);
        String cloudId = provider.getCloudId();
        assertNotNull(cloudId);

        String json = new String(Base64.getDecoder().decode(cloudId), StandardCharsets.UTF_8);
        Map<String, Object> root = (Map<String, Object>) JSON.std.mapFrom(json);

        assertEquals("POST", root.get("sts_request_method"));
        String url = new String(Base64.getDecoder().decode((String) root.get("sts_request_url")), StandardCharsets.UTF_8);
        assertEquals("https://sts.amazonaws.com/", url);
        String body = new String(Base64.getDecoder().decode((String) root.get("sts_request_body")), StandardCharsets.UTF_8);
        assertEquals("Action=GetCallerIdentity&Version=2011-06-15", body);
        String headersJson = new String(Base64.getDecoder().decode((String) root.get("sts_request_headers")), StandardCharsets.UTF_8);
        Map<String, Object> headers = (Map<String, Object>) JSON.std.mapFrom(headersJson);
        // Basic sanity checks
        assertNotNull(headers.get("Content-Type"));
        assertNotNull(headers.get("Host"));
        assertNotNull(headers.get("X-Amz-Date"));
        assertNotNull(headers.get("X-Amz-Security-Token"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void authorizationHeaderIsSigV4AndDateIsWellFormed() throws Exception {
        AwsCredentialResolver.AwsCredentials creds = new AwsCredentialResolver.AwsCredentials(
                "AKIAEXAMPLE", "SECRETKEYEXAMPLE", "SESSIONTOKEN");
        AwsIamCloudIdProvider provider = new AwsIamCloudIdProvider(fixedResolver(creds));

        Map<String, Object> headers = decodeHeaders(provider.getCloudId());

        // Header values are serialized as JSON arrays (List<String>); take the first element.
        String authorization = ((java.util.List<String>) headers.get("Authorization")).get(0);
        assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 "),
                "Authorization must use the SigV4 algorithm prefix, got: " + authorization);
        assertTrue(authorization.contains("Credential=AKIAEXAMPLE/"),
                "Authorization must embed the access key in the credential scope");
        assertTrue(authorization.contains("/us-east-1/sts/aws4_request"),
                "Authorization must reference the sts/us-east-1 credential scope");
        assertTrue(authorization.contains("SignedHeaders="), "Authorization must list SignedHeaders");
        assertTrue(authorization.matches(".*Signature=[0-9a-f]{64}$"),
                "Authorization must end with a 64-char lowercase hex HMAC-SHA256 signature");
        // When a session token is present it must be part of the signed headers.
        assertTrue(authorization.contains("x-amz-security-token"),
                "SignedHeaders must include x-amz-security-token when a session token is set");

        String amzDate = ((java.util.List<String>) headers.get("X-Amz-Date")).get(0);
        assertTrue(amzDate.matches("^\\d{8}T\\d{6}Z$"),
                "X-Amz-Date must be a SigV4 basic-format timestamp, got: " + amzDate);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void omitsSecurityTokenWhenNoSessionToken() throws Exception {
        AwsCredentialResolver.AwsCredentials creds = new AwsCredentialResolver.AwsCredentials(
                "AKIAEXAMPLE", "SECRETKEYEXAMPLE", null);
        AwsIamCloudIdProvider provider = new AwsIamCloudIdProvider(fixedResolver(creds));

        Map<String, Object> headers = decodeHeaders(provider.getCloudId());
        assertNull(headers.get("X-Amz-Security-Token"),
                "X-Amz-Security-Token must be absent when there is no session token");

        String authorization = ((java.util.List<String>) headers.get("Authorization")).get(0);
        assertFalse(authorization.contains("x-amz-security-token"),
                "SignedHeaders must not include x-amz-security-token without a session token");
    }

    @Test
    public void throwsWhenAccessKeyMissing() {
        AwsCredentialResolver.AwsCredentials creds = new AwsCredentialResolver.AwsCredentials(
                null, "SECRETKEYEXAMPLE", null);
        AwsIamCloudIdProvider provider = new AwsIamCloudIdProvider(fixedResolver(creds));
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::getCloudId);
        assertEquals("Missing AWS credentials", ex.getMessage());
    }

    @Test
    public void throwsWhenSecretKeyMissing() {
        AwsCredentialResolver.AwsCredentials creds = new AwsCredentialResolver.AwsCredentials(
                "AKIAEXAMPLE", null, null);
        AwsIamCloudIdProvider provider = new AwsIamCloudIdProvider(fixedResolver(creds));
        assertThrows(IllegalStateException.class, provider::getCloudId);
    }
}


