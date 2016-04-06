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
