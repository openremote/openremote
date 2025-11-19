package org.openremote.model.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UniqueIdentifierGeneratorTest {

    @Test
    public void generatedAssetIdHasExpectedLength() {
        assertEquals(22, UniqueIdentifierGenerator.generateId("masterlight-1-1").length(), "Asset ids must be 22 characters long");
    }
}
