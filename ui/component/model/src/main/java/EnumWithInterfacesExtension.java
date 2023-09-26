import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.Utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This extension exports enums that implement an interface as an object whose keys are the enum names and whose value
 * is the enum value as an instance of the interface that the enum implements (only supports a single interface at the
 * moment).
 * <blockquote><pre>{@code
 * public interface Coloured {
 *    String getColour();
 * }
 *
 * public enum Fruit implements Coloured {
 *    BANANA("yellow"),
 *    ORANGE("orange");
 *
 *    public String getColour();
 * }
 *
 * public interface ColouredUsage {
 *    Coloured getColouredThing();
 * }
 * }
 * </pre></blockquote>
 *
 * <p>This would transpile to:
 *
 * <blockquote><pre>{@code
 * const Fruit = {
 *     BANANA: {colour: "yellow"},
 *     ORANGE: {colour: "orange"}
 * };
 *
 * interface Coloured {
 *    colour: string;
 * }
 *
 * interface ColouredUsage {
 *    colouredThing: Coloured;
 * }
 *
 * const colouredUsage: ColouredUsage = {
 *    colouredThing: Fruit.BANANA
 * }
 * }
 * </pre></blockquote>
 * <a href="https://github.com/vojtechhabarta/typescript-generator/issues/299">https://github.com/vojtechhabarta/typescript-generator/issues/299</a>
 */
@SuppressWarnings("deprecation")
public class EnumWithInterfacesExtension extends Extension {

    public static class EnumInterfaceModel {
        protected List<EnumInterfaceMemberModel> memberModels;
        protected Map<String, String> fieldTypeMap;

        public EnumInterfaceModel(List<EnumInterfaceMemberModel> memberModels, Map<String, String> fieldTypeMap) {
            this.memberModels = memberModels;
            this.fieldTypeMap = fieldTypeMap;
        }

        public List<EnumInterfaceMemberModel> getMemberModels() {
            return memberModels;
        }

        public Map<String, String> getFieldTypeMap() {
            return fieldTypeMap;
        }
    }

    public static class EnumInterfaceMemberModel {
        protected String memberName;
        protected Map<String, Object> memberFieldValues;

        public EnumInterfaceMemberModel(String memberName, Map<String, Object> memberFieldValues) {
            this.memberName = memberName;
            this.memberFieldValues = memberFieldValues;
        }

        public String getMemberName() {
            return memberName;
        }

        public Map<String, Object> getMemberFieldValues() {
            return memberFieldValues;
        }
    }

    public static final String CFG_ENUM_PATTERN = "enumPattern";
    public static final String CFG_ENUM_INTERFACE_PATTERN = "enumInterfacePattern";
    public static final String TMPL_ENUM_NAME = "$$EnumName$$";
    public static final String TMPL_ENUM_IMPLEMENTS = "$$EnumImplements$$";
    public static final String TMPL_ENUM_MEMBER_NAME = "$$MemberName$$";
    public static final String TMPL_ENUM_MEMBER_FIELD_VALUES = "$$MemberFieldValues$$";
    public static final String TMPL_ENUM_FIELDS = "$$EnumFields$$";
    public static final String TMPL_ENUM_FIELD_SEPARATOR = ", public readonly ";
    public static final String TMPL_ENUM_MEMBER_VALUE_SEPARATOR = ", ";

    protected static final List<String> ENUM_CLASS_HEADER_TEMPLATE = Arrays.asList("", "/*export*/ class $$EnumName$$ implements $$EnumImplements$$ {");
    protected static final List<String> ENUM_CLASS_MEMBER_TEMPLATE = Collections.singletonList("\tpublic static readonly $$MemberName$$ = new $$EnumName$$(\"$$MemberName$$\"$$MemberFieldValues$$);");
    protected static final List<String> ENUM_CLASS_FOOTER_TEMPLATE = Arrays.asList(
            "",
            "\tprivate constructor(public readonly name: string$$EnumFields$$) {}",
            "",
            "\ttoString() {",
            "\t\treturn this.name;",
            "\t}",
            "}");

    protected List<Pattern> enumPatterns = null;
    protected List<Pattern> enumInterfacePatterns = null;
    protected List<TsEnumModel> matchedEnums = new ArrayList<>();
    protected boolean hasNoConfig = true;

    public static final Predicate<Settings> supportsNullValuesPredicate = settings -> !(settings.optionalPropertiesDeclaration == null
            || settings.optionalPropertiesDeclaration == OptionalPropertiesDeclaration.questionMark
            || settings.optionalPropertiesDeclaration == OptionalPropertiesDeclaration.undefinableType);

    public static final Predicate<Field> interfaceFieldPredicate = field -> !field.isSynthetic()
            && !Modifier.isStatic(field.getModifiers())
            && field.getAnnotation(JsonIgnore.class) == null
            && (Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers()));

    protected JacksonAnnotationIntrospector ignoreJsonFormatIntrospector = new JacksonAnnotationIntrospector() {
        @Override
        protected <A extends Annotation> A _findAnnotation(Annotated ann, Class<A> annoClass) {
            if (ann.getRawType().isEnum()
                    && annoClass == JsonFormat.class
                    && ann.hasAnnotation(JsonFormat.class)
                    && ann.getAnnotation(JsonFormat.class).shape() == JsonFormat.Shape.OBJECT
                    && matchedEnums.stream().anyMatch(matchedEnum -> matchedEnum.getOrigin() == ann.getRawType())) {
                return null;
            }
            return super._findAnnotation(ann, annoClass);
        }
    };

    // TODO: Utils.globToRegexp should be public
    private static Pattern globToRegexp(String glob) {
        final Pattern globToRegexpPattern = Pattern.compile("(\\*\\*)|(\\*)");
        final Matcher matcher = globToRegexpPattern.matcher(glob);
        final StringBuffer sb = new StringBuffer();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(Pattern.quote(glob.substring(lastEnd, matcher.start())));
            if (matcher.group(1) != null) {
                sb.append(Matcher.quoteReplacement(".*"));
            }
            if (matcher.group(2) != null) {
                sb.append(Matcher.quoteReplacement("[^.$]*"));
            }
            lastEnd = matcher.end();
        }
        sb.append(Pattern.quote(glob.substring(lastEnd, glob.length())));
        return Pattern.compile(sb.toString());
    }

    @Override
    public EmitterExtensionFeatures getFeatures() {
        return new EmitterExtensionFeatures();
    }

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        enumPatterns = configuration.entrySet().stream()
                .filter(es -> es.getKey().startsWith(CFG_ENUM_PATTERN))
                .map(es -> globToRegexp(es.getValue())).collect(Collectors.toList());

        enumInterfacePatterns = configuration.entrySet().stream()
                .filter(es -> es.getKey().startsWith(CFG_ENUM_INTERFACE_PATTERN))
                .map(es -> globToRegexp(es.getValue())).collect(Collectors.toList());

        hasNoConfig = enumPatterns.isEmpty() && enumInterfacePatterns.isEmpty();
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, new TsModelTransformer() {
            @Override
            public TsModel transformModel(Context context, TsModel model) {
                List<TsEnumModel> enums = model.getEnums();

                for (TsEnumModel enm : enums) {
                    if (enumOrInterfacesMatch(enm)) {
                        matchedEnums.add(enm);
                    }
                }

                return model.withRemovedEnums(matchedEnums);
            }
        }));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {

        if (matchedEnums.isEmpty()) {
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new SimpleModule().addSerializer(Enum.class, new JsonSerializer<Enum>() {
                    @Override
                    public void serialize(Enum anEnum, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                        jsonGenerator.writeRawValue(anEnum.getClass().getSimpleName() + "." + anEnum.name());
                    }
                }))
                .setAnnotationIntrospector(ignoreJsonFormatIntrospector); // Ignore Shape Object so we get proper enum references

        if (!supportsNullValuesPredicate.test(settings)) {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Don't output nulls if settings don't support it
        }

        // Do a topological sort of matched enums to ensure they are output in the correct order
        sortEnums(matchedEnums);

        for (TsEnumModel tsEnum : matchedEnums) {

            final EnumInterfaceModel enumInterfaceModel = parseEnum(tsEnum, model, settings);

            // TODO: TsEnumModel should have interface details like TsBeanModel
            String implementsStr = Arrays.stream(tsEnum.getOrigin().getInterfaces()).map(i -> {
                TsBeanModel beanModel = model.getBean(i);
                return beanModel != null ? beanModel.getName().getFullName() : i.getSimpleName();
            }).collect(Collectors.joining(" | "));


            String fieldsStr = enumInterfaceModel.getFieldTypeMap().entrySet().stream().map(es -> es.getKey() + ": " + es.getValue()).collect(Collectors.joining(TMPL_ENUM_FIELD_SEPARATOR));
            if (!fieldsStr.isEmpty()) {
                fieldsStr = TMPL_ENUM_FIELD_SEPARATOR + fieldsStr;
            }

            final Map<String, String> replacements = Map.of(
            "\"", settings.quotes,
            "/*export*/ ", exportKeyword ? "export " : "",
            TMPL_ENUM_NAME, tsEnum.getName().getSimpleName(),
            TMPL_ENUM_IMPLEMENTS, implementsStr,
            TMPL_ENUM_FIELDS, fieldsStr);
            Emitter.writeTemplate(writer, settings, ENUM_CLASS_HEADER_TEMPLATE, replacements);

            enumInterfaceModel.getMemberModels().forEach(enumInterfaceMemberModel -> {

                String memberFieldValues = enumInterfaceMemberModel.getMemberFieldValues().entrySet().stream()
                        .map(es -> getMemberValue(tsEnum.getName().getFullName(), es.getKey(), es.getValue(), settings, objectMapper))
                        .collect(Collectors.joining(TMPL_ENUM_MEMBER_VALUE_SEPARATOR));
                if (!memberFieldValues.isEmpty()) {
                    memberFieldValues = TMPL_ENUM_MEMBER_VALUE_SEPARATOR + memberFieldValues;
                }
                replacements.put(TMPL_ENUM_MEMBER_NAME, enumInterfaceMemberModel.getMemberName());
                replacements.put(TMPL_ENUM_MEMBER_FIELD_VALUES, memberFieldValues);
                Emitter.writeTemplate(writer, settings, ENUM_CLASS_MEMBER_TEMPLATE, replacements);
            });

            Emitter.writeTemplate(writer, settings, ENUM_CLASS_FOOTER_TEMPLATE, replacements);
        }
    }

    protected boolean enumOrInterfacesMatch(TsEnumModel tsEnumModel) {
        return tsEnumModel.getOrigin().getInterfaces().length > 0 && (hasNoConfig || enumMatches(tsEnumModel.getName().getFullName()) || enumInterfacesMatch(tsEnumModel.getOrigin().getGenericInterfaces()));
    }

    protected boolean enumMatches(String enumName) {
        return enumPatterns != null && Utils.classNameMatches(enumName, enumPatterns);
    }

    protected boolean enumInterfacesMatch(Type[] interfaces) {
        return enumInterfacePatterns != null && Arrays.stream(interfaces).anyMatch(i -> Utils.classNameMatches(i.getTypeName(), enumInterfacePatterns));
    }

    /**
     * returns the string representation of an enum's member value
     */
    protected String getMemberValue(String enumName, String memberName, Object value, Settings settings, ObjectMapper objectMapper) {

        if (value == null) {
            if (!supportsNullValuesPredicate.test(settings)) {
                return TsType.Undefined.format(settings);
            }
            return TsType.Null.format(settings);
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to convert enum '" + enumName + "' member '" + memberName + "' value to string: " + ex.getMessage());
        }
    }

    /**
     * This method builds up a model that can be used to populate the enum class template;
     */
    // TODO: Is there a nicer way of doing some of the below with existing classes?
    protected EnumInterfaceModel parseEnum(TsEnumModel enm, TsModel model, Settings settings) {

        List<TsPropertyModel> interfacePropertyModels = Arrays.stream(enm.getOrigin().getInterfaces())
                .map(model::getBean)
                .filter(b -> !Objects.isNull(b))
                .flatMap(tsBean -> tsBean.getProperties().stream())
                .collect(Collectors.toList());

        // Get field info from implemented interfaces and ensure optional fields are at the end
        List<Pair<Field, Optional<TsPropertyModel>>> enumFieldInfos = Arrays.stream(enm.getOrigin().getDeclaredFields())
                .filter(interfaceFieldPredicate)
                .map(field -> {
                    // Ignore case in comparison as TsPropertyModel has the first two letters as lower case for some reason might be coming from jackson - not investigated (e.g. getAArray() -> aarray)
                    Optional<TsPropertyModel> matchingPropertyModel = interfacePropertyModels.stream().filter(ipm -> ipm.getName().equalsIgnoreCase(field.getName())).findFirst();
                    return Pair.of(field, matchingPropertyModel);
                })
                .sorted((pair1, pair2) -> {
                    boolean pair1IsOptional = pair1.getValue2().map(mpm -> mpm.getTsType() instanceof TsType.OptionalType).orElse(false);
                    boolean pair2IsOptional = pair2.getValue2().map(mpm -> mpm.getTsType() instanceof TsType.OptionalType).orElse(false);
                    return pair1IsOptional ? pair2IsOptional ? 0 : 1 : pair2IsOptional ? -1 : 0;
                })
                .collect(Collectors.toList());

        Map<String, String> fieldTypeMap = enumFieldInfos.stream().collect(LinkedHashMap::new,
                (map, enumFieldInfo) -> {

                    Field field = enumFieldInfo.getValue1();
                    Optional<TsPropertyModel> tsPropertyModel = enumFieldInfo.getValue2();
                    String fieldTypeStr;
                    boolean propertyOptional = tsPropertyModel.map(mpm -> mpm.getTsType() instanceof TsType.OptionalType).orElseGet(() -> settings.optionalProperties == OptionalProperties.all);

                    // TODO: Need access to the DefaultTypeProcessor known types here but it is private
                    if (tsPropertyModel.isPresent()) {
                        fieldTypeStr = tsPropertyModel.get().getTsType().toString();
                    } else if (field.getType() == String.class) {
                        fieldTypeStr = TsType.String.toString();
                    } else if (Number.class.isAssignableFrom(field.getType())) {
                        fieldTypeStr = TsType.Number.toString();
                    } else if (field.getType() == Boolean.class) {
                        fieldTypeStr = TsType.Boolean.toString();
                    } else if (field.getType() == Date.class) {
                        fieldTypeStr = TsType.Date.toString();
                    } else if (field.getType() == Void.class) {
                        fieldTypeStr = TsType.Void.toString();
                    } else {
                        fieldTypeStr = field.getType().getSimpleName();
                    }
                    map.put(propertyOptional ? field.getName() + "?" : field.getName(), fieldTypeStr);
                }, HashMap::putAll);

        List<EnumInterfaceMemberModel> memberModels = Arrays.stream(enm.getOrigin().getEnumConstants())
                .map(m -> {
                    String memberName = ((Enum<?>) m).name();
                    Map<String, Object> memberFieldValues = enumFieldInfos.stream().collect(LinkedHashMap::new, (map, enumFieldInfo) -> {

                        Field field = enumFieldInfo.getValue1();
                        Object val = null;
                        try {
                            field.setAccessible(true);
                            val = field.get(m);
                        } catch (IllegalAccessException ex) {
                            TypeScriptGenerator.getLogger().verbose("Failed to get field '" + field.getName() + "' value: " + ex.getMessage());
                        }

                        map.put(field.getName(), val);
                    }, HashMap::putAll);

                    return new EnumInterfaceMemberModel(memberName, memberFieldValues);
                })
                .collect(Collectors.toList());

        return new EnumInterfaceModel(memberModels, fieldTypeMap);
    }

    /**
     * Topological sort based on https://rosettacode.org/wiki/Topological_sort#Java
     */
    public static void sortEnums(List<TsEnumModel> enumModels) {
        if (enumModels.size() < 2) {
            return;
        }

        int count = enumModels.size();
        boolean[][] adjacency = new boolean[count][count];
        List<Integer> todo = new LinkedList<>();
        List<TsEnumModel> result = new LinkedList<>();
        BiFunction<Integer, List<Integer>, Boolean> hasDependency = (r, t) -> {
            for (Integer c : t) {
                if (adjacency[r][c])
                    return true;
            }
            return false;
        };

        List<int[]> edgeList = new ArrayList<>();

        for (int i=0; i<enumModels.size(); i++) {
            TsEnumModel enumModel = enumModels.get(i);

            int finalI = i;
            Arrays.stream(enumModel.getOrigin().getDeclaredFields())
                    .filter(interfaceFieldPredicate)
                    .map(f -> {
                        if (f.getType().isArray()) {
                            return f.getType().getComponentType();
                        }
                        return f.getType();
                    })
                    .map(t ->
                            enumModels.stream().filter(em -> {
                                // Check if type matches an enum or any of an enums interfaces
                                return em.getOrigin() == t || Arrays.stream(em.getOrigin().getInterfaces()).anyMatch(intrface -> intrface == t);
                            }).findFirst()
                    )
                    .filter(Optional::isPresent)
                    .map(modelOptional -> enumModels.indexOf(modelOptional.get()))
                    .map(dependencyIndex -> new int[] {finalI, dependencyIndex})
                    .forEach(edgeList::add);
        }

        for (int[] edge : edgeList) {
            adjacency[edge[0]][edge[1]] = true;
        }

        for (int i = 0; i < count; i++) {
            todo.add(i);
        }

        try {
            outer:
            while (!todo.isEmpty()) {
                for (Integer r : todo) {
                    if (!hasDependency.apply(r, todo)) {
                        todo.remove(r);
                        result.add(enumModels.get(r));
                        // no need to worry about concurrent modification
                        continue outer;
                    }
                }
                throw new Exception("Graph has cycles");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to order enums with interfaces so cannot generate output: " + e.getMessage());
        }

        enumModels.sort(Comparator.comparingInt(result::indexOf));
    }
}
