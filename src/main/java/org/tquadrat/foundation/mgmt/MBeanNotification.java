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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;

/**
 *  {@summary This annotation will be used to declare a MBean notification.} If
 *  a class is annotated, it means that at least one method of this class may
 *  send this notification. Is the annotated component a method, it means that
 *  this method is sending the notification.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: MBeanNotification.java 634 2020-02-04 08:30:44Z tquadrat $
 *  @since 0.0.1
 *
 *  @UMLGraph.link
 */
@API( status = STABLE, since = "0.0.1" )
@Repeatable( MBeanNotifications.class )
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.TYPE, ElementType.METHOD} )
@Inherited
@ClassVersion( sourceVersion = "$Id: MBeanNotification.java 634 2020-02-04 08:30:44Z tquadrat $" )
public @interface MBeanNotification
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  Returns the notifier types (in dot notation).
     *
     *  @return The notifier types; the default is an empty array.
     */
    String [] notifierTypes() default {};

    /**
     *  Returns the class of the notification implementation.
     *
     *  @return The class of the notification implementation.
     */
    Class<?> notificationClass();

    /**
     *  Returns the description of the notification.
     *
     *  @return The description of the notification.
     */
    String description();
}
//  @interface MBeanNotification

/*
 *  End of File
 */