package com.github.nmorel.gwtjackson.rebind;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.github.nmorel.gwtjackson.client.deser.bean.AbstractBeanJsonDeserializer;
import com.github.nmorel.gwtjackson.client.ser.bean.AbstractBeanJsonSerializer;
import com.github.nmorel.gwtjackson.client.ser.bean.ObjectIdSerializer;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.rebind.AbstractSourceCreator;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

/**
 * @author Nicolas Morel
 */
public abstract class AbstractCreator extends AbstractSourceCreator {

    public static final List<String> BASE_TYPES = Arrays
        .asList( "java.math.BigDecimal", "java.math.BigInteger", "java.lang.Boolean", "java.lang.Byte", "java.lang.Character",
            "java.util.Date", "java.lang.Double", "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Short",
            "java.sql.Date", "java.sql.Time", "java.sql.Timestamp", "java.lang.String", "java.util.UUID" );

    public static final String BEAN_INSTANCE_NAME = "$$instance$$";

    public static final String IS_SET_FORMAT = "is_%s_set";

    public static final String BUILDER_MAPPER_FORMAT = "mapper_%s";

    public static final String JSON_DESERIALIZER_CLASS = "com.github.nmorel.gwtjackson.client.JsonDeserializer";

    public static final String JSON_SERIALIZER_CLASS = "com.github.nmorel.gwtjackson.client.JsonSerializer";

    public static final String JSON_READER_CLASS = "com.github.nmorel.gwtjackson.client.stream.JsonReader";

    public static final String JSON_WRITER_CLASS = "com.github.nmorel.gwtjackson.client.stream.JsonWriter";

    public static final String JSON_DECODING_CONTEXT_CLASS = "com.github.nmorel.gwtjackson.client.JsonDecodingContext";

    public static final String JSON_ENCODING_CONTEXT_CLASS = "com.github.nmorel.gwtjackson.client.JsonEncodingContext";

    public static final String ARRAY_CREATOR_CLASS = "com.github.nmorel.gwtjackson.client.deser.array.ArrayJsonDeserializer.ArrayCreator";

    protected static final String IDENTITY_SERIALIZATION_INFO_CLASS = "com.github.nmorel.gwtjackson.client.ser.bean" + "" +
        ".IdentitySerializationInfo";

    protected static final String IDENTITY_DESERIALIZATION_INFO_CLASS = "com.github.nmorel.gwtjackson.client.deser.bean" + "" +
        ".IdentityDeserializationInfo";

    /**
     * Returns the String represention of the java type for a primitive for example int/Integer, float/Float, char/Character.
     *
     * @param type primitive type
     *
     * @return the string representation
     */
    protected static String getJavaObjectTypeFor( JPrimitiveType type ) {
        if ( type == JPrimitiveType.INT ) {
            return "Integer";
        } else if ( type == JPrimitiveType.CHAR ) {
            return "Character";
        } else {
            String s = type.getSimpleSourceName();
            return s.substring( 0, 1 ).toUpperCase() + s.substring( 1 );
        }
    }

    protected final TreeLogger logger;

    protected final GeneratorContext context;

    protected final JacksonTypeOracle typeOracle;

    protected AbstractCreator( TreeLogger logger, GeneratorContext context ) {
        this( logger, context, new JacksonTypeOracle( logger, context.getTypeOracle() ) );
    }

    protected AbstractCreator( TreeLogger logger, GeneratorContext context, JacksonTypeOracle typeOracle ) {
        this.logger = logger;
        this.context = context;
        this.typeOracle = typeOracle;
    }

    protected PrintWriter getPrintWriter( String packageName, String className ) {
        return context.tryCreate( logger, packageName, className );
    }

    protected SourceWriter getSourceWriter( PrintWriter printWriter, String packageName, String className, String superClass,
                                            String... interfaces ) {
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory( packageName, className );
        if ( null != superClass ) {
            composer.setSuperclass( superClass );
        }
        for ( String interfaceName : interfaces ) {
            composer.addImplementedInterface( interfaceName );
        }
        return composer.createSourceWriter( context, printWriter );
    }

    protected String getQualifiedClassName( JType type ) {
        if ( null == type.isPrimitive() ) {
            return type.getParameterizedQualifiedSourceName();
        } else {
            return type.isPrimitive().getQualifiedBoxedSourceName();
        }
    }

    /**
     * Build the string that instantiate a serializer for the given type. If the type is a bean,
     * the implementation of {@link AbstractBeanJsonSerializer} will
     * be created.
     *
     * @param type type
     *
     * @return the code instantiating the serializer. Examples: <ul><li>ctx.getIntegerSerializer()</li><li>new org
     *         .PersonBeanJsonSerializer()
     *         </li></ul>
     */
    protected String getSerializerFromType( JType type ) throws UnableToCompleteException {
        return getSerializerFromType( type, null );
    }

    /**
     * Build the string that instantiate a serializer for the given type. If the type is a bean,
     * the implementation of {@link AbstractBeanJsonSerializer} will
     * be created.
     *
     * @param type type
     * @param propertyInfo additionnal info to gives to the serializer
     *
     * @return the code instantiating the serializer. Examples: <ul><li>ctx.getIntegerSerializer()</li><li>new org
     *         .PersonBeanJsonSerializer()
     *         </li></ul>
     */
    protected String getSerializerFromType( JType type, PropertyInfo propertyInfo ) throws UnableToCompleteException {
        JPrimitiveType primitiveType = type.isPrimitive();
        if ( null != primitiveType ) {
            String boxedName = getJavaObjectTypeFor( primitiveType );
            return "ctx.get" + boxedName + "JsonSerializer()";
        }

        JEnumType enumType = type.isEnum();
        if ( null != enumType ) {
            return String.format( "ctx.<%s>getEnumJsonSerializer()", enumType.getQualifiedSourceName() );
        }

        JArrayType arrayType = type.isArray();
        if ( null != arrayType ) {
            if ( null != arrayType.getComponentType().isPrimitive() ) {
                String boxedName = getJavaObjectTypeFor( arrayType.getComponentType().isPrimitive() );
                return "ctx.getPrimitive" + boxedName + "ArrayJsonSerializer()";
            } else {
                return String.format( "ctx.newArrayJsonSerializer(%s)", getSerializerFromType( arrayType
                    .getComponentType(), propertyInfo ) );
            }
        }

        JParameterizedType parameterizedType = type.isParameterized();
        if ( null != parameterizedType ) {
            String result;

            if ( typeOracle.isIterable( parameterizedType ) ) {
                result = String.format( "ctx.<%s, %s>newIterableJsonSerializer", parameterizedType
                    .getParameterizedQualifiedSourceName(), parameterizedType.getTypeArgs()[0]
                    .getParameterizedQualifiedSourceName() ) + "(%s)";
            } else if ( typeOracle.isMap( parameterizedType ) ) {
                // TODO add support for map
                logger.log( TreeLogger.Type.ERROR, "Map are not supported yet" );
                throw new UnableToCompleteException();
            } else {
                // TODO
                logger.log( TreeLogger.Type.ERROR, "Parameterized type '" + parameterizedType
                    .getQualifiedSourceName() + "' is not supported" );
                throw new UnableToCompleteException();
            }

            JClassType[] args = parameterizedType.getTypeArgs();
            String[] mappers = new String[args.length];
            for ( int i = 0; i < args.length; i++ ) {
                mappers[i] = getSerializerFromType( args[i], propertyInfo );
            }

            return String.format( result, mappers );
        }

        // TODO should we use isClassOrInterface ? need to add test for interface
        JClassType classType = type.isClass();
        if ( null != classType ) {
            String qualifiedSourceName = classType.getQualifiedSourceName();
            if ( BASE_TYPES.contains( qualifiedSourceName ) ) {
                if ( qualifiedSourceName.startsWith( "java.sql" ) ) {
                    return "ctx.getSql" + classType.getSimpleSourceName() + "JsonSerializer()";
                } else {
                    return "ctx.get" + classType.getSimpleSourceName() + "JsonSerializer()";
                }
            }

            // it's a bean
            BeanJsonSerializerCreator beanJsonSerializerCreator = new BeanJsonSerializerCreator( logger
                .branch( TreeLogger.Type.INFO, "Creating serializer for " + classType.getQualifiedSourceName() ), context, typeOracle );
            BeanJsonMapperInfo info = beanJsonSerializerCreator.create( classType );
            return String.format( "new %s(%s)", info
                .getQualifiedSerializerClassName(), generateBeanJsonSerializerParameters( classType, info, propertyInfo ) );
        }

        logger.log( TreeLogger.Type.ERROR, "Type '" + type.getQualifiedSourceName() + "' is not supported" );
        throw new UnableToCompleteException();
    }

    private String generateBeanJsonSerializerParameters( JClassType type, BeanJsonMapperInfo info,
                                                         PropertyInfo propertyInfo ) throws UnableToCompleteException {
        if ( null == propertyInfo || (null == propertyInfo.getIdentityInfo()) ) {
            return "";
        }

        StringSourceWriter sourceWriter = new StringSourceWriter();

        if ( null == propertyInfo.getIdentityInfo() ) {
            sourceWriter.print( "null" );
        } else {
            findIdPropertyInfo( info.getProperties(), propertyInfo.getIdentityInfo() );
            generateIdentifierSerializationInfo( sourceWriter, type, propertyInfo.getIdentityInfo() );
        }

        sourceWriter.print( ", " );

        // TODO subtype info on property
        sourceWriter.print( "null" );

        return sourceWriter.toString();
    }

    protected void generateIdentifierSerializationInfo( SourceWriter source, JClassType type, BeanIdentityInfo identityInfo ) throws
        UnableToCompleteException {
        String qualifiedType = getQualifiedClassName( identityInfo.getType() );

        String identityPropertyClass = String.format( "%s<%s, %s>", IDENTITY_SERIALIZATION_INFO_CLASS, type
            .getParameterizedQualifiedSourceName(), qualifiedType );

        source.println( "new %s(%s, \"%s\") {", identityPropertyClass, identityInfo.isAlwaysAsId(), identityInfo.getPropertyName() );
        source.indent();

        source.println();
        source.println( "@Override" );
        source.println( "protected %s<%s> newSerializer(%s ctx) {", JSON_SERIALIZER_CLASS, qualifiedType, JSON_ENCODING_CONTEXT_CLASS );
        source.indent();
        source.println( "return %s;", getSerializerFromType( identityInfo.getType() ) );
        source.outdent();
        source.println( "}" );
        source.println();

        source.println( "@Override" );
        source.println( "public %s<%s> getObjectId(%s bean, %s ctx) {", ObjectIdSerializer.class.getName(), qualifiedType, type
            .getParameterizedQualifiedSourceName(), JSON_ENCODING_CONTEXT_CLASS );
        source.indent();
        if ( null == identityInfo.getProperty() ) {
            String generatorType = String.format( "%s<%s>", ObjectIdGenerator.class.getName(), qualifiedType );
            source.println( "%s generator = new %s().forScope(%s.class);", generatorType, identityInfo.getGenerator()
                .getCanonicalName(), identityInfo.getScope().getName() );
            source.println( "%s scopedGen = ctx.findObjectIdGenerator(generator);", generatorType );
            source.println( "if(null == scopedGen) {" );
            source.indent();
            source.println( "scopedGen = generator.newForSerialization(ctx);" );
            source.println( "ctx.addGenerator(scopedGen);" );
            source.outdent();
            source.println( "}" );
            source.println( "return new %s<%s>(scopedGen.generateId(bean), getSerializer(ctx));", ObjectIdSerializer.class
                .getName(), qualifiedType );
        } else {
            source.println( "return new %s<%s>(%s, getSerializer(ctx));", ObjectIdSerializer.class.getName(), qualifiedType, identityInfo
                .getProperty().getGetterAccessor() );
        }
        source.outdent();
        source.println( "}" );
        source.println();

        source.outdent();
        source.println( "}" );
    }

    /**
     * Build the string that instantiate a deserializer for the given type. If the type is a bean,
     * the implementation of {@link AbstractBeanJsonDeserializer} will
     * be created.
     *
     * @param type type
     *
     * @return the code instantiating the deserializer. Examples: <ul><li>ctx.getIntegerDeserializer()</li><li>new org
     *         .PersonBeanJsonDeserializer()
     *         </li></ul>
     */
    protected String getDeserializerFromType( JType type ) throws UnableToCompleteException {
        return getDeserializerFromType( type, null );
    }

    /**
     * Build the string that instantiate a deserializer for the given type. If the type is a bean,
     * the implementation of {@link AbstractBeanJsonDeserializer} will
     * be created.
     *
     * @param type type
     * @param propertyInfo additionnal info to gives to the deserializer
     *
     * @return the code instantiating the deserializer. Examples: <ul><li>ctx.getIntegerDeserializer()</li><li>new org
     *         .PersonBeanJsonDeserializer()
     *         </li></ul>
     */
    protected String getDeserializerFromType( JType type, PropertyInfo propertyInfo ) throws UnableToCompleteException {
        JPrimitiveType primitiveType = type.isPrimitive();
        if ( null != primitiveType ) {
            String boxedName = getJavaObjectTypeFor( primitiveType );
            return "ctx.get" + boxedName + "JsonDeserializer()";
        }

        JEnumType enumType = type.isEnum();
        if ( null != enumType ) {
            return "ctx.newEnumJsonDeserializer(" + enumType.getQualifiedSourceName() + ".class)";
        }

        JArrayType arrayType = type.isArray();
        if ( null != arrayType ) {
            if ( null != arrayType.getComponentType().isPrimitive() ) {
                String boxedName = getJavaObjectTypeFor( arrayType.getComponentType().isPrimitive() );
                return "ctx.getPrimitive" + boxedName + "ArrayJsonDeserializer()";
            } else {
                String method = "ctx.newArrayJsonDeserializer(%s, %s)";
                String arrayCreator = "new " + ARRAY_CREATOR_CLASS + "<" + arrayType.getComponentType()
                    .getParameterizedQualifiedSourceName() + ">(){\n" +
                    "  @Override\n" +
                    "  public " + arrayType.getParameterizedQualifiedSourceName() + " create( int length ) {\n" +
                    "    return new " + arrayType.getComponentType().getParameterizedQualifiedSourceName() + "[length];\n" +
                    "  }\n" +
                    "}";
                return String.format( method, getDeserializerFromType( arrayType.getComponentType(), propertyInfo ), arrayCreator );
            }
        }

        JParameterizedType parameterizedType = type.isParameterized();
        if ( null != parameterizedType ) {
            String result;

            if ( typeOracle.isEnumSet( parameterizedType ) ) {
                result = "ctx.newEnumSetJsonDeserializer(" + parameterizedType.getTypeArgs()[0].getQualifiedSourceName() + ".class, %s)";
            } else if ( typeOracle.isIterable( parameterizedType ) ) {
                result = "ctx.new" + parameterizedType.getSimpleSourceName() + "JsonDeserializer(%s)";
            } else if ( typeOracle.isMap( parameterizedType ) ) {
                // TODO add support for map
                logger.log( TreeLogger.Type.ERROR, "Map are not supported yet" );
                throw new UnableToCompleteException();
            } else {
                // TODO
                logger.log( TreeLogger.Type.ERROR, "Parameterized type '" + parameterizedType
                    .getQualifiedSourceName() + "' is not supported" );
                throw new UnableToCompleteException();
            }

            JClassType[] args = parameterizedType.getTypeArgs();
            String[] mappers = new String[args.length];
            for ( int i = 0; i < args.length; i++ ) {
                mappers[i] = getDeserializerFromType( args[i], propertyInfo );
            }

            return String.format( result, mappers );
        }

        // TODO should we use isClassOrInterface ? need to add test for interface
        JClassType classType = type.isClass();
        if ( null != classType ) {
            String qualifiedSourceName = classType.getQualifiedSourceName();
            if ( BASE_TYPES.contains( qualifiedSourceName ) ) {
                if ( qualifiedSourceName.startsWith( "java.sql" ) ) {
                    return "ctx.getSql" + classType.getSimpleSourceName() + "JsonDeserializer()";
                } else {
                    return "ctx.get" + classType.getSimpleSourceName() + "JsonDeserializer()";
                }
            }

            // it's a bean
            BeanJsonDeserializerCreator beanJsonDeserializerCreator = new BeanJsonDeserializerCreator( logger
                .branch( TreeLogger.Type.INFO, "Creating deserializer for " + classType.getQualifiedSourceName() ), context, typeOracle );
            BeanJsonMapperInfo info = beanJsonDeserializerCreator.create( classType );
            return String.format( "new %s(%s)", info
                .getQualifiedDeserializerClassName(), generateBeanJsonDeserializerParameters( classType, info, propertyInfo ) );
        }

        logger.log( TreeLogger.Type.ERROR, "Type '" + type.getQualifiedSourceName() + "' is not supported" );
        throw new UnableToCompleteException();
    }

    private String generateBeanJsonDeserializerParameters( JClassType type, BeanJsonMapperInfo info,
                                                           PropertyInfo propertyInfo ) throws UnableToCompleteException {
        if ( null == propertyInfo || (null == propertyInfo.getIdentityInfo()) ) {
            return "";
        }

        StringSourceWriter sourceWriter = new StringSourceWriter();

        if ( null == propertyInfo.getIdentityInfo() ) {
            sourceWriter.print( "null" );
        } else {
            findIdPropertyInfo( info.getProperties(), propertyInfo.getIdentityInfo() );
            generateIdentifierDeserializationInfo( sourceWriter, type, propertyInfo.getIdentityInfo() );
        }

        sourceWriter.print( ", " );

        // TODO subtype info on property
        sourceWriter.print( "null" );

        return sourceWriter.toString();
    }

    protected void generateIdentifierDeserializationInfo( SourceWriter source, JClassType type, BeanIdentityInfo identityInfo ) throws
        UnableToCompleteException {
        String qualifiedType = getQualifiedClassName( identityInfo.getType() );

        String identityPropertyClass = String.format( "%s<%s>", IDENTITY_DESERIALIZATION_INFO_CLASS, qualifiedType );

        source.println( "new %s(\"%s\", %s.class, %s.class) {", identityPropertyClass, identityInfo.getPropertyName(), identityInfo
            .getGenerator().getCanonicalName(), identityInfo.getScope().getCanonicalName() );
        source.indent();

        source.println();
        source.println( "@Override" );
        source.println( "protected %s<%s> newDeserializer(%s ctx) {", JSON_DESERIALIZER_CLASS, qualifiedType, JSON_DECODING_CONTEXT_CLASS );
        source.indent();
        source.println( "return %s;", getDeserializerFromType( identityInfo.getType() ) );
        source.outdent();
        source.println( "}" );
        source.println();

        source.outdent();
        source.println( "}" );
    }

    protected void findIdPropertyInfo( Map<String, PropertyInfo> properties, BeanIdentityInfo identityInfo ) throws
        UnableToCompleteException {
        if ( null != identityInfo && identityInfo.isIdABeanProperty() ) {
            PropertyInfo property = properties.get( identityInfo.getPropertyName() );
            if ( null == property ) {
                logger.log( Type.ERROR, "Cannot find the property with the name '" + identityInfo
                    .getPropertyName() + "' used for identity" );
                throw new UnableToCompleteException();
            }
            identityInfo.setProperty( property );
        }
    }
}