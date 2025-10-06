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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
                String keyword = (String) annotationClass.getMethod("keyword").invoke(ann);
                String key = resolveKey(element, keyword);
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
        Map<String, Object> existing = new LinkedHashMap<>();

        if (Files.exists(path)) {
            try {
                existing.putAll(mapper.readValue(path.toFile(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                throw new RuntimeException("");
            }
        }

        for (var entry : translations.entrySet()) {
            existing.putIfAbsent(entry.getKey(), writeValue ? entry.getValue() : "");
        }

        try {
            mapper.writeValue(path.toFile(), existing);
        } catch (Exception e) {
            throw new RuntimeException("");
        }
    }
}
