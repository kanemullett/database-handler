package com.kanemullett.mapper;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jooq.Field;
import org.jooq.Record;
import org.springframework.stereotype.Component;

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
 */
@Component
public class DatabaseRecordMapper {

    private static final String DATA = "data";

    /**
     * Maps a single record into an instance of the given Immutables
     * interface type.
     *
     * @param record the flat jOOQ record to map.
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
    public <T> T map(Record record, Class<T> type) {
        try {
            final Class<?> implClass = Class.forName(type.getPackageName() + ".Immutable" + type.getSimpleName());
            final Object builder = implClass.getMethod("builder").invoke(null);
            final Class<?> builderClass = builder.getClass();

            final Map<String, PropertyDescriptor> propertiesByName = new LinkedHashMap<>();
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                propertiesByName.put(descriptor.getName(), descriptor);
            }

            for (Field<?> field : record.fields()) {
                final String name = field.getName();
                final Object value = record.get(field);

                if (propertiesByName.containsKey(name)) {
                    setBuilderProperty(builderClass, builder, name, value);
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
     * @param propertyName the name of the property to set, matching the
     *                     builder method's name.
     * @param value the value to set, coerced to the target parameter type
     *              if necessary.
     * @throws ReflectiveOperationException if the matching builder method
     *                                       cannot be invoked.
     */
    private void setBuilderProperty(Class<?> builderClass, Object builder, String propertyName, Object value) throws ReflectiveOperationException {

        for (Method method : builderClass.getMethods()) {
            if (method.getName().equals(propertyName) && method.getParameterCount() == 1) {
                method.invoke(builder, coerce(value, method.getParameterTypes()[0]));
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
     * @param value the value to coerce.
     * @param targetType the type to coerce the value to.
     * @return the coerced value.
     */
    private Object coerce(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        if ((targetType == int.class || targetType == Integer.class) && value instanceof Number number) {
            return number.intValue();
        }
        if ((targetType == long.class || targetType == Long.class) && value instanceof Number number) {
            return number.longValue();
        }
        if ((targetType == double.class || targetType == Double.class) && value instanceof Number number) {
            return number.doubleValue();
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }
}
