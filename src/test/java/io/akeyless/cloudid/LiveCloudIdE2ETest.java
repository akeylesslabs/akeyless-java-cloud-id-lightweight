package io.akeyless.cloudid;

import com.fasterxml.jackson.jr.ob.JSON;
import io.akeyless.cloudid.aws.AwsIamCloudIdProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Credentials-gated, live-cloud end-to-end tests.
 *
 * <p>These are NOT hermetic: they exercise the real credential resolvers and, for Azure/GCP,
 * the real instance-metadata endpoints. They are therefore SKIPPED unless the appropriate
 * environment is detected, so they never fail on a developer laptop or in generic CI.
 *
 * <ul>
 *   <li>AWS  - runs when {@code AWS_ACCESS_KEY_ID} and {@code AWS_SECRET_ACCESS_KEY} are set.</li>
 *   <li>Azure - runs only when {@code AKEYLESS_E2E_AZURE=true} (i.e. on an Azure VM with MI).</li>
 *   <li>GCP  - runs only when {@code AKEYLESS_E2E_GCP=true} (i.e. on a GCP VM).</li>
 * </ul>
 */
public class LiveCloudIdE2ETest {

    private static boolean isSet(String name) {
        String v = System.getenv(name);
        return v != null && !v.isEmpty();
    }

    private static boolean isTrue(String name) {
        return "true".equalsIgnoreCase(System.getenv(name));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void awsLiveEndToEnd() throws Exception {
        assumeTrue(isSet("AWS_ACCESS_KEY_ID") && isSet("AWS_SECRET_ACCESS_KEY"),
                "Skipping AWS live e2e: real AWS credentials not present in the environment");

        String cloudId = new AwsIamCloudIdProvider().getCloudId();
        assertNotNull(cloudId);

        String json = new String(Base64.getDecoder().decode(cloudId), StandardCharsets.UTF_8);
        Map<String, Object> root = (Map<String, Object>) JSON.std.mapFrom(json);
        assertEquals("POST", root.get("sts_request_method"));
        assertNotNull(root.get("sts_request_url"));
        assertNotNull(root.get("sts_request_body"));
        assertNotNull(root.get("sts_request_headers"));
    }

    @Test
    public void azureLiveEndToEnd() throws Exception {
        assumeTrue(isTrue("AKEYLESS_E2E_AZURE"),
                "Skipping Azure live e2e: set AKEYLESS_E2E_AZURE=true on an Azure VM with a managed identity");
        String cloudId = CloudProviderFactory.getCloudIdProvider("azure_ad").getCloudId();
        assertNotNull(cloudId);
    }

    @Test
    public void gcpLiveEndToEnd() throws Exception {
        assumeTrue(isTrue("AKEYLESS_E2E_GCP"),
                "Skipping GCP live e2e: set AKEYLESS_E2E_GCP=true on a GCP VM");
        String cloudId = CloudProviderFactory.getCloudIdProvider("gcp").getCloudId();
        assertNotNull(cloudId);
    }
}
