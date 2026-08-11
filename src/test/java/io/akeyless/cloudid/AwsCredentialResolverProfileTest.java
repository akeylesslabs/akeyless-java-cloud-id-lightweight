package io.akeyless.cloudid;

import io.akeyless.cloudid.aws.AwsCredentialResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AwsCredentialResolverProfileTest {
    private File tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("aws-profile-test").toFile();
        File credentials = new File(tempDir, "credentials");
        try (FileWriter fw = new FileWriter(credentials)) {
            fw.write("[default]\n");
            fw.write("aws_access_key_id=TESTKEY_FROM_PROFILE\n");
            fw.write("aws_secret_access_key=TESTSECRET_FROM_PROFILE\n");
            fw.write("aws_session_token=TESTSESSION_FROM_PROFILE\n");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (tempDir != null) deleteRecursively(tempDir);
    }

    @Test
    public void resolvesFromDefaultProfileWhenNoEnv() throws Exception {
        AwsCredentialResolver resolver = new AwsCredentialResolver(
                new io.akeyless.cloudid.http.JdkHttpTransport(),
                "default",
                new File(tempDir, "credentials").getAbsolutePath(),
                null,
                true // ignore env
        );
        AwsCredentialResolver.AwsCredentials creds = resolver.resolve();
        assertEquals("TESTKEY_FROM_PROFILE", creds.accessKeyId);
        assertEquals("TESTSECRET_FROM_PROFILE", creds.secretAccessKey);
        assertEquals("TESTSESSION_FROM_PROFILE", creds.sessionToken);
    }

    @Test
    public void resolvesFromNamedProfile() throws Exception {
        File credentials = new File(tempDir, "credentials");
        try (FileWriter fw = new FileWriter(credentials)) {
            fw.write("[default]\n");
            fw.write("aws_access_key_id=DEFAULT_KEY\n");
            fw.write("aws_secret_access_key=DEFAULT_SECRET\n");
            fw.write("\n[work]\n");
            fw.write("aws_access_key_id=WORK_KEY\n");
            fw.write("aws_secret_access_key=WORK_SECRET\n");
        }
        AwsCredentialResolver resolver = new AwsCredentialResolver(
                new io.akeyless.cloudid.http.JdkHttpTransport(),
                "work",
                credentials.getAbsolutePath(),
                null,
                true);
        AwsCredentialResolver.AwsCredentials creds = resolver.resolve();
        assertEquals("WORK_KEY", creds.accessKeyId);
        assertEquals("WORK_SECRET", creds.secretAccessKey);
        assertNull(creds.sessionToken, "session token should be null when not present in the profile");
    }

    @Test
    public void resolvesFromConfigFileFormatWithProfilePrefix() throws Exception {
        // When there is no credentials file, resolution falls back to the config file,
        // whose non-default sections are named "[profile <name>]".
        File config = new File(tempDir, "config");
        try (FileWriter fw = new FileWriter(config)) {
            fw.write("[profile prod]\n");
            fw.write("aws_access_key_id=PROD_KEY\n");
            fw.write("aws_secret_access_key=PROD_SECRET\n");
            fw.write("aws_security_token=PROD_LEGACY_TOKEN\n");
        }
        AwsCredentialResolver resolver = new AwsCredentialResolver(
                new io.akeyless.cloudid.http.JdkHttpTransport(),
                "prod",
                new File(tempDir, "does-not-exist-credentials").getAbsolutePath(),
                config.getAbsolutePath(),
                true);
        AwsCredentialResolver.AwsCredentials creds = resolver.resolve();
        assertEquals("PROD_KEY", creds.accessKeyId);
        assertEquals("PROD_SECRET", creds.secretAccessKey);
        // The legacy "aws_security_token" key is treated as the session token.
        assertEquals("PROD_LEGACY_TOKEN", creds.sessionToken);
    }

    private static void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        f.delete();
    }

    // No env mutation needed – constructor overrides are used
}


