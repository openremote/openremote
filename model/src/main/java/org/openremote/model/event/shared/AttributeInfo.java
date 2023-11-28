package org.openremote.model.event.shared;

import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.value.MetaHolder;
import org.openremote.model.value.NameValueHolder;

public interface AttributeInfo extends AssetInfo, NameValueHolder, MetaHolder {
    long getTimestamp();
    AttributeRef getAttributeRef();
    AttributeState getAttributeState();
}
