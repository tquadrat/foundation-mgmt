/*
 * ============================================================================
 * Copyright © 2002-2023 by Thomas Thrien.
 * All Rights Reserved.
 * ============================================================================
 * Licensed to the public under the agreements of the GNU Lesser General Public
 * License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.tquadrat.foundation.mgmt;

import static java.lang.String.format;
import static org.apiguardian.api.API.Status.STABLE;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_CHARSEQUENCE;
import static org.tquadrat.foundation.lang.DebugOutput.ifDebug;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNotEmptyArgument;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.annotation.UtilityClass;
import org.tquadrat.foundation.exception.PrivateConstructorForStaticClassCalledError;
import org.tquadrat.foundation.lang.NameValuePair;

/**
 *  This class provides some utilities that are useful in the context of JMX.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: JMXUtils.java 1070 2023-09-29 17:09:34Z tquadrat $
 *  @since 0.0.1
 *
 *  @UMLGraph.link
 */
@UtilityClass
@ClassVersion( sourceVersion = "$Id: JMXUtils.java 1070 2023-09-29 17:09:34Z tquadrat $" )
@API( status = STABLE, since = "0.0.1" )
public final class JMXUtils
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  The property name for the connector address: {@value}.
     */
    public static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    /**
     *  The name of the JMX domain that is used by all JMX enabled components
     *  of the library.
     */
    public static final String JMX_DOMAIN = "org.tquadrat";

    /**
     *  The property name for the class of an MBean: {@value}.
     */
    public static final String MBEAN_CLASS = "class";

    /**
     *  The property name for the function of an MBean: {@value}.
     */
    public static final String MBEAN_FUNCTION = "function";

    /**
     *  The property name for the loader of an MBean: {@value}.
     */
    public static final String MBEAN_LOADER = "loader";

    /**
     *  The property name for the name of an MBean: {@value}.
     */
    public static final String MBEAN_NAME = "name";

    /**
     *  The property name for the MBean type: {@value}.
     */
    public static final String MBEAN_TYPE = "type";

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  No instance allowed for this class.
     */
    private JMXUtils() { throw new PrivateConstructorForStaticClassCalledError( JMXUtils.class ); }

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  <p>{@summary Composes an object name from the given domain name and the
     *  given properties.}</p>
     *  <p>The object name has the form</p>
     *  <pre><code>    &lt;Domain&gt;:type=&lt;Type&gt;,function=&lt;Function&gt;<b>[</b>, class=&lt;Class&gt;<b>]</b><b>[</b>,…<b>]</b></code></pre>
     *  <p>The type is something like a category.</p>
     *  <p>The function is a description for what the MBean does.</p>
     *  <p>The class can be provided, if multiple MBean implementations with
     *  the same type and function will be loaded.</p>
     *  <p>Additional properties in the form
     *  <code>&lt;name&gt;=&lt;value&gt;</code> can be added as required.</p>
     *
     *  @param  domainName  The domain name.
     *  @param  type    The type of the MBean that will be named with the new
     *      object name.
     *  @param  function    The function of the MBean.
     *  @param  mbeanClass  The MBean's class; can be {@code null}.
     *  @param  properties  Additional properties as name-value-pairs; can be
     *      {@code null}.
     *  @return The object name.
     *  @throws MalformedObjectNameException    It is not possible to create a
     *      valid object name from the given domain name and properties.
     */
    @SafeVarargs
    @API( status = STABLE, since = "0.0.1" )
    public static ObjectName composeObjectName( final String domainName, final String type, final String function, final Class<?> mbeanClass, final NameValuePair<String>... properties ) throws MalformedObjectNameException
    {
        final var propertyList = new ArrayList<NameValuePair<String>> ();
        if( nonNull( properties ) ) propertyList.addAll( List.of( properties ) );
        if( nonNull( mbeanClass ) ) propertyList.add( new NameValuePair<>( MBEAN_CLASS, mbeanClass.getName() ) );
        propertyList.add( new NameValuePair<>( MBEAN_FUNCTION, requireNotEmptyArgument( function, "function" ) ) );
        propertyList.add( new NameValuePair<>( MBEAN_TYPE, requireNotEmptyArgument( type, "type" ) ) );

        @SuppressWarnings( "unchecked" )
        final var retValue = composeObjectName( domainName, propertyList.toArray( NameValuePair []::new ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  composeObjectName()

    /**
     *  Composes an object name from the given domain name and the given
     *  properties.
     *
     *  @param  domainName  The domain name.
     *  @param  properties  The properties as name-value-pairs; at least one
     *      property has to be provided.
     *  @return The object name.
     *  @throws MalformedObjectNameException    It is not possible to create a
     *      valid object name from the given domain name and properties.
     */
    @SafeVarargs
    @API( status = STABLE, since = "0.0.1" )
    public static ObjectName composeObjectName( final String domainName, final NameValuePair<String>... properties ) throws MalformedObjectNameException
    {
        final var name = new StringJoiner( ",", format( "%s:", requireNotEmptyArgument( domainName, "domainName" ) ), EMPTY_CHARSEQUENCE );
        for( final var property : requireNotEmptyArgument( properties, "properties" ) )
        {
            name.add( toString( property ) );
        }
        final var retValue = new ObjectName( name.toString() );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  composeObjectName()

    /**
     *  Converts an instance of
     *  {@link NameValuePair}
     *  to a String.
     *
     *  @param  pair    The name-value-pair.
     *  @return The String representation of the name-value-pair.
     */
    private static final String toString( final NameValuePair<String> pair )
    {
        if( isNull( pair.value() ) ) throw new IllegalArgumentException( "value is null" );
        final var retValue = format( "%1$s=%2$s", pair.name(), pair.value() );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   // toString()

    /**
     *  Unregisters the given MBean from the MBeanServer. All exceptions – if
     *  any – will be swallowed silently.
     *
     *  @param  mbean   The mbean to unregister; may be {@code null}.
     */
    @API( status = STABLE, since = "0.0.1" )
    public static void unregisterQuietly( final JMXSupport<?> mbean )
    {
        if( nonNull( mbean ) )
        {
            try
            {
                mbean.unregister();
            }
            catch( final InstanceNotFoundException | MBeanRegistrationException e )
            {
                ifDebug( e );
                /* Deliberately ignored */
            }
        }
    }   //  unregisterQuietly()
}
//  class ManagementUtils

/*
 *  End of File
 */