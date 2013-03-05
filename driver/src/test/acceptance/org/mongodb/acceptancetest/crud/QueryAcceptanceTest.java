/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.acceptancetest.crud;

import org.bson.BSONReader;
import org.bson.BSONWriter;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.ConvertibleToDocument;
import org.mongodb.Document;
import org.mongodb.MongoCollection;
import org.mongodb.MongoCursor;
import org.mongodb.QueryBuilder;
import org.mongodb.acceptancetest.AcceptanceTestCase;
import org.mongodb.serialization.CollectibleSerializer;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.bson.BSONType.INT32;
import static org.bson.BSONType.INT64;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mongodb.QueryBuilder.query;
import static org.mongodb.QueryOperators.TYPE;
import static org.mongodb.Sort.ascending;
import static org.mongodb.Sort.descending;

public class QueryAcceptanceTest extends AcceptanceTestCase {
    @Test
    public void shouldBeAbleToQueryWithDocumentSpecification() {
        collection.insert(new Document("name", "Bob"));

        final Document query = new Document("name", "Bob");
        final MongoCursor<Document> results = collection.filter(query).all();

        assertThat(results.next().get("name").toString(), is("Bob"));
    }

    @Test
    public void shouldBeAbleToQueryWithDocument() {
        collection.insert(new Document("name", "Bob"));

        final Document query = new Document("name", "Bob");
        final MongoCursor<Document> results = collection.filter(query).all();

        assertThat(results.next().get("name").toString(), is("Bob"));
    }

    @Test
    public void shouldBeAbleToQueryTypedCollectionWithDocument() {
        final MongoCollection<Person> personCollection = database.getCollection(collectionName, new PersonSerializer());
        personCollection.insert(new Person("Bob"));

        final MongoCursor<Person> results = personCollection.filter(new Document("name", "Bob")).all();

        assertThat(results.next().name, is("Bob"));
    }

    @Test
    public void shouldBeAbleToQueryWithType() {
        final MongoCollection<Person> personCollection = database.getCollection(collectionName, new PersonSerializer());
        final Person bob = new Person("Bob");
        personCollection.insert(bob);

        final MongoCursor<Person> results = personCollection.filter(bob).all();

        assertThat(results.next().name, is("Bob"));
    }

    @Test
    public void shouldBeAbleToFilterByType() {
        collection.insert(new Document("product", "Book").append("numTimesOrdered", "some"));
        collection.insert(new Document("product", "CD").append("numTimesOrdered", "6"));
        collection.insert(new Document("product", "DVD").append("numTimesOrdered", 9));
        collection.insert(new Document("product", "SomethingElse").append("numTimesOrdered", 10));

        final List<Document> results = new ArrayList<Document>();
        collection.filter(new Document("numTimesOrdered", new Document("$type", 16)))
                  .sort(new Document("numTimesOrdered", -1))
                  .into(results);

        assertThat(results.size(), is(2));
        assertThat(results.get(0).get("product").toString(), is("SomethingElse"));
        assertThat(results.get(1).get("product").toString(), is("DVD"));
    }

    @Test
    public void shouldUseFriendlyQueryType() {
        collection.insert(new Document("product", "Book").append("numTimesOrdered", "some"));
        collection.insert(new Document("product", "CD").append("numTimesOrdered", "6"));
        collection.insert(new Document("product", "DVD").append("numTimesOrdered", 9));
        collection.insert(new Document("product", "SomethingElse").append("numTimesOrdered", 10));
        collection.insert(new Document("product", "VeryPopular").append("numTimesOrdered", 7843273657286478L));

        final List<Document> results = new ArrayList<Document>();
        //TODO make BSON type serializable
        final Document filter = new Document("$or", asList(new Document("numTimesOrdered", new Document("$type",
                                                                                                       INT32.getValue())),
                                                          new Document("numTimesOrdered", new Document("$type", INT64.getValue()))));
        collection.filter(filter)
                  .sort(descending("numTimesOrdered"))
                  .into(results);

        assertThat(results.size(), is(3));
        assertThat(results.get(0).get("product").toString(), is("VeryPopular"));
        assertThat(results.get(1).get("product").toString(), is("SomethingElse"));
        assertThat(results.get(2).get("product").toString(), is("DVD"));
    }

    @Test
    public void shouldBeAbleToSortAscending() {
        collection.insert(new Document("product", "Book"));
        collection.insert(new Document("product", "DVD"));
        collection.insert(new Document("product", "CD"));

        final List<Document> results = new ArrayList<Document>();
        collection.sort(ascending("product"))
                  .into(results);

        assertThat(results.size(), is(3));
        assertThat(results.get(0).get("product").toString(), is("Book"));
        assertThat(results.get(1).get("product").toString(), is("CD"));
        assertThat(results.get(2).get("product").toString(), is("DVD"));
    }

    @Test
    public void shouldBeAbleToUseQueryBuilderForFilter() {
        collection.insert(new Document("product", "Book").append("numTimesOrdered", "some"));
        collection.insert(new Document("product", "CD").append("numTimesOrdered", "6"));
        collection.insert(new Document("product", "DVD").append("numTimesOrdered", 9));
        collection.insert(new Document("product", "SomethingElse").append("numTimesOrdered", 10));
        collection.insert(new Document("product", "VeryPopular").append("numTimesOrdered", 7843273657286478L));

        final List<Document> results = new ArrayList<Document>();

        final Document filter = new QueryBuilder().or(query("numTimesOrdered").is(query(TYPE).is(INT32.getValue())))
                                                  .or(query("numTimesOrdered").is(query(TYPE).is(INT64.getValue())))
                                                  .toDocument();
        collection.filter(filter)
                  .sort(descending("numTimesOrdered"))
                  .into(results);

        assertThat(results.size(), is(3));
        assertThat(results.get(0).get("product").toString(), is("VeryPopular"));
        assertThat(results.get(1).get("product").toString(), is("SomethingElse"));
        assertThat(results.get(2).get("product").toString(), is("DVD"));
    }


    @Test
    @Ignore("JSON stuff not implemented")
    public void shouldBeAbleToQueryWithJSON() {
    }

    private class PersonSerializer implements CollectibleSerializer<Person> {
        @Override
        public Object getId(final Person person) {
            return person.id;
        }

        @Override
        public void serialize(final BSONWriter bsonWriter, final Person value) {
            bsonWriter.writeStartDocument();
            bsonWriter.writeObjectId("_id", value.id);
            bsonWriter.writeString("name", value.name);
            bsonWriter.writeEndDocument();
        }

        @Override
        public Person deserialize(final BSONReader reader) {
            reader.readStartDocument();
            final ObjectId id = reader.readObjectId("_id");
            final String name = reader.readString("name");
            reader.readEndDocument();
            return new Person(id, name);
        }

        @Override
        public Class<Person> getSerializationClass() {
            return Person.class;
        }
    }

    private class Person implements ConvertibleToDocument {
        private ObjectId id = new ObjectId();
        private final String name;

        public Person(final String name) {
            this.name = name;
        }

        public Person(final ObjectId id, final String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Document toDocument() {
            return new Document("name", name);
        }
    }

}
