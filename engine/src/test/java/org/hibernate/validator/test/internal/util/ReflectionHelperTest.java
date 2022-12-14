/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.test.internal.util;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.hibernate.validator.internal.util.ReflectionHelper;
import org.hibernate.validator.testutil.TestForIssue;
import org.testng.annotations.Test;

/**
 * Tests for the {@code ReflectionHelper}.
 *
 * @author Hardy Ferentschik
 * @author Guillaume Smet
 */
public class ReflectionHelperTest {

	@Test
	public void testIsIterable() throws Exception {
		Type type = TestTypes.class.getField( "stringList" ).getGenericType();
		assertTrue( ReflectionHelper.isIterable( type ) );

		assertTrue( ReflectionHelper.isIterable( TreeSet.class ) );

		assertTrue( ReflectionHelper.isIterable( List.class ) );
		assertTrue( ReflectionHelper.isIterable( HashSet.class ) );
		assertTrue( ReflectionHelper.isIterable( Iterable.class ) );
		assertTrue( ReflectionHelper.isIterable( Collection.class ) );

		assertFalse( ReflectionHelper.isIterable( null ) );
		assertFalse( ReflectionHelper.isIterable( Object.class ) );
	}

	@Test
	public void testIsCollection() throws Exception {
		assertTrue( ReflectionHelper.isCollection( Iterable.class ) );
		assertTrue( ReflectionHelper.isCollection( Collection.class ) );

		assertTrue( ReflectionHelper.isCollection( List.class ) );
		Type type = TestTypes.class.getField( "stringList" ).getGenericType();
		assertTrue( ReflectionHelper.isCollection( type ) );

		assertTrue( ReflectionHelper.isCollection( TreeSet.class ) );
		assertTrue( ReflectionHelper.isCollection( HashSet.class ) );
		type = TestTypes.class.getField( "floatSet" ).getGenericType();
		assertTrue( ReflectionHelper.isCollection( type ) );

		assertTrue( ReflectionHelper.isCollection( Map.class ) );
		assertTrue( ReflectionHelper.isCollection( SortedMap.class ) );
		type = TestTypes.class.getField( "objectMap" ).getGenericType();
		assertTrue( ReflectionHelper.isCollection( type ) );

		assertTrue( ReflectionHelper.isCollection( int[].class ) );
		assertTrue( ReflectionHelper.isCollection( String[].class ) );
		type = TestTypes.class.getField( "stringArray" ).getGenericType();
		assertTrue( ReflectionHelper.isCollection( type ) );
		type = TestTypes.class.getField( "intArray" ).getGenericType();
		assertTrue( ReflectionHelper.isCollection( type ) );

		assertFalse( ReflectionHelper.isCollection( null ) );
		assertFalse( ReflectionHelper.isCollection( Object.class ) );
	}

	@Test
	public void testGetCollectionElementType() throws Exception {
		Type type = TestTypes.class.getField( "stringList" ).getGenericType();
		assertThat( ReflectionHelper.getCollectionElementType( type ) ).isEqualTo( String.class );

		type = TestTypes.class.getField( "floatSet" ).getGenericType();
		assertThat( ReflectionHelper.getCollectionElementType( type ) ).isEqualTo( Float.class );

		type = TestTypes.class.getField( "stringArray" ).getGenericType();
		assertThat( ReflectionHelper.getCollectionElementType( type ) ).isEqualTo( String.class );

		type = TestTypes.class.getField( "objectMap" ).getGenericType();
		assertThat( ReflectionHelper.getCollectionElementType( type ) ).isEqualTo( Object.class );

		type = TestTypes.class.getField( "intArray" ).getGenericType();
		assertThat( ReflectionHelper.getCollectionElementType( type ) ).isEqualTo( int.class );
	}

	@Test
	public void testIsMap() throws Exception {
		assertTrue( ReflectionHelper.isMap( Map.class ) );
		assertTrue( ReflectionHelper.isMap( SortedMap.class ) );

		Type type = TestTypes.class.getField( "objectMap" ).getGenericType();
		assertTrue( ReflectionHelper.isMap( type ) );

		assertFalse( ReflectionHelper.isMap( null ) );
		assertFalse( ReflectionHelper.isMap( Object.class ) );
	}

	@Test
	public void testGetIndexedValueForMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		Object testObject = new Object();
		String key = "key";
		map.put( key, testObject );

		Object value = ReflectionHelper.getMappedValue( map, key );
		assertEquals( value, testObject, "We should be able to retrieve the indexed object" );

		value = ReflectionHelper.getMappedValue( map, "foo" );
		assertNull( value, "A non existent index should return the null value" );

		value = ReflectionHelper.getMappedValue( map, "2" );
		assertNull( value, "A non existent index should return the null value" );
	}

	@Test
	public void testGetIndexedValueForList() {
		List<Object> list = new ArrayList<Object>();
		Object testObject = new Object();
		list.add( testObject );

		Object value = ReflectionHelper.getIndexedValue( list, 0 );
		assertEquals( value, testObject, "We should be able to retrieve the indexed object" );

		value = ReflectionHelper.getIndexedValue( list, 2 );
		assertNull( value, "A non existent index should return the null value" );
	}

	@Test
	public void testGetIndexedValueForNull() {
		Object value = ReflectionHelper.getIndexedValue( null, 0 );
		assertNull( value );
	}

	@Test
	@TestForIssue(jiraKey = "HV-622")
	public void testIsGetterMethod() throws Exception {
		Method method = Bar.class.getMethod( "getBar" );
		assertTrue( ReflectionHelper.isGetterMethod( method ) );

		method = Bar.class.getMethod( "getBar", String.class );
		assertFalse( ReflectionHelper.isGetterMethod( method ) );
	}

	@SuppressWarnings("unused")
	private static class TestTypes {
		public List<String> stringList;
		public Set<Float> floatSet;
		public Map<String, Object> objectMap;
		public String[] stringArray;
		public int[] intArray;
	}

	@SuppressWarnings("unused")
	private static class Bar {
		public String getBar() {
			return null;
		}

		public String getBar(String param) {
			return null;
		}
	}
}
