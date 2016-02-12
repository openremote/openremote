package org.openremote.manager.client;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import org.openremote.manager.client.presenter.AssetsPlace;
import org.openremote.manager.client.presenter.LoginPlace;
import org.openremote.manager.client.presenter.MapPlace;

/**
 * Created by Richard on 12/02/2016.
 */
@WithTokenizers(
        {
                AssetsPlace.Tokenizer.class,
                MapPlace.Tokenizer.class,
                LoginPlace.Tokenizer.class
        }
)
public interface HistoryMapper extends PlaceHistoryMapper {
}
