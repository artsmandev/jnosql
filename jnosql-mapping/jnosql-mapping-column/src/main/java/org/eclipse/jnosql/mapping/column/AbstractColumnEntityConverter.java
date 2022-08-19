/*
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.mapping.column;

import jakarta.nosql.column.Column;
import jakarta.nosql.column.ColumnEntity;
import jakarta.nosql.mapping.Converters;
import jakarta.nosql.mapping.MappingException;
import jakarta.nosql.mapping.column.ColumnEntityConverter;
import org.eclipse.jnosql.mapping.column.ColumnFieldConverters.ColumnFieldConverterFactory;
import org.eclipse.jnosql.mapping.reflection.EntityMetadata;
import org.eclipse.jnosql.mapping.reflection.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.FieldMapping;
import org.eclipse.jnosql.mapping.reflection.EntityType;
import org.eclipse.jnosql.mapping.reflection.FieldValue;
import org.eclipse.jnosql.mapping.reflection.InheritanceMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jnosql.mapping.reflection.EntityType.EMBEDDED;
import static org.eclipse.jnosql.mapping.reflection.EntityType.ENTITY;



/**
 * Template method to {@link ColumnEntityConverter}
 */
public abstract class AbstractColumnEntityConverter implements ColumnEntityConverter {

    private final ColumnFieldConverterFactory converterFactory = new ColumnFieldConverterFactory();

    protected abstract EntitiesMetadata getEntities();

    protected abstract Converters getConverters();

    @Override
    public ColumnEntity toColumn(Object entity) {
        requireNonNull(entity, "entity is required");
        EntityMetadata mapping = getEntities().get(entity.getClass());
        ColumnEntity communication = ColumnEntity.of(mapping.getName());
        mapping.getFields().stream()
                .map(f -> to(f, entity))
                .filter(FieldValue::isNotEmpty)
                .map(f -> f.toColumn(this, getConverters()))
                .flatMap(List::stream)
                .forEach(communication::add);

        mapping.getInheritance().ifPresent(i -> communication.add(i.getDiscriminatorColumn(), i.getDiscriminatorValue()));
        return communication;
    }

    @Override
    public <T> T toEntity(Class<T> entity, ColumnEntity communication) {
        requireNonNull(communication, "communication is required");
        requireNonNull(entity, "entity is required");
        return toEntity(entity, communication.getColumns());
    }

    @Override
    public <T> T toEntity(T entity, ColumnEntity communication) {
        requireNonNull(communication, "communication is required");
        requireNonNull(entity, "entity is required");
        EntityMetadata mapping = getEntities().get(entity.getClass());
        return convertEntity(communication.getColumns(), mapping, entity);
    }

    @Override
    public <T> T toEntity(ColumnEntity entity) {
        requireNonNull(entity, "entity is required");
        EntityMetadata mapping = getEntities().findByName(entity.getName());
        if (mapping.isInheritance()) {
            return mapInheritanceEntity(entity, mapping.getType());
        }
        T instance = mapping.newInstance();
        return convertEntity(entity.getColumns(), mapping, instance);
    }

    protected ColumnFieldValue to(FieldMapping field, Object entityInstance) {
        Object value = field.read(entityInstance);
        return DefaultColumnFieldValue.of(value, field);
    }

    protected <T> Consumer<String> feedObject(T instance, List<Column> columns, Map<String, FieldMapping> fieldsGroupByName) {
        return (String k) -> {
            Optional<Column> column = columns.stream().filter(c -> c.getName().equals(k)).findFirst();
            FieldMapping field = fieldsGroupByName.get(k);
            ColumnFieldConverter fieldConverter = converterFactory.get(field);
            if (ENTITY.equals(field.getType())) {
                column.ifPresent(c -> fieldConverter.convert(instance,  c, field, this));
            } else {
                fieldConverter.convert(instance, columns, column.orElse(null), field, this);
            }
        };
    }


    protected <T> T toEntity(Class<T> entityClass, List<Column> columns) {
        EntityMetadata mapping = getEntities().get(entityClass);
        if (mapping.isInheritance()) {
            return inheritanceToEntity(columns, mapping);
        }
        T instance = mapping.newInstance();
        return convertEntity(columns, mapping, instance);
    }

    private <T> T convertEntity(List<Column> columns, EntityMetadata mapping, T instance) {
        final Map<String, FieldMapping> fieldsGroupByName = mapping.getFieldsGroupByName();
        final List<String> names = columns.stream().map(Column::getName).sorted().collect(Collectors.toList());
        final Predicate<String> existField = k -> Collections.binarySearch(names, k) >= 0;
        final Predicate<String> isElementType = k -> {
            EntityType type = fieldsGroupByName.get(k).getType();
            return EMBEDDED.equals(type) || ENTITY.equals(type);
        };
        fieldsGroupByName.keySet().stream()
                .filter(existField.or(isElementType))
                .forEach(feedObject(instance, columns, fieldsGroupByName));

        return instance;
    }

    private <T> T mapInheritanceEntity(ColumnEntity entity, Class<?> entityClass) {
        Map<String, InheritanceMetadata> group = getEntities()
                .findByParentGroupByDiscriminatorValue(entityClass);

        if (group.isEmpty()) {
            throw new MappingException("There is no discriminator inheritance to the document collection "
                    + entity.getName());
        }
        String column = group.values()
                .stream()
                .findFirst()
                .map(InheritanceMetadata::getDiscriminatorColumn)
                .orElseThrow();

        String discriminator = entity.find(column, String.class)
                .orElseThrow(
                        () -> new MappingException("To inheritance there is the discriminator column missing" +
                                " on the Document Collection, the document name: " + column));

        InheritanceMetadata inheritance = Optional.ofNullable(group.get(discriminator))
                .orElseThrow(() -> new MappingException("There is no inheritance map to the discriminator" +
                        " column value " + discriminator));

        EntityMetadata mapping = getEntities().get(inheritance.getEntity());
        T instance = mapping.newInstance();
        return convertEntity(entity.getColumns(), mapping, instance);
    }

    private <T> T inheritanceToEntity(List<Column> columns, EntityMetadata mapping) {
        Map<String, InheritanceMetadata> group = getEntities()
                .findByParentGroupByDiscriminatorValue(mapping.getType());

        if (group.isEmpty()) {
            throw new MappingException("There is no discriminator inheritance to the document collection "
                    + mapping.getName());
        }

        String column = group.values()
                .stream()
                .findFirst()
                .map(InheritanceMetadata::getDiscriminatorColumn)
                .orElseThrow();

        String discriminator = columns.stream()
                .filter(d -> d.getName().equals(column))
                .findFirst()
                .map(d -> d.get(String.class))
                .orElseThrow(
                        () -> new MappingException("To inheritance there is the discriminator column missing" +
                                " on the Document Collection, the document name: " + column));

        InheritanceMetadata inheritance = Optional.ofNullable(group.get(discriminator))
                .orElseThrow(() -> new MappingException("There is no inheritance map to the discriminator" +
                        " column value " + discriminator));

        EntityMetadata inheritanceMetadata = getEntities().get(inheritance.getEntity());
        T instance = inheritanceMetadata.newInstance();
        return convertEntity(columns, inheritanceMetadata, instance);
    }
}
