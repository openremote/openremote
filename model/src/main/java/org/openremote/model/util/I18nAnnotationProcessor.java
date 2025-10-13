/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.model.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.openremote.model.util.JSONSchemaUtil.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static javax.tools.Diagnostic.Kind.*;

public class I18nAnnotationProcessor extends AbstractProcessor {

    private final ObjectMapper mapper = new ObjectMapper();
    // LinkedHashMap ensures consistent ordering
    private final Map<String, String> translations = new LinkedHashMap<>();

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of("shared");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
            JsonSchemaTitle.class.getCanonicalName(),
            JsonSchemaDescription.class.getCanonicalName()
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        collectI18nKeys(JsonSchemaTitle.class, roundEnv);
        collectI18nKeys(JsonSchemaDescription.class, roundEnv);

        if (roundEnv.processingOver()) {
            writeKeys();
        }

        return false;
    }

    private <A extends Annotation> void collectI18nKeys(Class<A> annotationClass, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotationClass)) {
            try {
                A ann = element.getAnnotation(annotationClass);
                boolean translate = (boolean) annotationClass.getMethod("i18n").invoke(ann);
                if (!translate) continue;

                Optional<String> i18nSuffix = Arrays.stream(annotationClass.getMethods())
                        .filter(m -> m.getName().equals("i18nSuffix"))
                        .map(m -> (String)m.getDefaultValue())
                        .findFirst();
                String keyword = (String) annotationClass.getMethod("keyword").invoke(ann);
                String key = resolveKey(element, i18nSuffix.orElse(keyword));
                String value = (String) annotationClass.getMethod("value").invoke(ann);

                translations.put(key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to process i18n annotation " + annotationClass.getSimpleName(), e);
            }
        }
    }

    private String resolveKey(Element element, String suffix) {
        if (element.getKind().isClass()) {
            return ((TypeElement) element).getQualifiedName().toString() + "." + suffix;
        } else {
            String className = ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString();
            return className + "." + element.getSimpleName().toString() + "." + suffix;
        }
    }

    private void writeKeys() {
        Path locales = Paths.get(processingEnv.getOptions().get("shared")).resolve("locales");
        File[] dirs = locales.toFile().listFiles();

        assert dirs != null;
        for (File d : dirs) appendKeys(d.toPath().resolve("or.json"), d.getName().equals("en"));
    }

    private void appendKeys(Path path, boolean writeValue) {
        ObjectNode existing;

        if (!Files.exists(path)) {
            processingEnv.getMessager().printMessage(WARNING, "Missing translation file");
            return;
        }

        try {
            existing = mapper.readValue(path.toFile(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int conflicts = 0;

        nextTranslation:
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String key = entry.getKey();
            String[] keys = key.split("\\.");
            int zeroIndexLength = keys.length-1;

            ObjectNode node = existing;

            for (int i = 0; i < zeroIndexLength; i++) {
                ObjectNode newNode = mapper.createObjectNode();
                JsonNode oldNode = node.putIfAbsent(keys[i], newNode);
                if (oldNode instanceof ObjectNode obj) {
                    node = obj;
                } else if (oldNode == null) {
                    node = newNode;
                } else {
                    processingEnv.getMessager().printMessage(ERROR, "Translation key conflict for: "+key);
                    conflicts++;
                    continue nextTranslation;
                }
            }
            node.putIfAbsent(keys[zeroIndexLength], new TextNode(writeValue ? entry.getValue() : ""));
        }

        if (conflicts > 0) {
            processingEnv.getMessager().printMessage(ERROR,
                    conflicts + " translation key conflict(s) detected. Please remove the above keys or the conflicting annotations."
            );
            throw new RuntimeException("Key conflict");
        }

        try {
            String updatedJsonString = buildJsonWithoutSpaces(existing, 0) + "\n";
            Files.write(path, updatedJsonString.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Hacky solution to get around weird Jackson formatting which adds spaces between field names and colons
    private static String buildJsonWithoutSpaces(JsonNode jsonNode, int indentLevel) {
        StringBuilder jsonBuilder = new StringBuilder();
        String indent = "  ".repeat(indentLevel); // Create indentation spaces

        // Close and return the object if empty
        if (!jsonNode.properties().iterator().hasNext()) {
            return jsonBuilder.append("{}").toString();
        } else {
            jsonBuilder.append("{\n");
        }

        // Iterate through the fields without adding a space between the "fieldName" and colon
        for (Map.Entry<String, JsonNode> entry : jsonNode.properties()) {
            jsonBuilder.append(indent).append("  \"").append(entry.getKey()).append("\": ");
            JsonNode value = entry.getValue();

            if (value.isObject()) {
                jsonBuilder.append(buildJsonWithoutSpaces(value, indentLevel + 1));
            } else {
                jsonBuilder.append(value);
            }
            jsonBuilder.append(",\n");
        }

        if (jsonBuilder.length() > 1) {
            jsonBuilder.setLength(jsonBuilder.length() - 2); // Remove last comma and newline
        }
        jsonBuilder.append("\n").append(indent).append("}"); // Close the object

        return jsonBuilder.toString();
    }
}
