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
package org.openremote.manager.client.service;

public class ValidatorServiceImpl implements ValidatorService {

    /*
    ManagerMessages messages;

    Validator blankValidator = new Validator() {
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public List<EditorError> validate(Editor editor, Object value) {
            List<EditorError> result = new ArrayList<EditorError>();
            String valueStr = value == null ? "" : value.toString();
            if (valueStr == null || "".equals(valueStr)) {
                result.add(new BasicEditorError(editor, value, messages.fieldBlank()));
            }
            return result;
        }
    };

    @Inject
    public ValidatorServiceImpl(ManagerMessages messages) {
        this.messages = messages;
    }

    @Override
    public Validator getBlankFieldValidator() {
        return blankValidator;
    }
    */
}
