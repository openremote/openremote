package org.openremote.manager.shared.ngsi;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * http://telefonicaid.github.io/fiware-orion/api/v2/
 */
public class Model {

    public static char[] FIELD_CHARS = new char[]{
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '%', '!', '"', '$', '\'', '`', '(', ')', '*', '+', '.', ',', '-', ':', ';', '<', '=', '>', '@', '[', '\\', ']', '^', '_', '{', '|', '}', '~'
    };

    public static final int FIELD_MAX_LENGTH = 256;

    static {
        Arrays.sort(FIELD_CHARS);
    }

    public static ModelProblem[] validateField(String value) {
        Set<ModelProblem> problems = new LinkedHashSet<>();
        if (value == null || value.length() ==0) {
            problems.add(ModelProblem.FIELD_EMPTY);
        } else {
            char[] chars = value.toCharArray();
            if (chars.length > FIELD_MAX_LENGTH) {
                problems.add(ModelProblem.FIELD_TOO_LONG);

            }
            for (char c : chars) {
                if (Arrays.binarySearch(FIELD_CHARS, c) < 0) {
                    problems.add(ModelProblem.FIELD_INVALID_CHARS);
                    break;
                }
            }
        }
        return problems.toArray(new ModelProblem[problems.size()]);
    }
}
