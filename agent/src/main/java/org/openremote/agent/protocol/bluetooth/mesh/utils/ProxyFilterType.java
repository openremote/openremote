package org.openremote.agent.protocol.bluetooth.mesh.utils;

/**
 * Wrapper class for proxy filter types
 */
public class ProxyFilterType {

    // @IntDef({INCLUSION_LIST_FILTER, EXCLUSION_LIST_FILTER})
    // public @interface FilterTypes {
    // }

    /**
     * A inclusion list filter has an associated inclusion list, which is a list of destination addresses
     * that are of interest for the Proxy Client. The inclusion list filter blocks all destination addresses
     * except those that have been added to the inclusion list.
     */
    public static final int INCLUSION_LIST_FILTER = 0x00;   //inclusion list filter type

    /**
     * A exclusion list filter has an associated exclusion list, which is a list of destination addresses
     * that the Proxy Client does not want to receive. The exclusion list filter accepts all destination addresses
     * except those that have been added to the exclusion list.
     */
    public static final int EXCLUSION_LIST_FILTER = 0x01;   //The node supports Relay feature that is enabled

    /**
     * Filter type
     */
    private final int filterType;

    /**
     * Constructs the filter type to bet set to a proxy
     *
     * @param filterType filter type supported by the proxy
     */
    public ProxyFilterType(/*@FilterTypes*/ final int filterType) {
        this.filterType = filterType;
    }

    /**
     * Returns the filter type
     */
    /* @FilterTypes */
    public int getType() {
        return filterType;
    }

    /**
     * Returns the filter type name
     */
    public String getFilterTypeName() {
        if (filterType == INCLUSION_LIST_FILTER) {
            return "Inclusion List";
        } else {
            return "Exclusion List";
        }
    }
}
