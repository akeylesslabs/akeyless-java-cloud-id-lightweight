package io.akeyless.cloudid;

import io.akeyless.cloudid.aws.AwsIamCloudIdProvider;
import io.akeyless.cloudid.azure.AzureAdCloudIdProvider;
import io.akeyless.cloudid.gcp.GcpCloudIdProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CloudProviderFactoryTest {

    @Test
    public void mapsAccessTypes() {
        CloudIdProvider aws = CloudProviderFactory.getCloudIdProvider("aws_iam");
        assertInstanceOf(AwsIamCloudIdProvider.class, aws);

        CloudIdProvider azure = CloudProviderFactory.getCloudIdProvider("azure_ad");
        assertInstanceOf(AzureAdCloudIdProvider.class, azure);

        CloudIdProvider gcp = CloudProviderFactory.getCloudIdProvider("gcp");
        assertInstanceOf(GcpCloudIdProvider.class, gcp);
    }

    @Test
    public void rejectsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> CloudProviderFactory.getCloudIdProvider("unknown"));
    }

    @Test
    public void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> CloudProviderFactory.getCloudIdProvider(null));
    }

    @Test
    public void normalizesCaseAndWhitespace() {
        assertInstanceOf(AwsIamCloudIdProvider.class, CloudProviderFactory.getCloudIdProvider("  AWS_IAM  "));
        assertInstanceOf(AzureAdCloudIdProvider.class, CloudProviderFactory.getCloudIdProvider("Azure_Ad"));
        assertInstanceOf(GcpCloudIdProvider.class, CloudProviderFactory.getCloudIdProvider("GCP"));
    }
}


