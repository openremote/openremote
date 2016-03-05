package org.openremote.manager.server.contextbroker;

public class NgsiEntryPointHttpMessageConverter { /* extends AbstractHttpMessageConverter<EntryPoint> {

    private static final Logger LOG = Logger.getLogger(NgsiEntryPointHttpMessageConverter.class.getName());

    public NgsiEntryPointHttpMessageConverter() {
        super(
            new MediaType("application", "json", Charsets.UTF_8),
            new MediaType("application", "*+json", Charsets.UTF_8)
        );
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.equals(EntryPoint.class);
    }

    @Override
    protected EntryPoint readInternal(Class<? extends EntryPoint> clazz, HttpInputMessage input) throws HttpMessageConverterException {
        try {
            JsonObject jsonObject = Json.parse(new String(input.getBody(), "utf-8"));
            return new EntryPoint(jsonObject);
        } catch (IOException ex) {
            throw new HttpMessageConverterException("Error converting from JSON", ex);
        }
    }

    @Override
    protected void writeInternal(EntryPoint object, HttpOutputMessage output) throws HttpMessageConverterException {
        try {
            output.write(object.getJsonObject().toJson().getBytes("utf-8"));
        } catch (Exception ex) {
            throw new HttpMessageConverterException("Error converting to JSON", ex);
        }
    }
    */
}
