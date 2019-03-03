/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.test.persistency.outcome

import org.cristalise.dsl.persistency.outcome.Attribute;
import org.cristalise.dsl.persistency.outcome.Field
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.test.builders.SchemaTestBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class SchemaBuilderDynymicFormsSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }

    def 'Field can specify dynamicForms.mask'() {
        expect:
        SchemaTestBuilder.build('test', 'PatientDetails', 0) {
            struct(name: 'PatientDetails') {
                field(name: 'Weight',      type: 'decimal') {dynamicForms(mask : '999.99')}
                field(name: 'DateOfBirth', type: 'date')    {dynamicForms(mask : '99/99/9999')}
            }
        }.compareXML(
            """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                 <xs:element name="PatientDetails">
                   <xs:complexType>
                     <xs:all minOccurs="0">
                       <xs:element name='Weight' type='xs:decimal' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <mask>999.99</mask>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                       <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
                         <xs:annotation>
                           <xs:appinfo>
                             <dynamicForms>
                               <mask>99/99/9999</mask>
                             </dynamicForms>
                           </xs:appinfo>
                         </xs:annotation>
                       </xs:element>
                     </xs:all>
                   </xs:complexType>
                 </xs:element>
               </xs:schema>""")
    }
}
