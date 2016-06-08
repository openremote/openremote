/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test;

import com.google.gwt.place.shared.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Do the same job as the GWT generated code.
 */
public class ClientPlaceHistoryMapper implements PlaceHistoryMapper {

    final protected Map<String, PlaceTokenizer<Place>> byPrefix= new HashMap<>();
    final protected Map<Class, PlaceTokenizer<Place>> byPlaceType = new HashMap<>();
    final protected Map<PlaceTokenizer<Place>, String> toPrefix= new HashMap<>();

    public ClientPlaceHistoryMapper(WithTokenizers withTokenizers) {
        if (withTokenizers == null)
            return;
        for (Class<? extends PlaceTokenizer<?>> tokenizerType : withTokenizers.value()) {
            Prefix prefixAnnotation;
            if ((prefixAnnotation = tokenizerType.getAnnotation(Prefix.class)) != null) {
                String prefix = prefixAnnotation.value();
                Type[] genericInterfaces = tokenizerType.getGenericInterfaces();
                Class placeType = (Class)((ParameterizedType)genericInterfaces[0]).getActualTypeArguments()[0];
                try {
                    @SuppressWarnings("unchecked")
                    PlaceTokenizer<Place> tokenizer = (PlaceTokenizer<Place>) tokenizerType.newInstance();
                    byPrefix.put(prefix, tokenizer);
                    toPrefix.put(tokenizer, prefix);
                    byPlaceType.put(placeType, tokenizer);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }


    @Override
    public Place getPlace(String token) {
        if (token == null)
            return null;
        String prefix;
        String remaining;
        int separatorIndex = token.indexOf(":");
        if (separatorIndex >= 0) {
            prefix = token.substring(0, separatorIndex);
            remaining = token.substring(separatorIndex+1, token.length());
        } else {
            prefix = token;
            remaining = "";
        }
        return byPrefix.containsKey(prefix) ? byPrefix.get(prefix).getPlace(remaining) : null;
    }


    @Override
    public String getToken(Place place) {
        Class placeType = place.getClass();
        if (byPlaceType.containsKey(placeType)) {
            PlaceTokenizer<Place> tokenizer = byPlaceType.get(placeType);
            if (toPrefix.containsKey(tokenizer)) {
                return toPrefix.get(tokenizer) + ":" + tokenizer.getToken(place);
            }
            return null;
        }
        return null;
    }
}
