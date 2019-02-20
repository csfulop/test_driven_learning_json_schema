package hu.fcs.tdl;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


public class JsonSchemaTest {

    @Test
    void test01ObjectShouldAcceptObject() {
        validateInputWithSchema("/01/schema01.json", "/01/input01.json");
    }

    @ParameterizedTest
    @ValueSource(strings = {"fail01null.json", "fail02string.json", "fail03boolean.json", "fail04int.json", "fail05list.json", "fail06float.json"})
    void test01ObjectShouldFailNonObject(String input) {
        assertThrows(
            ValidationException.class,
            () -> validateInputWithSchema("/01/schema01.json", "/01/" + input)
        );
    }

    private void validateInputWithSchema(String schemaFileName, String inputFileName) {
        try (
            InputStream schemaStream = getClass().getResourceAsStream(schemaFileName);
            InputStream inputStream = getClass().getResourceAsStream(inputFileName);
        ) {
            if (schemaStream == null) {
                fail("resource file: " + schemaFileName + " doesn't exists");
            }
            if (inputStream == null) {
                fail("resource file: " + inputFileName + " doesn't exists");
            }
            JSONObject schemaJson = new JSONObject(new JSONTokener(schemaStream));
            Schema schema = SchemaLoader.load(schemaJson);
            JSONTokener tokener = new JSONTokener(inputStream);
            Object value = tokener.nextValue();
            schema.validate(value);
        } catch (IOException e) {
            fail(e);
        }
    }
}
