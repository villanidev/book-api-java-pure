package villanidev.httpserver.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.OutputStream;

public class JsonUtils {

    /*public static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper()
                //.registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }*/

    // Singleton fallback
    /*private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);*/

    // ThreadLocal for high-throughput scenarios
    private static final ThreadLocal<ObjectMapper> mapper = ThreadLocal.withInitial(
            () -> new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    );

    public static String toJson(Object object) {
        try {
            return mapper.get().writeValueAsString(object);
            //return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public static void toJsonStreaming(OutputStream outputStream, Object object) {
        try {
            mapper.get().writeValue(outputStream, object);
            //mapper.writeValue(outputStream, object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.get().readValue(json, type);
            //return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
}
