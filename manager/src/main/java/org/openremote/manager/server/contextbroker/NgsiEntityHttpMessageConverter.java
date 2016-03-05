package org.openremote.manager.server.contextbroker;

public class NgsiEntityHttpMessageConverter { /*extends AbstractHttpMessageConverter<Object> {

    public static final String OPERATION_IS_UPDATE = "OPERATION_IS_UPDATE";

    public NgsiEntityHttpMessageConverter() {
        super(
            new MediaType("application", "json", Charsets.UTF_8),
            new MediaType("application", "*+json", Charsets.UTF_8)
        );
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.equals(Entity.class)
            || clazz.equals(Entity[].class)
            || clazz.equals(KeyValueEntity[].class)
            || clazz.equals(KeyValueEntity.class);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage input) throws HttpMessageConverterException {
        try {
            if (clazz.equals(Entity.class)) {
                JsonObject jsonObject = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                return new Entity(jsonObject);
            } else if (clazz.equals(Entity[].class)) {
                List<Entity> list = new ArrayList<>();
                JsonArray jsonArray = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                for (int i = 0; i < jsonArray.length(); i++) {
                    JsonObject jsonObject = jsonArray.get(i);
                    list.add(new Entity(jsonObject));
                }
                return list.toArray(new Entity[list.size()]);
            } else if (clazz.equals(KeyValueEntity.class)) {
                JsonObject jsonObject = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                return new KeyValueEntity(jsonObject);
            } else if (clazz.equals(KeyValueEntity[].class)) {
                List<KeyValueEntity> list = new ArrayList<>();
                JsonArray jsonArray = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                for (int i = 0; i < jsonArray.length(); i++) {
                    JsonObject jsonObject = jsonArray.get(i);
                    list.add(new KeyValueEntity(jsonObject));
                }
                return list.toArray(new KeyValueEntity[list.size()]);
            } else {
                throw new HttpMessageConverterException("Unsupported type: " + clazz);
            }
        } catch (IOException ex) {
            throw new HttpMessageConverterException("Error converting from JSON", ex);
        }
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage output) throws HttpMessageConverterException {
        try {

            if (object instanceof AbstractEntity) {
                AbstractEntity entity = (AbstractEntity) object;
                output.write(getJsonObject(entity, output.getHeaders()).toJson().getBytes("utf-8"));
            } else if (object instanceof AbstractEntity[]) {
                AbstractEntity[] abstractEntities = (AbstractEntity[]) object;
                JsonArray jsonArray = Json.createArray();
                for (int i = 0; i < abstractEntities.length; i++) {
                    JsonObject jsonObject = getJsonObject(abstractEntities[i], output.getHeaders());
                    jsonArray.set(i, jsonObject);
                }
                output.write(jsonArray.toJson().getBytes("utf-8"));
            }
        } catch (Exception ex) {
            throw new HttpMessageConverterException("Error converting to JSON", ex);
        }
    }

    protected JsonObject getJsonObject(AbstractEntity entity, MultiMap headers) {
        // Need to strip out id and type attributes of an entity if it's a PATCH or POST. Since we
        // don't know if it's a PATCH or POST here, we look for a temporary header flag and then
        // remove it. Why doesn't the NGSI server just ignore those fields? FU, that's why.
        String isUpdate = headers.get(OPERATION_IS_UPDATE);
        if (isUpdate != null) {
            headers.remove(OPERATION_IS_UPDATE);
            JsonObject original = entity.getJsonObject();
            JsonObject copy = Json.parse(original.toJson());
            if (copy.hasKey("id"))
                copy.remove("id");
            if (copy.hasKey("type"))
                copy.remove("type");
            return copy;
        }
        return entity.getJsonObject();
    }
    */
}
