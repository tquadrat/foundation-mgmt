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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;

/**
 *  This annotation will be used to provide information about the parameters
 *  of an action or constructor for an MBean implementation.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: MBeanParameter.java 995 2022-01-23 01:09:35Z tquadrat $
 *  @since 0.0.1
 */
@ClassVersion( sourceVersion = "$Id: MBeanParameter.java 995 2022-01-23 01:09:35Z tquadrat $" )
@API( status = STABLE, since = "0.0.1" )
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Inherited
public @interface MBeanParameter
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  Returns the parameter's name.
     *
     *  @return The name of the parameter.
     */
    String name();

    /**
     *  Returns the description for the parameter.
     *
     *  @return The description.
     */
    String description() default "";
}
//  @interface MBeanParameter

/*
 *  End of File
 */