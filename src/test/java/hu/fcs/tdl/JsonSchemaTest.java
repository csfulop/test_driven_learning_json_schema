package hu.fcs.tdl;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;


public class JsonSchemaTest {

    @Test
    void testJsonSchema() throws IOException {
        try (
            InputStream schemaStream = getClass().getResourceAsStream("/schema01.json");
            InputStream inputStream = getClass().getResourceAsStream("/schema01.json");
        ) {
            JSONObject schemaJson = new JSONObject(new JSONTokener(schemaStream));
            Schema schema = SchemaLoader.load(schemaJson);
            JSONObject inputJson = new JSONObject(new JSONTokener(inputStream));
            schema.validate(inputJson);
        }
    }
}
