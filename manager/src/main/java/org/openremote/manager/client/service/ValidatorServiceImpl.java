package org.openremote.manager.client.service;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.inject.Inject;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.openremote.manager.client.i18n.ManagerMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Richard on 18/02/2016.
 */
public class ValidatorServiceImpl implements ValidatorService {

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
}
