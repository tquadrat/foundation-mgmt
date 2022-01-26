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

package org.tquadrat.foundation.mgmt;

import static org.apiguardian.api.API.Status.STABLE;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import java.util.concurrent.ThreadFactory;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.mgmt.internal.JMXSupportImpl;

/**
 *  <p>{@summary The implementations of this interface will provide a dynamic
 *  MBean for the instrumentation of object annotated with
 *  {@link org.tquadrat.foundation.mgmt.ManagedObject}.}</p>
 *
 *  @param  <T> The type of the managed object.
 *
 *  @see org.tquadrat.foundation.mgmt.ManagedObject
 *  @see org.tquadrat.foundation.mgmt.MBeanAction
 *  @see org.tquadrat.foundation.mgmt.MBeanGetter
 *  @see org.tquadrat.foundation.mgmt.MBeanNotification
 *  @see org.tquadrat.foundation.mgmt.MBeanNotifications
 *  @see org.tquadrat.foundation.mgmt.MBeanSetter
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: JMXSupport.java 995 2022-01-23 01:09:35Z tquadrat $
 *  @since 0.0.1
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: JMXSupport.java 995 2022-01-23 01:09:35Z tquadrat $" )
@API( status = STABLE, since = "0.0.1" )
public sealed interface JMXSupport<T> extends DynamicMBean, NotificationEmitter
    permits org.tquadrat.foundation.mgmt.internal.JMXSupportImpl
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Returns the sequence number that is used for the next notification.
     *
     *  @return The next sequence number.
     */
    public long getNextNotificationSequenceNumber();

    /**
     *  Returns the object that is instrumented by this MBean instance.
     *
     *  @return The instrumented object.
     */
    public T getObject();

    /**
     *  Returns the object description.
     *
     *  @return The object description.
     */
    public String getObjectDescription();

    /**
     *  Returns the object name for this MBean. This is a copy of that instance
     *  that is used during registration, not the real thing. Therefore, it
     *  should be only used for reference.
     *
     *  @return A copy of the MBean's object name.
     */
    public ObjectName getObjectName();

    /**
     *  <p>{@summary Registers the server with the JMX agent.}</p>
     *  <p>If modules are used, the registered object must be accessible for
     *  this module ({@code org.tquadrat.foundation.mgmt}). This can be
     *  achieved easiest by opening the package with the respective class to
     *  the management module.</p>
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
     *      {@link org.tquadrat.foundation.mgmt.ManagedObject &#64;ManagedObject}.
     *  @throws InstanceAlreadyExistsException  There is already a MBean with
     *      the given object name registered.
     *  @throws MBeanRegistrationException  Problems with the registration of
     *      the MBean.
     */
    public static <O> JMXSupport<O> register( final O object, final ObjectName objectName, final ThreadFactory threadFactory ) throws IllegalArgumentException, InstanceAlreadyExistsException, MBeanRegistrationException
    {
        return JMXSupportImpl.register( object, objectName, threadFactory );
    }   //  register()

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
    public <A,V extends A> void sendAttributeChangeNotification( final String message, final String description, final Class<A> attributeType, final V oldValue, final V newValue );

    /**
     *  Sends a simple notification with a message text.
     *
     *  @param  type    The type of the notification.
     *  @param  message The message for this notification.
     */
    public void sendNotification( final String type, final String message );

    /**
     *  Unregisters the MBean from the MBeanServer.
     *
     *  @throws InstanceNotFoundException   The MBean is not registered.
     *  @throws MBeanRegistrationException  Problems with the registration of
     *      the MBean.
     */
    public void unregister() throws InstanceNotFoundException, MBeanRegistrationException;
}
//  interface JMXSupport

/*
 *  End of File
 */