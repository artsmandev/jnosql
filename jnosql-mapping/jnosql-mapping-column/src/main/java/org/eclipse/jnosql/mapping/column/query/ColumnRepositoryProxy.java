/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.mapping.column.query;


import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.column.JNoSQLColumnTemplate;
import org.eclipse.jnosql.mapping.core.query.AbstractRepository;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;


/**
 * Proxy handler to generate {@link jakarta.data.repository.PageableRepository} for column-based repositories.
 *
 * @param <T> The entity type managed by the repository.
 * @param <K> The key type used for column-based operations.
 */
public class ColumnRepositoryProxy<T, K> extends AbstractColumnRepositoryProxy<T, K> {

    private final JNoSQLColumnTemplate template;

    private final ColumnRepository<T, K> repository;

    private final EntityMetadata entityMetadata;

    private final Converters converters;

    private final Class<?> repositoryType;


    ColumnRepositoryProxy(JNoSQLColumnTemplate template, EntitiesMetadata entities, Class<?> repositoryType,
                          Converters converters) {
        this.template = template;
        Class<T> typeClass = (Class) ((ParameterizedType) repositoryType.getGenericInterfaces()[0])
                .getActualTypeArguments()[0];
        this.entityMetadata = entities.get(typeClass);
        this.repository = new ColumnRepository<>(template, entityMetadata);
        this.converters = converters;
        this.repositoryType =  repositoryType;
    }

    @Override
    protected AbstractRepository<T, K> repository() {
        return repository;
    }

    @Override
    protected EntityMetadata entityMetadata() {
        return entityMetadata;
    }

    @Override
    protected JNoSQLColumnTemplate template() {
        return template;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    protected Class<?> repositoryType() {
        return repositoryType;
    }


    /**
     * Repository implementation for column-based repositories.
     *
     * @param <T> The entity type managed by the repository.
     * @param <K> The key type used for column-based operations.
     */
    public static class ColumnRepository<T, K> extends AbstractColumnRepository<T, K> {

        private final JNoSQLColumnTemplate template;

        private final EntityMetadata entityMetadata;

        ColumnRepository(JNoSQLColumnTemplate template, EntityMetadata entityMetadata) {
            this.template = template;
            this.entityMetadata = entityMetadata;
        }

        @Override
        protected JNoSQLColumnTemplate template() {
            return template;
        }

        @Override
        protected EntityMetadata entityMetadata() {
            return entityMetadata;
        }

        /**
         * Creates a new instance of ColumnRepository.
         *
         * @param <T>      The entity type managed by the repository.
         * @param <K>      The key type used for column-based operations.
         * @param template The JNoSQLColumnTemplate used for column database operations. Must not be {@code null}.
         * @param metadata The metadata of the entity. Must not be {@code null}.
         * @return A new instance of ColumnRepository.
         * @throws NullPointerException If either the template or metadata is {@code null}.
         */
        public static <T, K> ColumnRepository<T, K> of(JNoSQLColumnTemplate template, EntityMetadata metadata) {
            Objects.requireNonNull(template,"template is required");
            Objects.requireNonNull(metadata,"metadata is required");
            return new ColumnRepository<>(template, metadata);
        }
    }
}
