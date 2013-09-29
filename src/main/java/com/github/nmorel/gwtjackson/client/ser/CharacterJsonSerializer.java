package com.github.nmorel.gwtjackson.client.ser;

import javax.annotation.Nonnull;
import java.io.IOException;

import com.github.nmorel.gwtjackson.client.JsonEncodingContext;
import com.github.nmorel.gwtjackson.client.JsonSerializer;
import com.github.nmorel.gwtjackson.client.stream.JsonWriter;

/**
 * Default {@link com.github.nmorel.gwtjackson.client.JsonSerializer} implementation for {@link Character}.
 *
 * @author Nicolas Morel
 */
public class CharacterJsonSerializer extends JsonSerializer<Character> {

    private static final CharacterJsonSerializer INSTANCE = new CharacterJsonSerializer();

    /**
     * @return an instance of {@link CharacterJsonSerializer}
     */
    public static CharacterJsonSerializer getInstance() {
        return INSTANCE;
    }

    private CharacterJsonSerializer() { }

    @Override
    public void doEncode( JsonWriter writer, @Nonnull Character value, JsonEncodingContext ctx ) throws IOException {
        writer.value( value.toString() );
    }
}