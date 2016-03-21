package org.openremote.manager.client;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.map.MapPlace;

@WithTokenizers(
    {
        AssetsPlace.Tokenizer.class,
        MapPlace.Tokenizer.class
    }
)
public interface ManagerHistoryMapper extends PlaceHistoryMapper {
}
