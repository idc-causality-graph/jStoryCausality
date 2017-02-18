package il.ac.idc.yonatan.causality.config.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

/**
 * Created by ygraber on 2/18/17.
 */
@JsonComponent
public class JsonPairComponent {
    public static class PairDeserializer extends JsonDeserializer<Pair> {
        public PairDeserializer(){
            System.out.println("\n\n\nhi\n\n\n");
        }
        @Override
        public Pair deserialize(
                JsonParser jsonParser,
                DeserializationContext deserializationContext) throws IOException {
            final Object[] array = jsonParser.readValueAs(Object[].class);
            return Pair.of(array[0], array[1]);
        }
    }

    public static class PairSerializer extends JsonSerializer<Pair> {
        @Override
        public void serialize(
                Pair pair,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartArray(2);
            jsonGenerator.writeObject(pair.getLeft());
            jsonGenerator.writeObject(pair.getRight());
            jsonGenerator.writeEndArray();
        }
    }
}
