package hu.fcs.tdl;

import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.StringSchema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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

    @Test
    void test02TypeOfIdIsIntAndRequiredIsPresent() {
        validateInputWithSchema("/02/schema02.json", "/02/input02.json");
    }

    @Test
    void test02RequiredMissing() {
        validateExceptionMessage("/02/schema02.json", "/02/fail01requiredMissing.json", "#: required key [productId] not found");
    }

    @Test
    void test02WrongType() {
        validateExceptionMessage("/02/schema02.json", "/02/fail02wrongType.json", "#/productId: expected type: Number, found: String");
    }

    @Test
    void test03Minimum() {
        validateInputWithSchema("/03/schema03.json", "/03/input03.json");
    }

    @Test
    void test03LowerThanMinimum() {
        validateExceptionMessage("/03/schema03.json", "/03/fail03lowerThanMinimum.json", "#/price: 0 is not greater than 0");
    }

    @Test
    void test04TagsShouldBeANonEmptyList() {
        validateInputWithSchema("/04/schema04.json", "/04/input04.json");
    }

    @Test
    void test04OptionalPropertyCanBeMissing() {
        validateInputWithSchema("/04/schema04.json", "/04/input04optionalMissing.json");
    }

    @Test
    void test04TagsArraySouldBeUnique() {
        validateExceptionMessage("/04/schema04.json", "/04/fail04notUnique.json", "#/tags: array items are not unique");
    }

    @Test
    void test04TagsArraySouldNotBeEmpty() {
        validateExceptionMessage("/04/schema04.json", "/04/fail04emptyArray.json", "#/tags: expected minimum item count: 1, found: 0");
    }

    @Test
    void test04WrongTypeInArray() {
        validateExceptionMessage("/04/schema04.json", "/04/fail04wrongTypeInArray.json", "#/tags/2: expected type: String, found: Integer");
    }

    @Test
    void test04TagsShouldBeArray() {
        validateExceptionMessage("/04/schema04.json", "/04/fail04notArray.json", "#/tags: expected type: JSONArray, found: Null");
    }

    @Test
    void test05NestedObject() {
        validateInputWithSchema("/05/schema05.json", "/05/input05.json");
    }

    @Test
    void test05NestedObjectMissingProperty() {
        validateExceptionMessage("/05/schema05.json", "/05/fail05missingNestedProperty.json", "#/dimensions: required key [width] not found");
    }

    @Test
    void test05WrongNestedType() {
        validateExceptionMessage("/05/schema05.json", "/05/fail05wrongNestedType.json", "#/dimensions/height: expected type: Number, found: Null");
    }

    @Test
    void test06SchemaReference() {
        validateInputWithSchemaAndRef(
            "/06/schema06.json", "/06/input06.json",
            "http://example.com/location.schema.json", "/06/locationSchema.json"
        );
    }

    @Test
    void test07PrintJsonSchema() {
        // given
        Schema schema = loadSchema("/05/schema05.json");
        // when
        String schemaStr = schema.toString();
        // then
        assertThat(schemaStr, containsString("\"Name of the product\""));
        assertThat(schemaStr, containsString("\"width\":{\"type\":\"number\"}"));
    }

    @Test
    void test08BuildJsonSchema() {
        // given
        StringSchema stringSchema = StringSchema.builder().build();
        ObjectSchema.Builder builder = ObjectSchema.builder().addPropertySchema("name", stringSchema);
        Schema schema = new ObjectSchema(builder);
        // when
        String schemaStr = schema.toString();
        // then
        assertThat(schemaStr, is("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"));
    }

    private void validateInputWithSchema(String schemaFileName, String inputFileName) {
        SchemaLoader.SchemaLoaderBuilder builder = SchemaLoader.builder();
        validateInputWithSchema(schemaFileName, inputFileName, builder);
    }

    private void validateInputWithSchemaAndRef(String schemaFileName, String inputFileName, String refURI, String refFileName) {
        try (
            InputStream refStream = new FileInputStream("./src/test/resources/" + refFileName);
        ) {
            JSONObject refSchemaJson = new JSONObject(new JSONTokener(refStream));
            SchemaLoader.SchemaLoaderBuilder builder = SchemaLoader.builder().registerSchemaByURI(new URI(refURI), refSchemaJson);
            validateInputWithSchema(schemaFileName, inputFileName, builder);
        } catch (IOException | URISyntaxException e) {
            fail(e);
        }
    }

    private void validateInputWithSchema(String schemaFileName, String inputFileName, SchemaLoader.SchemaLoaderBuilder builder) {
        try (
            InputStream inputStream = new FileInputStream("./src/test/resources" + inputFileName);
        ) {
            Schema schema = loadSchema(schemaFileName, builder);
            JSONTokener tokener = new JSONTokener(inputStream);
            Object value = tokener.nextValue();
            schema.validate(value);
        } catch (IOException e) {
            fail(e);
        }
    }

    private Schema loadSchema(String schemaFileName) {
        return loadSchema(schemaFileName, SchemaLoader.builder());
    }

    private Schema loadSchema(String schemaFileName, SchemaLoader.SchemaLoaderBuilder builder) {
        Schema schema = null;
        try (
            InputStream schemaStream = new FileInputStream("./src/test/resources" + schemaFileName);

        ) {
            JSONObject schemaJson = new JSONObject(new JSONTokener(schemaStream));
            SchemaLoader schemaLoader = builder.schemaJson(schemaJson).build();
            schema = schemaLoader.load().build();
        } catch (IOException e) {
            fail(e);
        }
        return schema;
    }


    private void validateExceptionMessage(String schemaFileName, String inputFileName, String exceptionMessage) {
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> validateInputWithSchema(schemaFileName, inputFileName)
        );
        assertThat(exception.getMessage(), is(exceptionMessage));
    }
}
