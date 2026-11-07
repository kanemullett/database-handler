package com.kanemullett.mapper;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.Record;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Maps flat jOOQ {@link Record} instances onto Immutables-style domain
 * interfaces (getter-only, backed by a generated {@code Immutable<Name>}
 * builder).
 *
 * <p>jOOQ's built-in record mapping only knows how to populate classic
 * getter/setter POJOs automatically. Since our domain model has no setters,
 * this mapper instead resolves each Immutables builder via reflection and
 * invokes its generated builder methods directly.
 *
 * <p>For each column in the record: if the target type declares a matching
 * property, its value is set via the builder. If not, the column's name and
 * value are placed into {@code DatabaseRecord}'s {@code data} map instead of
 * being silently dropped, so no information returned from a query is lost
 * even if the target type doesn't model every selected column.
 *
 * <p>Column aliases are purely cosmetic here - useful for disambiguating
 * columns with the same physical name across joined tables - and carry no
 * special meaning for mapping; matching is done purely by name against the
 * target type's declared properties.
 *
 * <p>Collection, map and nested object properties are expected to be stored
 * as JSON ({@code jsonb}) and are deserialised with Jackson using the
 * property's full generic type, e.g. {@code List<RoundTemplate>}.
 */
public class DatabaseRecordMapper {

    private static final String DATA = "data";

    // JavaTimeModule parses the ISO-8601 strings UpdateBuilderFunction writes
    // for java.time values nested inside jsonb columns. Offsets are kept as
    // stored rather than normalised to UTC, so values round-trip unchanged.
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    /**
     * Maps a single record into an instance of the given Immutables
     * interface type.
     *
     * @param jooqRecord the flat jOOQ record to map.
     * @param type the Immutables interface type to build. Must have a
     *             corresponding generated {@code Immutable<Name>} class in
     *             the same package.
     * @param <T> the type of object to build.
     * @return the constructed object, with any unmatched columns available
     *         via its {@code data} map.
     * @throws IllegalStateException if the record cannot be mapped into the
     *                                given type, e.g. because no generated
     *                                {@code Immutable<Name>} class could be
     *                                found or its builder could not be
     *                                introspected.
     */
    public <T> T map(Record jooqRecord, Class<T> type) {
        try {
            final Class<?> implClass = Class.forName(type.getPackageName() + ".Immutable" + type.getSimpleName());
            final Object builder = implClass.getMethod("builder").invoke(null);
            final Class<?> builderClass = builder.getClass();

            final Map<String, PropertyDescriptor> propertiesByName = new LinkedHashMap<>();
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                propertiesByName.put(descriptor.getName(), descriptor);
            }

            for (Field<?> field : jooqRecord.fields()) {
                final String name = field.getName();
                final Object value = jooqRecord.get(field);

                // Null columns are left unset on the builder: @Nullable
                // properties stay null, @Default properties take their
                // default, and Immutables' maps and non-null setters would
                // otherwise reject the null with an NPE.
                if (value == null) {
                    continue;
                }

                if (propertiesByName.containsKey(name)) {
                    setBuilderProperty(builderClass, builder, propertiesByName.get(name), value);
                } else {
                    putData(builderClass, builder, name, value);
                }
            }

            return type.cast(builderClass.getMethod("build").invoke(builder));
        } catch (ReflectiveOperationException | IntrospectionException e) {
            throw new IllegalStateException("Unable to map record into " + type, e);
        }
    }

    /**
     * Sets a single property on the given Immutables builder, coercing the
     * value's type if needed to match the builder method's declared
     * parameter type.
     *
     * <p>If no builder method matching the property name is found, the
     * value is silently skipped.
     *
     * @param builderClass the builder's class, used to locate the setter
     *                     method via reflection.
     * @param builder the builder instance to invoke the setter method on.
     * @param property the property to set; its name matches the builder
     *                 method's name and its getter's generic return type is
     *                 used when deserialising JSON values.
     * @param value the value to set, coerced to the target parameter type
     *              if necessary.
     * @throws ReflectiveOperationException if the matching builder method
     *                                       cannot be invoked.
     */
    private void setBuilderProperty(Class<?> builderClass, Object builder, PropertyDescriptor property, Object value) throws ReflectiveOperationException {

        final Type genericType = property.getReadMethod() != null
            ? property.getReadMethod().getGenericReturnType()
            : property.getPropertyType();

        for (Method method : builderClass.getMethods()) {
            if (method.getName().equals(property.getName()) && method.getParameterCount() == 1) {
                method.invoke(builder, coerce(value, method.getParameterTypes()[0], genericType));
                return;
            }
        }
    }

    /**
     * Places a single unmatched column's name and value into the builder's
     * {@code data} map, via its generated {@code putData(key, value)}
     * builder method.
     *
     * <p>If the target type's builder has no such method - e.g. because its
     * type doesn't extend {@code DatabaseRecord} - the value is silently
     * skipped.
     *
     * <p>Callers must not pass {@code null} values: Immutables maps reject
     * them. Null unmatched columns are omitted from the {@code data} map,
     * so {@code getData().get(key)} still returns {@code null} for them.
     *
     * @param builderClass the builder's class, used to locate the
     *                     {@code putData} method via reflection.
     * @param builder the builder instance to invoke {@code putData} on.
     * @param key the unmatched column's name.
     * @param value the unmatched column's value.
     * @throws ReflectiveOperationException if the {@code putData} method
     *                                       cannot be invoked.
     */
    private void putData(Class<?> builderClass, Object builder, String key, Object value) throws ReflectiveOperationException {

        for (Method method : builderClass.getMethods()) {
            if (method.getName().equals("put" + capitalize(DATA)) && method.getParameterCount() == 2) {
                method.invoke(builder, key, value);
                return;
            }
        }
    }

    /**
     * Capitalizes the first letter of the given string, used to build the
     * generated {@code putData} builder method name from the {@link #DATA}
     * constant.
     *
     * @param value the string to capitalize.
     * @return the capitalized string.
     */
    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * Coerces a value to the given target type where a straightforward,
     * lossless conversion is possible (e.g. a {@code Long} column value
     * being set onto an {@code int} property), so that minor type mismatches
     * between the database driver's returned type and the domain property's
     * declared type don't cause a reflective invocation failure.
     *
     * <p>If the value is already an instance of the target type, or no
     * known conversion applies, it is returned unchanged.
     *
     * <p>JSON column values ({@link JSON} / {@link JSONB}), and strings
     * being set onto collection or map properties, are deserialised into
     * the property's generic type.
     *
     * @param value the value to coerce.
     * @param targetType the builder parameter type to coerce the value to.
     * @param genericType the property's full generic type, used when
     *                    deserialising JSON.
     * @return the coerced value.
     */
    private Object coerce(Object value, Class<?> targetType, Type genericType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return value.toString();
        }

        final String json = jsonText(value, targetType);
        if (json != null) {
            return fromJson(json, genericType);
        }
        if (value instanceof java.util.Date legacyTemporal) {
            return coerceLegacyTemporal(legacyTemporal, targetType);
        }
        if (value instanceof Number number) {
            return coerceNumber(number, targetType);
        }
        if (targetType.isEnum() && value instanceof String name) {
            return toEnumConstant(name, targetType);
        }
        return value;
    }

    /**
     * Returns the JSON text to deserialise for a value, if it should be
     * treated as JSON: {@link JSON} / {@link JSONB} column values, or a
     * string being set onto a collection or map property.
     *
     * @param value the column value.
     * @param targetType the builder parameter type.
     * @return the JSON text, or {@code null} if the value isn't JSON.
     */
    private String jsonText(Object value, Class<?> targetType) {
        if (value instanceof JSONB jsonb) {
            return jsonb.data();
        }
        if (value instanceof JSON json) {
            return json.data();
        }
        final boolean structuredTarget = Iterable.class.isAssignableFrom(targetType)
            || Map.class.isAssignableFrom(targetType);
        return structuredTarget && value instanceof String string ? string : null;
    }

    /**
     * Converts a legacy JDBC temporal value ({@link Timestamp},
     * {@link java.sql.Date}, {@link Time}) to its {@code java.time}
     * equivalent. Depending on how a query is built, jOOQ can hand these
     * back instead of {@code java.time} types.
     *
     * @param value the legacy temporal value.
     * @param targetType the builder parameter type.
     * @return the converted value, or the original if no conversion applies.
     */
    private Object coerceLegacyTemporal(java.util.Date value, Class<?> targetType) {
        if (value instanceof Timestamp timestamp && targetType == LocalDateTime.class) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof Timestamp timestamp && targetType == Instant.class) {
            return timestamp.toInstant();
        }
        if (value instanceof java.sql.Date date && targetType == LocalDate.class) {
            return date.toLocalDate();
        }
        if (value instanceof Time time && targetType == LocalTime.class) {
            return time.toLocalTime();
        }
        return value;
    }

    /**
     * Converts a numeric value to the target numeric type, e.g. a
     * {@code Long} column value being set onto an {@code int} property.
     *
     * @param value the numeric value.
     * @param targetType the builder parameter type.
     * @return the converted value, or the original if no conversion applies.
     */
    private Object coerceNumber(Number value, Class<?> targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return value.intValue();
        }
        if (targetType == long.class || targetType == Long.class) {
            return value.longValue();
        }
        if (targetType == double.class || targetType == Double.class) {
            return value.doubleValue();
        }
        return value;
    }

    /**
     * Deserialises a JSON string into the given generic type.
     *
     * @param json the JSON string.
     * @param type the type to deserialise into, e.g. {@code List<RoundTemplate>}.
     * @return the deserialised value.
     * @throws IllegalArgumentException if the JSON cannot be deserialised
     *                                  into the given type.
     */
    private Object fromJson(String json, Type type) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialise JSON into " + type.getTypeName(), e);
        }
    }

    /**
     * Resolves the enum constant of the given type matching the given name.
     * An exact match on the constant's name is preferred; failing that, a
     * case-insensitive match is accepted, since database enum labels are
     * commonly lower-case while Java constants are upper-case.
     *
     * @param name the column value to resolve.
     * @param enumType the enum type to resolve the constant from.
     * @return the matching enum constant.
     * @throws IllegalArgumentException if no constant matches the name.
     */
    private Object toEnumConstant(String name, Class<?> enumType) {
        Object caseInsensitiveMatch = null;
        for (Object constant : enumType.getEnumConstants()) {
            final String constantName = ((Enum<?>) constant).name();
            if (constantName.equals(name)) {
                return constant;
            }
            if (caseInsensitiveMatch == null && constantName.equalsIgnoreCase(name)) {
                caseInsensitiveMatch = constant;
            }
        }
        if (caseInsensitiveMatch != null) {
            return caseInsensitiveMatch;
        }
        throw new IllegalArgumentException("No constant of " + enumType.getName() + " matches value '" + name + "'");
    }
}
