package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

/**
 * Created by Richard on 12/02/2016.
 */
@WithTokenizers(
        {
                AssetsPlace.Tokenizer.class,
                OverviewPlace.Tokenizer.class
        }
)
public interface HistoryMapper extends PlaceHistoryMapper {
}
