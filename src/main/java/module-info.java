/*
 * ============================================================================
 * Copyright © 2002-2022 by Thomas Thrien.
 * All Rights Reserved.
 * ============================================================================
 *
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

/**
 *  The module for the JMX extensions by the <i>Foundation</i> Library.
 *
 *  @version $Id: module-info.java 990 2022-01-14 23:34:24Z tquadrat $
 *
 *  @todo task.list
 */

module org.tquadrat.foundation.mgmt
{
    requires java.base;
    requires java.management;
    requires org.tquadrat.foundation.util;

    //---* Common Use *--------------------------------------------------------
    exports org.tquadrat.foundation.mgmt;
}

/*
 *  End of File
 */