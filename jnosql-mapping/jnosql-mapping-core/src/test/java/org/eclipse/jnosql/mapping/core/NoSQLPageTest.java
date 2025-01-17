/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.mapping.core;

import jakarta.data.page.Page;
import jakarta.data.page.Pageable;
import org.eclipse.jnosql.mapping.core.entities.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoSQLPageTest {


    @Test
    void shouldReturnErrorWhenNull() {
        assertThrows(NullPointerException.class, ()->
                NoSQLPage.of(Collections.emptyList(), null));

        assertThrows(NullPointerException.class, ()->
                NoSQLPage.of(null, Pageable.ofPage(2)));
    }


    @Test
    void shouldReturnUnsupportedOperation() {
        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));

        assertThrows(UnsupportedOperationException.class, page::totalPages);

        assertThrows(UnsupportedOperationException.class, page::totalElements);
    }

    @Test
    void shouldReturnHasContent() {

        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));

        Assertions.assertTrue(page.hasContent());
        page = NoSQLPage.of(Collections.emptyList(),
                Pageable.ofPage(2));
        Assertions.assertFalse(page.hasContent());
    }

    @Test
    void shouldNumberOfElements() {

        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));

        assertEquals(1, page.numberOfElements());
    }

    @Test
    void shouldIterator() {
        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));
        Assertions.assertNotNull(page.iterator());
    }

    @Test
    void shouldPageable() {
        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));
        Pageable pageable = page.pageable();
        Assertions.assertNotNull(pageable);
        assertEquals(Pageable.ofPage(2), pageable);
    }

    @Test
    void shouldNextPageable() {
        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));
        Pageable pageable = page.nextPageable();
        assertEquals(Pageable.ofPage(3), pageable);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPageableIsNull() {
        assertThrows(NullPointerException.class, () -> NoSQLPage.skip(null));
    }

    @Test
    void shouldCalculateSkip() {
        long skipValue = NoSQLPage.skip(Pageable.ofPage(2).size(10));
        assertEquals(10, skipValue);
    }

    @Test
    void shouldCalculateSkipForFirstPage() {
        // Create a pageable with page=1 and size=5
        long skipValue = NoSQLPage.skip(Pageable.ofPage(1).size(5));
        assertEquals(0, skipValue);
    }

    @Test
    void shouldToString(){
        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));

        assertThat(page.toString()).isNotBlank();
    }

    @Test
    void shouldEqualsHasCode(){
        Page<Person> page = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));
        Page<Person> page2 = NoSQLPage.of(Collections.singletonList(Person.builder().withName("Otavio").build()),
                Pageable.ofPage(2));

        assertEquals(page, page2);
        assertEquals(page.hashCode(), page2.hashCode());

    }
}