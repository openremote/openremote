package org.openremote.model.query;

import java.io.Serializable;

public class AssetDatapointQuery implements Serializable {

    public long fromTimestamp;
    public long toTimestamp;
    public String assetId;
    public String attributeName;
    public int amountOfPoints;
    public Hyperfunction hyperfunction;

    public AssetDatapointQuery() {
        hyperfunction = new Hyperfunction();
    }


    /* --------------- */

    // Contains the Function type (many of them are Hyperfunctions from TimescaleDB https://docs.timescale.com/api/latest/hyperfunctions/),
    // and includes extra details or metadata that might be required for a specific algorithm
    public static class Hyperfunction {
        public Function function;
    }

    public enum Function {
        LTTB, // default
        GP_LTTB,
        ASAP_SMOOTH
    }
}
