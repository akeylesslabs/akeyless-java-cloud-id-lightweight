package io.akeyless.cloudid;

import io.akeyless.cloudid.util.JsonExtractors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the {@link JsonExtractors} helper, which was previously uncovered.
 */
public class JsonExtractorsTest {

    @Test
    public void extractsQuotedStringValue() {
        String json = "{\"access_token\":\"abc123\",\"token_type\":\"Bearer\"}";
        assertEquals("abc123", JsonExtractors.extractStringField(json, "access_token"));
        assertEquals("Bearer", JsonExtractors.extractStringField(json, "token_type"));
    }

    @Test
    public void toleratesWhitespaceBeforeValue() {
        String json = "{\"access_token\":   \"spaced\"}";
        assertEquals("spaced", JsonExtractors.extractStringField(json, "access_token"));
    }

    @Test
    public void handlesEscapedCharactersInValue() {
        // The extractor unescapes by dropping the backslash and keeping the next char.
        String json = "{\"k\":\"a\\\"b\"}";
        assertEquals("a\"b", JsonExtractors.extractStringField(json, "k"));
    }

    @Test
    public void extractsUnquotedValue() {
        String json = "{\"expires_in\":3600,\"token_type\":\"Bearer\"}";
        assertEquals("3600", JsonExtractors.extractStringField(json, "expires_in"));
    }

    @Test
    public void returnsNullWhenFieldMissing() {
        assertNull(JsonExtractors.extractStringField("{\"other\":\"x\"}", "access_token"));
    }

    @Test
    public void returnsNullForNullInputs() {
        assertNull(JsonExtractors.extractStringField(null, "access_token"));
        assertNull(JsonExtractors.extractStringField("{\"a\":\"b\"}", null));
    }

    @Test
    public void extractsEmptyStringValue() {
        assertEquals("", JsonExtractors.extractStringField("{\"access_token\":\"\"}", "access_token"));
    }
}
