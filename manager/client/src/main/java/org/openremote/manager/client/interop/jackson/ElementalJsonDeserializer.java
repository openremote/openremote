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
package org.openremote.manager.client.interop.jackson;

import com.github.nmorel.gwtjackson.client.JsonDeserializationContext;
import com.github.nmorel.gwtjackson.client.JsonDeserializer;
import com.github.nmorel.gwtjackson.client.JsonDeserializerParameters;
import com.github.nmorel.gwtjackson.client.stream.JsonToken;
import com.google.gwt.core.client.JsArrayInteger;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * This can only (safely) deserialize JSON Objects and Arrays, not primitives.
 */
public class ElementalJsonDeserializer extends JsonDeserializer<JsonValue> {

    @Override
    protected JsonValue doDeserialize(com.github.nmorel.gwtjackson.client.stream.JsonReader reader, JsonDeserializationContext ctx, JsonDeserializerParameters params) {
        //  TODO: Reinstate reader.nextJavaScriptObject once GWT Jackson bug is resolved https://github.com/nmorel/gwt-jackson/issues/106
        if (reader.hasNext()) {
//            String jsonStr = reader.getInput();
//
//            // Determine where in the string the token is (pos is private unfortunately)
//            int lineNumber = reader.getLineNumber();
//            int columnNumber = reader.getColumnNumber();
//            int startPos = 0;
//            while (lineNumber > 1) {
//                startPos = jsonStr.indexOf('\n', startPos+1);
//                lineNumber--;
//            }
//            startPos += columnNumber-1;
//
//            // Use the reader to find the end of the current token
//            reader.skipValue();
//            lineNumber = reader.getLineNumber();
//            columnNumber = reader.getColumnNumber();
//            int endPos = 0;
//            while (lineNumber > 1) {
//                endPos = jsonStr.indexOf('\n', endPos+1);
//                lineNumber--;
//            }
//            endPos += columnNumber;
//
//            jsonStr = jsonStr.substring(startPos, endPos);
//            return Json.instance().parse(jsonStr);

//            JsonToken token = reader.peek();
//            if (token == JsonToken.BEGIN_ARRAY || token == JsonToken.BEGIN_OBJECT) {
//                return reader.nextJavaScriptObject(ctx.isUseSafeEval()).cast();
//            }

            return Json.instance().parse(reader.nextValue());
        }
        return null;
    }
}
