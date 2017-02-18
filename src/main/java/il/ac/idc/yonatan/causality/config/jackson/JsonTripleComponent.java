package il.ac.idc.yonatan.causality.config.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

/**
 * Created by ygraber on 2/18/17.
 */
@JsonComponent
public class JsonTripleComponent {
    public static class TripleDeserializer extends JsonDeserializer<Triple> {
        @Override
        public Triple deserialize(
                JsonParser jsonParser,
                DeserializationContext deserializationContext) throws IOException {
            final Object[] array = jsonParser.readValueAs(Object[].class);
            return Triple.of(array[0], array[1], array[2]);
        }
    }

    public static class TripleSerializer extends JsonSerializer<Triple> {
        @Override
        public void serialize(
                Triple triple,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartArray(3);
            jsonGenerator.writeObject(triple.getLeft());
            jsonGenerator.writeObject(triple.getMiddle());
            jsonGenerator.writeObject(triple.getRight());
            jsonGenerator.writeEndArray();
        }
    }
}
