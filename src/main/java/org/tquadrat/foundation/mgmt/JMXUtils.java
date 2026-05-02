/*
 * ============================================================================
 * Copyright © 2002-2026 by Thomas Thrien.
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
import static java.rmi.registry.LocateRegistry.createRegistry;
import static java.rmi.registry.LocateRegistry.getRegistry;
import static javax.management.remote.JMXConnectorServerFactory.newJMXConnectorServer;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_CHARSEQUENCE;
import static org.tquadrat.foundation.lang.DebugOutput.ifDebug;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.lang.Objects.requireNotBlankArgument;
import static org.tquadrat.foundation.lang.Objects.requireNotEmptyArgument;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.annotation.UtilityClass;
import org.tquadrat.foundation.exception.PrivateConstructorForStaticClassCalledError;
import org.tquadrat.foundation.exception.UnexpectedExceptionError;
import org.tquadrat.foundation.lang.NameValuePair;

/**
 *  This class provides some utilities that are useful in the context of JMX.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: JMXUtils.java 1217 2026-05-02 12:58:09Z tquadrat $
 *  @since 0.0.1
 *
 *  @UMLGraph.link
 */
@UtilityClass
@ClassVersion( sourceVersion = "$Id: JMXUtils.java 1217 2026-05-02 12:58:09Z tquadrat $" )
@API( status = STABLE, since = "0.0.1" )
public final class JMXUtils
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  <p>{@summary The JNDI name for the exposed
     *  {@link MBeanServer}: {@value}}</p>
     *
     *  @see #enableRemoteAccess(MBeanServer,JMXServiceURL,int,Map)
     *  @see #enableRemoteAccess(MBeanServer,int,Map)
     *  @see #enableRemoteAccess(MBeanServer,String,int,int,Map)
     *  @see #disableRemoteAccess(JMXServiceURL)
     */
    public static final String BIND_NAME = "jmxrmi";

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

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The
     *  {@link javax.management.remote.JMXConnectorServer}
     *  instance that expose an
     *  {@link MBeanServer}.
     *
     *  @see #enableRemoteAccess(MBeanServer,int,Map)
     */
    private static final Map<JMXServiceURL,JMXConnectorServer> m_ConnectorServers = new HashMap<>();

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
     *  <p>{@summary Disables the external access to the
     *  {@link MBeanServer}
     *  for the given service URL.} Nothing happens if there was no NBean
     *  server exposed that URL.</p>
     *  <p>Internally, this method deactivates the
     *  {@linkplain JMXConnectorServer connector server},
     *  that is, stops listening for client connections. Calling this method
     *  will also close all client connections that were made by this server.
     *  After this method returns, whether normally or with an exception, the
     *  connector server will not create any new client connections.</p>
     *  <p>Once a connector server has been stopped, it cannot be started
     *  again.</p>
     *  <p>Calling this method when the connector server has already been
     *  stopped has also no effect.</p>
     *  <p>If closing a client connection produces an exception, that
     *  exception is not thrown from this method. A
     *  {@link javax.management.remote.JMXConnectionNotification JMXConnectionNotification}
     *  with type
     *  {@link javax.management.remote.JMXConnectionNotification#FAILED JMXConnectionNotification.FAILED}
     *  is emitted from this MBean with the connection ID of the connection
     *  that could not be closed.</p>
     *  <p>Closing a connector server is a potentially slow operation. For
     *  example, if a client machine with an open connection has crashed, the
     *  close operation might have to wait for a network protocol timeout.
     *  Callers that do not want to block in a close operation should do it in
     *  a separate thread.</p>
     *  <p>This method works for both locally and remotely exposed connector
     *  servers.</p>
     *
     *  @param  serviceURL  The URL that is used to connect to the MBean
     *      server.
     *  @throws IOException The connection server cannot be closed cleanly.
     *      When this exception is thrown, the connection server has already
     *      attempted to close all client connections. All client connections
     *      are closed except possibly those that generated exceptions when the
     *      server attempted to close them.
     *  @since 0.25.3
     */
    @API( status = STABLE, since = "0.25.3" )
    public static final void disableRemoteAccess( final JMXServiceURL serviceURL ) throws IOException
    {
        final JMXConnectorServer connectorServer;
        synchronized( m_ConnectorServers )
        {
            connectorServer = m_ConnectorServers.remove( requireNonNullArgument( serviceURL, "serviceURL" ) );
        }
        if( nonNull( connectorServer ) )
        {
            /*
             * Stopping the connector server will also unbind it from the
             * registry automatically. We do not need to do explicitly here.
             */
            connectorServer.stop();
        }
    }   //  disableRemoteAccess()

    /**
     *  <p>{@summary Enables the external access to the
     *  {@link MBeanServer}.}</p>
     *  <p>{@link #enableRemoteAccess(MBeanServer,String,int,int,Map)}
     *  and
     *  {@link #enableRemoteAccess(MBeanServer,int,Map)}
     *  are delegating to this method. See there for details.</p>
     *
     *  @param  mbeanServer The MBean server that should be exposed.
     *  @param  serviceURL  The service URL.
     *  @param  registryPortNumber  The port number that is used for the
     *      registry connection.
     *  @param  environment The configuration settings for the
     *      {@link JMXConnectorServer}.
     *  @throws RemoteException The RMI registry cannot be created/exported.
     *  @throws IOException Failed to create the connection server.
     *  @throws IllegalStateException   The connection server was previously
     *      stopped and the attempt to restart it failed.
     *
     *  @since 0.25.3
     */
    @API( status = INTERNAL, since = "0.25.3" )
    private static final void enableRemoteAccess( final MBeanServer mbeanServer, final JMXServiceURL serviceURL, final int registryPortNumber, final Map<String,?> environment ) throws IllegalStateException, IOException, RemoteException
    {
        requireNonNullArgument( mbeanServer, "mBeanServer" );

        synchronized( m_ConnectorServers )
        {
            var connectorServer = m_ConnectorServers.get( requireNonNullArgument( serviceURL, "serviceURL" ) );
            if( isNull( connectorServer ) )
            {
                //---* Start the RMI registry *------------------------------------
                startRMIRegistry( registryPortNumber );

                connectorServer = newJMXConnectorServer( serviceURL, environment, mbeanServer );
                connectorServer.start();
                m_ConnectorServers.put( serviceURL, connectorServer );
            }
            else
            {
                if( !connectorServer.isActive() )
                {
                    try
                    {
                        connectorServer.start();
                    }
                    catch( final IOException e )
                    {
                        throw new IllegalStateException( "Cannot (re)start ConnectorServer on port %d (URL: %s)".formatted( registryPortNumber, serviceURL ), e );
                    }
                }
            }
        }
    }   //  enableRemoteAccess()

    /**
     *  <p>{@summary Enables the external access to the
     *  {@link MBeanServer}
     *  from a process running on the same machine.}</p>
     *  <p>Basically, this method creates a new instance of
     *  {@link JMXConnectorServer},
     *  registers it to a
     *  {@linkplain Registry JNDI registry}
     *  associated with the given port number, and finally
     *  {@linkplain JMXConnectorServer#start() starts}
     *  it.</p>
     *  <p>If the registry does not exist yet, it will be created first.</p>
     *  <p>The same MBean server can be multiple times, using this method or
     *  {@link #enableRemoteAccess(MBeanServer,String,int,int,Map)},
     *  but the given port numbers must be different.</p>
     *
     *  @param  mbeanServer The MBean server that should be exposed.
     *  @param  registryPortNumber  The port number that is used for the connection.
     *  @param  environment The configuration settings for the
     *      {@link JMXConnectorServer}
     *      that is used to expose the MBean server. This parameter can be
     *      {@code null}, although it is recommended to use
     *      {@link Map#of()}
     *      in case no attributes should be provided. The keys in this map must
     *      be Strings. The appropriate type of each associated value depends
     *      on the attribute. The contents of {@code environment} are not
     *      changed by this call.
     *  @return The
     *      {@link JMXServiceURL}
     *      that was used to register the MBean server. It has the format
     *      {@code service:jmx:rmi:///jndi/rmi://localhost:<port>/jmxrmi}.
     *  @throws RemoteException The RMI registry cannot be created/exported.
     *  @throws IOException Failed to create the connection server.
     *  @throws IllegalStateException   The connection server was previously
     *      stopped and the attempt to restart it failed.
     *
     *  @see #BIND_NAME

     *  @since 0.25.3
     */
    @API( status = STABLE, since = "0.25.3" )
    public static final JMXServiceURL enableRemoteAccess( final MBeanServer mbeanServer, final int registryPortNumber, final Map<String,?> environment ) throws IllegalStateException, IOException, RemoteException
    {
        final JMXServiceURL retValue;
        try
        {
            retValue = new JMXServiceURL( "service:jmx:rmi:///jndi/rmi://localhost:%2$d/%1$s".formatted( BIND_NAME, registryPortNumber ) );
        }
        catch( MalformedURLException e )
        {
            throw new UnexpectedExceptionError( e );
        }

        enableRemoteAccess( requireNonNullArgument( mbeanServer, "mBeanServer" ), retValue, registryPortNumber, environment );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  enableRemoteAccess()

    /**
     *  <p>{@summary Makes the
     *  {@link MBeanServer}
     *  accessible for remote machines.}</p>
     *  <p>Basically, this method creates a new instance of
     *  {@link JMXConnectorServer},
     *  registers it to a
     *  {@linkplain Registry JNDI registry}
     *  associated with the given port number, and finally
     *  {@linkplain JMXConnectorServer#start() starts}
     *  it.</p>
     *  <p>If the registry does not exist yet, it will be created first.</p>
     *  <p>The same MBean server can be multiple times, using this method or
     *  {@link #enableRemoteAccess(MBeanServer, int, Map)},
     *  but the given port numbers must be different.</p>
     *
     *  @param  mbeanServer The MBean server that should be exposed.
     *  @param  hostName    The host name that is used for the connection.
     *  @param  registryPortNumber  The port number that is used for the
     *      registry connection.
     *  @param  dataPortNumber  The port number that is used for the
     *      data transport.
     *  @param  environment The configuration settings for the
     *      {@link JMXConnectorServer}
     *      that is used to expose the MBean server. This parameter can be
     *      {@code null}, although it is recommended to use
     *      {@link Map#of()}
     *      in case no attributes should be provided. The keys in this map must
     *      be Strings. The appropriate type of each associated value depends
     *      on the attribute. The contents of {@code environment} are not
     *      changed by this call.
     *  @return The
     *      {@link JMXServiceURL}
     *      that was used to register the MBean server. It has the format
     *      {@code service:jmx:rmi://<host>:<dataPort>/jndi/rmi://<host>:<registryPort>/jmxrmi}
     *  @throws RemoteException The RMI registry cannot be created/exported.
     *  @throws IOException Failed to create the connection server.
     *  @throws IllegalStateException   The connection server was previously
     *      stopped and the attempt to restart it failed.
     *  @throws MalformedURLException   It is not possible to compose a valid
     *      {@link JMXServiceURL}
     *      with the given {@code hostName}.
     *
     *  @see #BIND_NAME
     *
     *  @since 0.25.3
     */
    @API( status = STABLE, since = "0.25.3" )
    public static final JMXServiceURL enableRemoteAccess( final MBeanServer mbeanServer, final String hostName, final int registryPortNumber, final int dataPortNumber, Map<String,?> environment ) throws IllegalStateException, IOException, RemoteException
    {
        final var retValue = new JMXServiceURL( "service:jmx:rmi://%2$s:%4$d/jndi/rmi://%2$s:%3$d/%1$s".formatted( BIND_NAME, requireNotBlankArgument( hostName, "hostName" ), registryPortNumber, dataPortNumber ) );
        enableRemoteAccess( requireNonNullArgument( mbeanServer, "mBeanServer" ), retValue, registryPortNumber, environment );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  enableRemoteAccess()

    /**
     *  <p>{@summary Ensures that an
     *  {@linkplain Registry RMI registry}
     *  is running for given the port.} If there is no registry, a new one will
     *  be started.</p>
     *
     *  @param  port    The registry port.
     *  @throws RemoteException The registry cannot be created.
     */
    private static final void startRMIRegistry( final int port ) throws RemoteException
    {
        try
        {
            //---* Check if RMI registry already exists *------------------
            /*
             *  LocateRegistry.getRegistry() returns only a stub or proxy,
             *  but does not verify whether the registry really exists.
             *  The following call Registry::list() enforces a connection
             *  with the registry – and fails with a RemoteException if the
             *  registry does not exist.
             */
            final var registry = getRegistry( port );
            registry.list();
        }
        catch( final RemoteException _ )
        {
            //---* Create the RMI registry if it does not exist *----------
            createRegistry( port );
        }
    }   //  startRMIRegistry()

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
        final var retValue = "%1$s=%2$s".formatted(  pair.name(), pair.value() );

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