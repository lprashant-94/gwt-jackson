/*
 * Copyright 2013 Nicolas Morel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nmorel.gwtjackson.client.deser.map.key;

/**
 * @author Nicolas Morel
 */
public class StringKeyDeserializerTest extends AbstractKeyDeserializerTest<String> {

    @Override
    protected StringKeyDeserializer createDeserializer() {
        return StringKeyDeserializer.getInstance();
    }

    @Override
    public void testDeserializeValue() {
        assertDeserialization( "", "" );
        assertDeserialization( "Json", "Json" );
        assertDeserialization( "&é(-è_ çà)='", "&é(-è_ çà)='" );
    }
}
