package com.github.nmorel.gwtjackson.jackson.advanced;

import com.github.nmorel.gwtjackson.jackson.AbstractJacksonTest;
import com.github.nmorel.gwtjackson.shared.advanced.InheritanceTester;
import org.junit.Test;

/** @author Nicolas Morel */
public class InheritanceJacksonTest extends AbstractJacksonTest
{
    @Test
    public void testEncodingPrivateField()
    {
        InheritanceTester.INSTANCE.testEncoding( createEncoder( InheritanceTester.ChildBean.class ) );
    }

    @Test
    public void testDecodingPrivateField()
    {
        InheritanceTester.INSTANCE.testDecoding( createDecoder( InheritanceTester.ChildBean.class ) );
    }
}