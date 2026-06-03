/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.manager.rules;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.openremote.model.rules.Ruleset;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportingGroovyCompilationCustomizer extends CompilationCustomizer {

    private static final Logger LOG = Logger.getLogger(ReportingGroovyCompilationCustomizer.class.getName());

    protected static final Set<String> GRAPE_ANNOTATIONS = Set.of(
        "Grab",
        "GrabConfig",
        "GrabExclude",
        "GrabResolver",
        "GrabResolvers",
        "Grapes",
        "groovy.lang.Grab",
        "groovy.lang.GrabConfig",
        "groovy.lang.GrabExclude",
        "groovy.lang.GrabResolver",
        "groovy.lang.GrabResolvers",
        "groovy.lang.Grapes"
    );
    protected static final Pattern GRAPE_ANNOTATION_PATTERN = Pattern.compile(
        "@\\s*(?:groovy\\.lang\\.)?(Grab|GrabConfig|GrabExclude|GrabResolver|GrabResolvers|Grapes)\\b"
    );

    protected final GroovySandboxReporter reporter;
    protected final Ruleset ruleset;
    protected final Set<String> reportedSources = ConcurrentHashMap.newKeySet();

    public ReportingGroovyCompilationCustomizer(GroovySandboxReporter reporter, Ruleset ruleset) {
        super(CompilePhase.CONVERSION);
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.ruleset = Objects.requireNonNull(ruleset, "ruleset");
    }

    public static void reportGrapeAnnotations(GroovySandboxReporter reporter, Ruleset ruleset, String source) {
        if (reporter == null || ruleset == null || source == null || source.isBlank()) {
            return;
        }

        Matcher matcher = GRAPE_ANNOTATION_PATTERN.matcher(source);
        while (matcher.find()) {
            String annotationType = "groovy.lang." + matcher.group(1);
            reporter.report(ruleset, GroovySandboxSignature.of(
                GroovySandboxPhase.SOURCE,
                GroovySandboxOperation.SOURCE_GRAB,
                annotationType,
                GroovySandboxSignature.NONE,
                GroovySandboxClassifier.classify(
                    GroovySandboxOperation.SOURCE_GRAB,
                    annotationType,
                    GroovySandboxSignature.NONE
                )
            ));
        }
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        try {
            String sourceName = source == null ? GroovySandboxSignature.NONE : source.getName();
            if (!reportedSources.add(sourceName)) {
                return;
            }

            ModuleNode module = source == null ? null : source.getAST();
            if (module != null) {
                reportModule(module);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to report Groovy sandbox source signatures", e);
        }
    }

    protected void reportModule(ModuleNode module) {
        if (module.hasPackageName()) {
            report(GroovySandboxOperation.SOURCE_PACKAGE, removeTrailingDot(module.getPackageName()), GroovySandboxSignature.NONE);
        }

        if (module.hasPackage()) {
            reportAnnotations(module.getPackage().getAnnotations());
        }

        module.getImports().forEach(importNode -> {
            reportImport(GroovySandboxOperation.SOURCE_IMPORT, importClassName(importNode), importAlias(importNode));
            reportAnnotations(importNode.getAnnotations());
        });

        module.getStarImports().forEach(importNode -> {
            reportImport(GroovySandboxOperation.SOURCE_STAR_IMPORT, importPackageName(importNode), "*");
            reportAnnotations(importNode.getAnnotations());
        });

        for (Map.Entry<String, ImportNode> entry : module.getStaticImports().entrySet()) {
            ImportNode importNode = entry.getValue();
            reportImport(GroovySandboxOperation.SOURCE_STATIC_IMPORT, importClassName(importNode), importMember(importNode, entry.getKey()));
            reportAnnotations(importNode.getAnnotations());
        }

        for (Map.Entry<String, ImportNode> entry : module.getStaticStarImports().entrySet()) {
            ImportNode importNode = entry.getValue();
            reportImport(GroovySandboxOperation.SOURCE_STATIC_STAR_IMPORT, importClassName(importNode), "*");
            reportAnnotations(importNode.getAnnotations());
        }

        module.getClasses().forEach(this::reportClass);
    }

    protected void reportClass(ClassNode classNode) {
        if (classNode == null) {
            return;
        }

        report(GroovySandboxOperation.SOURCE_CLASS, className(classNode), className(classNode.getSuperClass()));
        reportAnnotations(classNode.getAnnotations());

        for (ConstructorNode constructor : classNode.getDeclaredConstructors()) {
            reportMethod(classNode, constructor, "<init>");
        }

        for (MethodNode method : classNode.getMethods()) {
            reportMethod(classNode, method, method.getName());
        }
    }

    protected void reportMethod(ClassNode owner, MethodNode method, String name) {
        if (method == null || method.isSynthetic() || method.isStaticConstructor()) {
            return;
        }

        report(
            GroovySandboxOperation.SOURCE_METHOD,
            className(owner),
            name,
            parameterTypeNames(method.getParameters())
        );
        reportAnnotations(method.getAnnotations());
    }

    protected void reportAnnotations(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }

        for (AnnotationNode annotation : annotations) {
            String annotationType = className(annotation.getClassNode());
            report(
                isGrapeAnnotation(annotationType) ? GroovySandboxOperation.SOURCE_GRAB : GroovySandboxOperation.SOURCE_ANNOTATION,
                annotationType,
                GroovySandboxSignature.NONE
            );
        }
    }

    protected void reportImport(GroovySandboxOperation operation, String receiverType, String member) {
        report(operation, receiverType, member);
    }

    protected void report(GroovySandboxOperation operation, String receiverType, String member, String... argumentTypes) {
        GroovySandboxClassification classification = GroovySandboxClassifier.classify(operation, receiverType, member);
        reporter.report(ruleset, GroovySandboxSignature.of(
            GroovySandboxPhase.SOURCE,
            operation,
            receiverType,
            member,
            classification,
            argumentTypes
        ));
    }

    protected static String importClassName(ImportNode importNode) {
        if (importNode == null) {
            return null;
        }

        String className = className(importNode.getType());
        return className == null ? importNode.getClassName() : className;
    }

    protected static String importPackageName(ImportNode importNode) {
        if (importNode == null) {
            return null;
        }

        String packageName = importNode.getPackageName();
        return packageName == null ? importNode.getText() : packageName;
    }

    protected static String importAlias(ImportNode importNode) {
        if (importNode == null || importNode.getAlias() == null) {
            return GroovySandboxSignature.NONE;
        }

        return importNode.getAlias();
    }

    protected static String importMember(ImportNode importNode, String fallback) {
        if (importNode != null && importNode.getFieldName() != null) {
            return importNode.getFieldName();
        }

        return fallback;
    }

    protected static String[] parameterTypeNames(Parameter[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return new String[0];
        }

        return Arrays.stream(parameters)
            .map(Parameter::getType)
            .map(ReportingGroovyCompilationCustomizer::className)
            .toArray(String[]::new);
    }

    protected static String className(ClassNode classNode) {
        if (classNode == null) {
            return null;
        }
        return classNode.getName();
    }

    protected static boolean isGrapeAnnotation(String annotationType) {
        return annotationType != null && GRAPE_ANNOTATIONS.contains(annotationType);
    }

    protected static String removeTrailingDot(String value) {
        if (value == null || !value.endsWith(".")) {
            return value;
        }

        return value.substring(0, value.length() - 1);
    }
}
