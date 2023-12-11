package org.openremote.model.attribute;

import org.openremote.model.asset.AssetInfo;
import org.openremote.model.value.MetaHolder;
import org.openremote.model.value.NameValueHolder;

import java.util.Optional;

public interface AttributeInfo extends AssetInfo, NameValueHolder<Object>, MetaHolder, Comparable<AttributeInfo> {
    long getTimestamp();
    AttributeRef getRef();
    String getId();
    String getName();
    AttributeState getState();

    Optional<Object> getOldValue();

    long getOldValueTimestamp();
}
