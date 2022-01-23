/*
 * ============================================================================
 * Copyright © 2002-2022 by Thomas Thrien.
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

package org.tquadrat.foundation.mgmt.internal;

import static java.lang.System.currentTimeMillis;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.lang.Objects.requireNotEmptyArgument;
import static org.tquadrat.foundation.util.StringUtils.format;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.ImpossibleExceptionError;
import org.tquadrat.foundation.lang.Lazy;
import org.tquadrat.foundation.lang.Objects;
import org.tquadrat.foundation.mgmt.JMXSupport;
import org.tquadrat.foundation.mgmt.MBeanAction;
import org.tquadrat.foundation.mgmt.MBeanGetter;
import org.tquadrat.foundation.mgmt.MBeanNotification;
import org.tquadrat.foundation.mgmt.MBeanNotifications;
import org.tquadrat.foundation.mgmt.MBeanParameter;
import org.tquadrat.foundation.mgmt.MBeanSetter;
import org.tquadrat.foundation.mgmt.ManagedObject;
import org.tquadrat.foundation.stream.MapStream;

/**
 *  This class implements the interface
 *  {@link JMXSupport}
 *  that in turn extends the definition for a dynamic MBean
 *  ({@link javax.management.DynamicMBean})
 *  and can instrument any object whose class is annotated properly.
 *
 *  @param  <T> The type of the managed object.
 *
 *  @see ManagedObject
 *  @see MBeanAction
 *  @see MBeanGetter
 *  @see MBeanNotification
 *  @see MBeanNotifications
 *  @see MBeanSetter
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: JMXSupportImpl.java 682 2020-03-21 19:20:31Z tquadrat $
 *  @since 0.0.1
 *
 *  @UMLGraph.link
 */
@SuppressWarnings( "OverlyComplexClass" )
@ClassVersion( sourceVersion = "$Id: JMXSupportImpl.java 682 2020-03-21 19:20:31Z tquadrat $" )
@API( status = INTERNAL, since = "0.0.1" )
public final class JMXSupportImpl<T> implements JMXSupport<T>
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  Message: {@value}.
     */
    public static final String MSG_ActionNotFound = "There is no MBean action with the name '%1$s'";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_AttributeNotFound = "The MBean attribute '%1$s' could not be found";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_InvalidAttributeValue = "The value '%s$s' is invalid for the MBean attribute with the name '%1$s'";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_InvocationDenied = "Cannot invoke the MBean action '%1$s'";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_InvocationException = "The invocation of the MBean action '%1$s' caused an Exception";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_InvocationProblems = "There was a problem when invoking the MBean action '%1$s'";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_NoManagedObject = "The given Object is not annotated as 'ManagedObject'";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_NoProperties = "No properties had been provided";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_SetterDenied = "The invocation of the setter for the MBean attribute '%1$s' was denied";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_SetterException = "The invocation of the setter for the MBean attribute '%1$s' caused an Exception";

    /**
     *  Message: {@value}.
     */
    public static final String MSG_SetterProblems = "There was a problem when invoking the setter for the MBean attribute '%1$s'";

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  {@summary This object supports the broadcasting of notifications.} It
     *  is
     *  {@linkplain Optional#empty()}
     *  if the object, that is instrumented by this instance of
     *  {@code JMXSupport}, indicates in its
     *  {@link ManagedObject}
     *  annotation that it will not use notifications.
     */
    @SuppressWarnings( "OptionalUsedAsFieldOrParameterType" )
    private Optional<NotificationBroadcasterSupport> m_BroadcasterSupport = Optional.empty();

    /**
     *  The actions (operations) for the JMX API; the action name is the key
     *  for this map.
     */
    private final Map<String,Method> m_MBeanActions = new HashMap<>();

    /**
     *  The getters for the JMX API; the attribute name is the key for this
     *  map.
     */
    private final Map<String,Method> m_MBeanGetters = new HashMap<>();

    /**
     *  The predefined
     *  {@link MBeanInfo}
     *  structure.
     */
    private final Lazy<MBeanInfo> m_MBeanInfo;

    /**
     *  The setters for the JMX API; the attribute name is the key for this
     *  map.
     */
    private final Map<String,Method> m_MBeanSetters = new HashMap<>();

    /**
     *  The object that is instrumented by this generic MBean.
     */
    private final T m_Object;

    /**
     *  The description for the object that is instrumented by this generic
     *  MBean.
     */
    private final String m_ObjectDescription;

    /**
     *  The object name for this generic MBean.
     */
    private final ObjectName m_ObjectName;

    /**
     *  The notification sequence number.
     */
    private final AtomicLong m_Sequence = new AtomicLong( 0 );

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Initialises the abstract server part of a concrete server
     *  implementation.
     *
     *  @param  object  The object to register with the JMX agent.
     *  @param  name  The object name for the MBean.
     *  @param  description The description for the MBean.
     *  @param  useNotifications    {@code true} if the instrumented object
     *      will send notifications, {@code false} (the default) otherwise.
     *  @param  threadFactory   The thread factory that is used when
     *      notifications will be emitted asynchronously; can be {@code null}
     *      and will be ignored if {@code useNotifications} is {@code false}.
     */
    private JMXSupportImpl( final T object, final ObjectName name, final String description, final boolean useNotifications, final ThreadFactory threadFactory )
    {
        m_MBeanInfo = Lazy.use( this::createMBeanInfo );

        //---* Save the parameters *-------------------------------------------
        m_Object = requireNonNullArgument( object, "object" );
        m_ObjectName = requireNonNullArgument( name, "objectName" );
        m_ObjectDescription = requireNotEmptyArgument( description, "description" );

        //---* Collect the methods *-------------------------------------------
        for( final var method: object.getClass().getMethods() )
        {
            //---* It is a getter *--------------------------------------------
            if( method.isAnnotationPresent( MBeanGetter.class ) )
            {
                /*
                 *  A getter may not have any parameters and must return a
                 *  value, but in this context it is not required that its name
                 *  starts with "get".
                 */
                if( (method.getParameterTypes().length == 0) && !method.getReturnType().equals( void.class ) )
                {
                    final var annotation = method.getAnnotation( MBeanGetter.class );
                    m_MBeanGetters.put( annotation.attribute(), method );
                }
            }

            //---* It is a setter *--------------------------------------------
            if( method.isAnnotationPresent( MBeanSetter.class ) )
            {
                /*
                 *  A setter must have exactly one parameter and may not return
                 *  anything but void, but in this context it is not required
                 *  that its name starts with "set".
                 */
                if( (method.getParameterTypes().length == 1) && method.getReturnType().equals( void.class ) )
                {
                    final var annotation = method.getAnnotation( MBeanSetter.class );
                    m_MBeanSetters.put( annotation.attribute(), method );
                }
            }

            //---* It is an action *-------------------------------------------
            if( method.isAnnotationPresent( MBeanAction.class ) )
            {
                final var annotation = method.getAnnotation( MBeanAction.class );
                m_MBeanActions.put( annotation.action(), method );
            }
        }

        //---* Add the notification support *----------------------------------
        if( useNotifications )
        {
            //---* Create the executor if necessary *--------------------------
            final Executor executor = nonNull( threadFactory ) ? newSingleThreadExecutor( threadFactory ) : null;

            //---* Retrieve the notification info *----------------------------
            final var notificationInfo = createNotificationInfo( object );

            //---* Create the broadcast support *------------------------------
            m_BroadcasterSupport = Optional.of( new NotificationBroadcasterSupport( executor, notificationInfo ) );
        }
    }   //  JMXSupportImpl()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    @Override
    public final void addNotificationListener( final NotificationListener listener, final NotificationFilter filter, final Object handback )
    {
        m_BroadcasterSupport.ifPresent( b ->b.addNotificationListener( listener, filter, handback ) );
    }   //  addNotificationListener()

    /**
     *  Creates the structure that holds the action information.
     *
     *  @return The action (or operation) information.
     */
    private final MBeanOperationInfo [] createActionInfo()
    {
        final var retValue = MapStream.of( m_MBeanActions )
            .map( e ->
            {
                final var name = e.getKey();
                final var method = e.getValue();
                final var annotation = method.getAnnotation( MBeanAction.class );
                final var description = annotation.description();
                final var signature = determineSignature( method );
                final var returnType = method.getReturnType().getName();
                final var impact = annotation.impact();

                //---* Done *--------------------------------------------------
                return new MBeanOperationInfo( name, description, signature, returnType, impact );
            })
            .toArray( MBeanOperationInfo []::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createActionInfo()

    /**
     *  Creates the structure that holds the attribute information.
     *
     *  @return The attribute information.
     *  @throws IntrospectionException  There is a consistency problem in the
     *      definition of an attribute.
     */
    private final MBeanAttributeInfo [] createAttributeInfo() throws IntrospectionException
    {
        final Collection<MBeanAttributeInfo> attributeInfos = new LinkedList<>();
        final Collection<String> processedSetters = new HashSet<>();

        //---* Process the getters first *-------------------------------------
       m_MBeanGetters.forEach( (name,getter) ->
        {
            //---* Collect the values *----------------------------------------
            final var type = getter.getReturnType().getName();
            final var description = getter.getAnnotation( MBeanGetter.class ).description();
            final var isWriteable = m_MBeanSetters.containsKey( name );
            if( isWriteable )
            {
                //---* Setter is paired with the getter *----------------------
                processedSetters.add( name );
            }

            //---* Create the info object *------------------------------------
            attributeInfos.add( new MBeanAttributeInfo( name, type, description, true, isWriteable, false ) );
        });

        //---* Process the setters *-------------------------------------------
        m_MBeanSetters.forEach( (name,setter) ->
        {
            //---* Check if already processed *--------------------------------
            if( !processedSetters.contains( name ) )
            {
                //---* Collect the values *------------------------------------
                final var type = setter.getParameterTypes() [0].getName();
                final var description = setter.getAnnotation( MBeanSetter.class ).description();

                //---* Create the info object *--------------------------------
                /*
                 * Setters that have a correspondent getter are already
                 * processed when the getter was handled; therefore all the
                 * attributes created here will be read-only (the isReadable
                 * argument is set to false).
                 */
                attributeInfos.add( new MBeanAttributeInfo( name, type, description, false, true, false ) );
            }
        });

        //---* Cleanup *-------------------------------------------------------
        processedSetters.clear();

        //---* Compose the return values *-------------------------------------
        final var retValue = attributeInfos.toArray( MBeanAttributeInfo []::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createAttributeInfo()

    /**
     *  Creates the MBeanInfo data structure.<br>
     *  <br>This implementation will swallow all exceptions, an overriding
     *  implementation may want to log it.
     *
     *  @return An instance of
     *      {@link MBeanInfo}
     *      allowing all attributes and actions exposed by this generic MBean
     *      to be retrieved.
     */
    private MBeanInfo createMBeanInfo()
    {
        MBeanInfo retValue = null;
        try
        {
            retValue = new MBeanInfo
            (
                getObject().getClass().getName(), // The name of the instrumented class.
                getObjectDescription(),           // The description of the instrumented object.
                createAttributeInfo(),            // The attributes.
                null,                             // The constructors.
                createActionInfo(),               // The operations.
                getNotificationInfo()             // The notifications.
           );
        }
        catch( @SuppressWarnings( "unused" ) final IntrospectionException e ) { /* Ignored */ }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createMBeanInfo()

    /**
     *  Creates a single notification info element.
     *
     *  @param  notification    The annotation data.
     *  @return The notification info element.
     */
    private static MBeanNotificationInfo createNotificationInfo( final MBeanNotification notification )
    {
        final var retValue = new MBeanNotificationInfo( notification.notifierTypes(), notification.notificationClass().getName(), notification.description() );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createNotificationInfo()

    /**
     *  Creates the structure that holds the notification info.
     *
     *  @param  object  The object to examine.
     *  @return The notification info structure.
     */
    private MBeanNotificationInfo [] createNotificationInfo( final Object object )
    {
        final Collection<MBeanNotificationInfo> notificationInfos = new LinkedList<>();

        //---* Get class level notification declarations *---------------------
        var notifications = m_Object.getClass().getAnnotation( MBeanNotifications.class );
        if( nonNull( notifications ) )
        {
            for( final var entry : notifications.value() )
            {
                notificationInfos.add( createNotificationInfo( entry ) );
            }
        }

        var notification = m_Object.getClass().getAnnotation( MBeanNotification.class );
        if( nonNull( notification ) )
        {
            notificationInfos.add( createNotificationInfo( notification ) );
        }

        //---* Get the method level notifications *----------------------------
        for( final var method: object.getClass().getMethods() )
        {
            notifications = method.getAnnotation( MBeanNotifications.class );
            if( nonNull( notifications ) )
            {
                for( final var entry : notifications.value() )
                {
                    notificationInfos.add( createNotificationInfo( entry ) );
                }
            }

            notification = method.getAnnotation( MBeanNotification.class );
            if( nonNull( notification ) )
            {
                notificationInfos.add( createNotificationInfo( notification ) );
            }
        }

        //---* Compose the return value *--------------------------------------
        final var retValue = notificationInfos.isEmpty()
            ? null
            : notificationInfos.toArray( MBeanNotificationInfo[]::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createNotificationInfo()

    /**
     *  Determines the parameter information for the given method.
     *
     *  @param  method  The method to analyse.
     *  @return The parameter info structure.
     */
    private static MBeanParameterInfo [] determineSignature( final Method method )
    {
        MBeanParameterInfo [] retValue = null;

        //---* Get the parameter types *---------------------------------------
        final var parameterTypes = method.getParameterTypes();
        if( parameterTypes.length > 0 )
        {
            retValue = new MBeanParameterInfo [parameterTypes.length];

            //---* Get the annotations *---------------------------------------
            final var allAnnotations = method.getParameterAnnotations();

            //---* Lets compose the signature *--------------------------------
            String name;
            String type;
            String description;
            if( allAnnotations.length > 0 )
            {
                for( var i = 0; i < parameterTypes.length; ++i )
                {
                    //---* Collect the values *--------------------------------
                    type = parameterTypes [i].getName();
                    name = format( "arg%d", i );
                    description = "?";
                    for( final var annotation : allAnnotations [i] )
                    {
                        //---* Is it the right annotation type? *--------------
                        if( annotation instanceof MBeanParameter parameterAnnotation )
                        {
                            name = parameterAnnotation.name();
                            description = parameterAnnotation.description();
                            break;
                        }
                    }

                    //---* Create the information object *---------------------
                    retValue [i] = new MBeanParameterInfo( name, type, description );
                }
            }
            else
            {
                description = "?";
                for( var i = 0; i < parameterTypes.length; ++i )
                {
                    //---* Collect the values *--------------------------------
                    type = parameterTypes [i].getName();
                    name = format( "arg%1$d", i );

                    //---* Create the information object *---------------------
                    retValue [i] = new MBeanParameterInfo( name, type, description );
                }
            }
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  determineSignature()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final Object getAttribute( final String attributeName ) throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        //---* Get the getter *------------------------------------------------
        final var method = m_MBeanGetters.get( requireNonNullArgument( attributeName, "attributeName" ) );
        if( isNull( method ) )
        {
            throw new AttributeNotFoundException( format( MSG_AttributeNotFound, attributeName ) );
        }

        //---* Retrieve the attributes value *---------------------------------
        Object retValue = null;
        try
        {
            retValue = method.invoke( m_Object, (Object []) null );
        }
        catch( final InvocationTargetException | IllegalArgumentException | IllegalAccessException e )
        {
            throw new ReflectionException( e );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getAttribute()

    /**
     *  Get the values of several attributes of the Dynamic MBean. This
     *  implementation will swallow all exceptions that might be thrown.
     *
     *  @param  attributes  A list of the attributes to be retrieved.
     *  @return The list of attributes retrieved.
     */
    @Override
    public AttributeList getAttributes( final String [] attributes )
    {
        //---* Create the return value *---------------------------------------
        final var retValue = new AttributeList();
        Attribute attribute;
        for( final var attributeName: requireNonNullArgument( attributes, "attributes" ) )
        {
            try
            {
                attribute = new Attribute( attributeName, getAttribute( attributeName ) );
                retValue.add( attribute );
            }
            catch( @SuppressWarnings( "unused" ) final AttributeNotFoundException | MBeanException | ReflectionException e ) { /* Ignored! */ }
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getAttributes()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final MBeanInfo getMBeanInfo() { return m_MBeanInfo.get(); }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final long getNextNotificationSequenceNumber() { return m_Sequence.get(); }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final MBeanNotificationInfo [] getNotificationInfo()
    {
        final var retValue = m_BroadcasterSupport.map( NotificationBroadcasterSupport::getNotificationInfo ).orElse( null );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getNotificationInfo()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final T getObject() { return m_Object; }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final String getObjectDescription() { return m_ObjectDescription; }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final ObjectName getObjectName()
    {
        ObjectName retValue = null;

        //---* Create the return value *---------------------------------------
        try
        {
            retValue =  new ObjectName( m_ObjectName.getDomain(), m_ObjectName.getKeyPropertyList() );
        }
        catch( final MalformedObjectNameException e )
        {
            throw new ImpossibleExceptionError( e );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getObjectName()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final Object invoke( final String actionName, final Object [] params, final String [] signature ) throws MBeanException, ReflectionException
    {
        Object retValue = null;

        //---* Get the method for the action *---------------------------------
        final var method = m_MBeanActions.get( actionName );
        if( nonNull( method ) )
        {
            //noinspection OverlyBroadCatchBlock
            try
            {
                retValue = method.invoke( m_Object, params );
            }
            catch( final InvocationTargetException e )
            {
                final var t = e.getCause();
                final var cause = t instanceof Exception ? (Exception) t : e;
                throw new ReflectionException( cause, format( MSG_InvocationProblems, actionName ) );
            }
            catch( final IllegalAccessException e )
            {
                throw new ReflectionException( e, format( MSG_InvocationDenied, actionName ) );
            }
            catch( final Exception e )
            {
                throw new MBeanException( e, format( MSG_InvocationException, actionName ) );
            }
        }
        else
        {
            throw new MBeanException( new NoSuchMethodException( format( MSG_ActionNotFound, actionName ) ) );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  invoke()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void removeNotificationListener( final NotificationListener listener ) throws ListenerNotFoundException
    {
        if( m_BroadcasterSupport.isPresent() )
        {
            m_BroadcasterSupport.get().removeNotificationListener( listener );
        }
    }   //  removeNotificationListener()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void removeNotificationListener( final NotificationListener listener, final NotificationFilter filter, final Object handback ) throws ListenerNotFoundException
    {
        if( m_BroadcasterSupport.isPresent() )
        {
            m_BroadcasterSupport.get().removeNotificationListener( listener, filter, handback );
        }
    }   //  removeNotificationListener()

    /**
     *  Registers the server with the JMX agent.
     *
     *  @param  <O> The type of the object.
     *  @param  object  The object to register with the JMX agent.
     *  @param  objectName  The object name that is used for the object to
     *      register.
     *  @param  threadFactory   The thread factory that is used when
     *      notifications should be sent asynchronously; can be {@code null}.
     *  @return The MBean object that was generated as the instrumentation for
     *      the object to manage.
     *  @throws IllegalArgumentException    The object is not annotated with
     *      {@link ManagedObject &#64;ManagedObject}.
     *  @throws InstanceAlreadyExistsException  There is already a MBean with
     *      the given object name registered.
     *  @throws MBeanRegistrationException  Problems with the registration of
     *      the MBean.
     */
    public static <O> JMXSupport<O> register( final O object, final ObjectName objectName, final ThreadFactory threadFactory ) throws IllegalArgumentException, InstanceAlreadyExistsException, MBeanRegistrationException
    {
        final var instrumentation = requireNonNullArgument( object, "object" )
            .getClass()
            .getAnnotation( ManagedObject.class );
        if( isNull( instrumentation ) ) throw new IllegalArgumentException( MSG_NoManagedObject );
        final var description = instrumentation.description();
        final var useNotifications = instrumentation.useNotifications();

        //---* Create the MBean *----------------------------------------------
        final var retValue = new JMXSupportImpl<>( object, requireNonNullArgument( objectName, "objectName" ), description, useNotifications, threadFactory );

        //---* Register the MBean with the platform MBeanServer *--------------
        registerMBean( retValue, objectName );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  register()

    /**
     *  Register the MBean with the platform MBeanServer.
     *
     *  @param  mbean   The MBean to register.
     *  @param  objectName  The object name for the registration.
     *  @throws InstanceAlreadyExistsException  There is already an MBean with
     *      the given object name.
     *  @throws MBeanRegistrationException  Problems with the registration of
     *      the MBean.
     */
    private static void registerMBean( final DynamicMBean mbean, final ObjectName objectName ) throws InstanceAlreadyExistsException, MBeanRegistrationException
    {
        final var mbeanServer = getPlatformMBeanServer();
        try
        {
            mbeanServer.registerMBean( mbean, objectName );
        }
        catch( final NotCompliantMBeanException e )
        {
            throw new ImpossibleExceptionError( e );
        }
    }   //  registerMBean()

    /**
     *  Returns the notification sequence number for the current notification
     *  and increments the number.
     *
     *  @return The next sequence number.
     */
    private long retrieveNextNotificationSequenceNumber() { return m_Sequence.getAndIncrement(); }

    /**
     *  Sends an attribute change notification. The type is always
     *  {@code jmx.attribute.change}.
     *
     *  @param  <A> The type for the attribute.
     *  @param  <V> The type for the values.
     *  @param  message The message for this notification.
     *  @param  description The description for the attribute.
     *  @param  attributeType   The type of the attribute.
     *  @param  oldValue    The old value.
     *  @param  newValue    The new value.
     */
    @Override
    public final <A,V extends A> void sendAttributeChangeNotification( final String message, final String description, final Class<A> attributeType, final V oldValue, final V newValue )
    {
        final Notification notification = new AttributeChangeNotification
        (
            this,
            retrieveNextNotificationSequenceNumber(),
            currentTimeMillis(),
            message,
            description,
            attributeType.getName(),
            oldValue,
            newValue
        );
        sendNotification( notification );
    }   //  sendAttributeChangeNotification()

    /**
     *  <p>{@summary Sends a notification.} It is not recommended using this
     *  method directly as this class will provide some very helpful
     *  convenience methods.</p>
     *  <p>If it is really necessary to create your own implementation for a
     *  notification, it is important that the source element is set to a
     *  reference to this instance, and not to the object that is instrumented
     *  using this object.
     *
     *  @param  notification    The notification to send.
     *
     *  @see javax.management.NotificationBroadcasterSupport#sendNotification(javax.management.Notification)
     *  @see javax.management.Notification#setSource(Object)
     *  @see javax.management.Notification#getSource()
     */
    private final void sendNotification( final Notification notification )
    {
        if( nonNull( notification ) )
        {
            m_BroadcasterSupport.ifPresent( n -> n.sendNotification( notification ) );
        }
    }   //  sendNotification()

    /**
     *  Sends a simple notification with a message text.
     *
     *  @param  type    The type of the notification.
     *  @param  message The message for this notification.
     */
    @Override
    public final void sendNotification( final String type, final String message )
    {
        final var notification = new Notification( type, this, retrieveNextNotificationSequenceNumber(), currentTimeMillis(), message );
        sendNotification( notification );
    }   //  sendNotification()

    /**
     *  Sets the value of a specific attribute of the generic MBean.
     *
     *  @param  attribute   The identification of the attribute to be set and
     *      the value it is to be set to.
     *  @throws AttributeNotFoundException  There is no attribute with the
     *      given name.
     *  @throws InvalidAttributeValueException  The value of the attribute does
     *      not fit to the attribute's declaration.
     *  @throws MBeanException  Wraps a
     *      {@link java.lang.Exception Exception}
     *      thrown by the MBean's setter.
     *  @throws ReflectionException  Wraps a
     *      {@link java.lang.Exception Exception}
     *      thrown while trying to invoke MBean's setter.
     *
     *  @see #getAttribute(String) getAttribute()
     *  @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    @Override
    public final void setAttribute( final Attribute attribute ) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
    {
        //---* Get the setter *------------------------------------------------
        final var method = m_MBeanSetters.get( attribute.getName() );
        if( method != null )
        {
            final var signature = method.getParameterTypes();

            //---* Test if the value can be set to the attribute *-------------
            var isAssignable = signature [0].isInstance( attribute.getValue() );
            if( !isAssignable )
            {
                isAssignable = signature [0] == Boolean.TYPE && attribute.getValue() instanceof Boolean;
            }
            if( !isAssignable )
            {
                isAssignable = signature [0] == Byte.TYPE && attribute.getValue() instanceof Byte;
            }
            if( !isAssignable )
            {
                isAssignable = signature [0] == Character.TYPE && attribute.getValue() instanceof Character;
            }
            if( !isAssignable )
            {
                isAssignable = signature [0] == Double.TYPE && attribute.getValue() instanceof Double;
            }
            if( !isAssignable )
            {
                isAssignable = signature [0] == Float.TYPE && attribute.getValue() instanceof Float;
            }
            if( !isAssignable )
            {
                isAssignable = signature [0] == Integer.TYPE && attribute.getValue() instanceof Integer;
            }
            if( !isAssignable )
            {
                isAssignable = signature [0] == Long.TYPE && attribute.getValue() instanceof Long;
            }
            if( !isAssignable )
            {
                isAssignable = signature [0] == Short.TYPE && attribute.getValue() instanceof Short;
            }

            //---* Set the attribute *-----------------------------------------
            if( isAssignable )
            {
                //noinspection OverlyBroadCatchBlock
                try
                {
                    method.invoke( m_Object, attribute.getValue() );
                }
                catch( final InvocationTargetException e )
                {
                    throw new ReflectionException( e, format( MSG_SetterProblems, attribute.getName() ) );
                }
                catch( final IllegalAccessException e )
                {
                    throw new ReflectionException( e, format( MSG_SetterDenied, attribute.getName() ) );
                }
                catch( final Exception e )
                {
                    throw new MBeanException( e, format( MSG_SetterException, attribute.getName() ) );
                }
            }
            else
            {
                throw new InvalidAttributeValueException( format( MSG_InvalidAttributeValue, attribute.getName(), Objects.toString( attribute.getValue() ) ) );
            }
        }
        else
        {
            throw new AttributeNotFoundException( format( MSG_AttributeNotFound, attribute.getName() ) );
        }
    }   //  setAttribute()

    /**
     *  Sets the values of several attributes of the generic MBean. This
     *  implementation will silently swallow any exception that might occur; an
     *  implementation in a subclass might want to log them.<br>
     *  <br>An Attribute that causes an exception when
     *  {@link #setAttribute(Attribute) setAttribute()}
     *  is called with it will not be part of the returned list of attributes.
     *
     *  @param  attributes  A list of attributes: The identification of the
     *      attributes to be set and  the values they are to be set to.
     *  @return The list of attributes that were set, with their new values.
     *
     *  @see #getAttributes(String[]) getAttributes()
     *  @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
     */
    @Override
    public AttributeList setAttributes( final AttributeList attributes )
    {
        final var retValue = new AttributeList();

        //---* Set the values *------------------------------------------------
        for( final var o : attributes )
        {
            final var attribute = (Attribute) o;
            try
            {
                setAttribute( attribute );
                retValue.add( attribute );
            }
            catch( @SuppressWarnings( "unused" ) final AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException e )
            {
                continue;
            }
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  setAttributes()

    /**
     *  Unregisters the MBean from the MBeanServer.
     *
     *  @throws InstanceNotFoundException   The MBean is not registered.
     *  @throws MBeanRegistrationException  Problems with the registration of
     *      the MBean.
     */
    @Override
    public final void unregister() throws InstanceNotFoundException, MBeanRegistrationException
    {
        //---* Unregister the MBean *------------------------------------------
        final var mbeanServer = getPlatformMBeanServer();
        mbeanServer.unregisterMBean( m_ObjectName );

        //---* Cleanup *-------------------------------------------------------
        m_MBeanActions.clear();
        m_MBeanGetters.clear();
        m_MBeanSetters.clear();

        /*
         * Even if the object would expose a method to destroy it, we would not
         * be allowed to call it here – obviously.
         */
    }   //  unregister()
}
//  class JMXSupportImpl

/*
 *  End of File
 */