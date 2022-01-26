/*
 * ============================================================================
 *  Copyright © 2002-2022 by Thomas Thrien.
 *  All Rights Reserved.
 * ============================================================================
 *  Licensed to the public under the agreements of the GNU Lesser General Public
 *  License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *       http://www.gnu.org/licenses/lgpl.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */

package org.tquadrat.foundation.mgmt;

import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tquadrat.foundation.mgmt.JMXUtils.composeObjectName;
import static org.tquadrat.foundation.testutil.TestUtils.EMPTY_STRING;

import org.apiguardian.api.API;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.EmptyArgumentException;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.lang.NameValuePair;
import org.tquadrat.foundation.testutil.TestBaseClass;

/**
 *  Some tests for the class
 *  {@link JMXUtils}.
 *
 *  @version $Id: TestJMXUtils.java 995 2022-01-23 01:09:35Z tquadrat $
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 */
@ClassVersion( sourceVersion = "$Id: TestJMXUtils.java 995 2022-01-23 01:09:35Z tquadrat $" )
@API( status = STABLE, since = "0.1.0" )
@DisplayName( "org.tquadrat.foundation.mgmt.TestJMXUtils" )
public class TestJMXUtils extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Several tests for
     *  {@link JMXUtils#composeObjectName(String, String...)}
     *  and
     *  {@link JMXUtils#composeObjectName(String, String, String, Class, String...)}.
     *
     *  @throws Exception   Something went wrong unexpectedly.
     */
    @Test
    final void testComposeObjectName() throws Exception
    {
        skipThreadTest();

        @SuppressWarnings( "unchecked" )
        final NameValuePair<String> [] EMPTY_NameValuePair_ARRAY = new NameValuePair [0];
        final NameValuePair<String> [] NULL_NameValuePair_ARRAY = null;
        final var nvp = new NameValuePair<>( "name", "value" );

        assertThrows( NullArgumentException.class, () -> composeObjectName( null, nvp ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( EMPTY_STRING, nvp ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", NULL_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", EMPTY_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName" ) );

        assertThrows( NullArgumentException.class, () -> composeObjectName( null, "type", "function", getClass(), EMPTY_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( null, "type", "function", null, EMPTY_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( null, "type", "function", getClass(), NULL_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( null, "type", "function", null, NULL_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( null, "type", "function", getClass() ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( null, "type", "function", null ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( EMPTY_STRING, "type", "function", getClass(), EMPTY_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( EMPTY_STRING, "type", "function", null, EMPTY_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( EMPTY_STRING, "type", "function", getClass() ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( EMPTY_STRING, "type", "function", null ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( EMPTY_STRING, "type", "function", getClass(), NULL_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( EMPTY_STRING, "type", "function", null, NULL_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", null, "function", getClass(), EMPTY_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", null, "function", null, EMPTY_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", null, "function", getClass() ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", null, "function", null ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", null, "function", getClass(), NULL_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", null, "function", null, NULL_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", EMPTY_STRING, "function", getClass(), EMPTY_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", EMPTY_STRING, "function", null, EMPTY_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", EMPTY_STRING, "function", getClass() ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", EMPTY_STRING, "function", null ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", EMPTY_STRING, "function", getClass(), NULL_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", EMPTY_STRING, "function", null, NULL_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", "type", null, getClass(), EMPTY_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", "type", null, null, EMPTY_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", "type", null, getClass() ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", "type", null, null ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", "type", null, getClass(), NULL_NameValuePair_ARRAY ) );
        assertThrows( NullArgumentException.class, () -> composeObjectName( "domainName", "type", null, null, NULL_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", "type", EMPTY_STRING, getClass(), EMPTY_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", "type", EMPTY_STRING, null, EMPTY_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", "type", EMPTY_STRING, getClass() ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", "type", EMPTY_STRING, null ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", "type", EMPTY_STRING, getClass(), NULL_NameValuePair_ARRAY ) );
        assertThrows( EmptyArgumentException.class, () -> composeObjectName( "domainName", "type", EMPTY_STRING, null, NULL_NameValuePair_ARRAY ) );

        var candidate = composeObjectName( "domainName", nvp );
        assertNotNull( candidate );
        assertEquals( "domainName:name=value", candidate.getCanonicalName() );

        candidate = composeObjectName( "domainName", "type", "function", null );
        assertNotNull( candidate );
        assertEquals( "domainName:function=function,type=type", candidate.getCanonicalName() );

        candidate = composeObjectName( "domainName", "type", "function", null, nvp );
        assertNotNull( candidate );
        assertEquals( "domainName:function=function,name=value,type=type", candidate.getCanonicalName() );

        assertThrows( IllegalArgumentException.class, () -> composeObjectName( "domainName", new NameValuePair<>( "name", null ) ) );
    }   //  testComposeObjectName()

    /**
     *  Validates whether the class is static.
     */
    @Test
    final void validateClass()
    {
        assertTrue( validateAsStaticClass( JMXUtils.class ) );
    }   //  validateClass()
}
//  class TestJMXUtils

/*
 *  End of File
 */