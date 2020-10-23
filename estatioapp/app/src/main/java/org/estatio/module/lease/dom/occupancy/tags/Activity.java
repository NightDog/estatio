/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.module.lease.dom.occupancy.tags;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.Where;

import org.estatio.module.base.dom.EstatioRole;
import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.incode.module.base.dom.types.NameType;
import org.incode.module.base.dom.utils.TitleBuilder;

import org.estatio.module.base.dom.UdoDomainObject2;
import org.incode.module.base.dom.with.WithNameGetter;
import org.estatio.module.base.dom.apptenancy.ApplicationTenancyConstants;
import org.estatio.module.base.dom.apptenancy.WithApplicationTenancyGlobal;

import lombok.Getter;
import lombok.Setter;

@javax.jdo.annotations.PersistenceCapable(
        identityType = IdentityType.DATASTORE
        ,schema = "dbo"    // Isis' ObjectSpecId inferred from @DomainObject#objectType
)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@javax.jdo.annotations.Uniques({
        @javax.jdo.annotations.Unique(
                name = "Activity_sector_name_UNQ", members = { "sector", "name" })
})
@javax.jdo.annotations.Queries({
        @javax.jdo.annotations.Query(
                name = "findBySectorAndName", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.module.lease.dom.occupancy.tags.Activity "
                        + "WHERE sector == :sector "
                        + "   && name == :name")
})
@DomainObject(
        bounded = true,
        editing = Editing.DISABLED,
        objectType = "org.estatio.dom.lease.tags.Activity"
)
public class Activity
        extends UdoDomainObject2<Activity>
        implements WithNameGetter, WithApplicationTenancyGlobal {

    public Activity() {
        super("sector,name");
    }

    public String title() {
        return TitleBuilder.start()
                .withName(getName())
                .withParent(getSector())
                .toString();
    }

    @Property(hidden = Where.EVERYWHERE)
    public ApplicationTenancy getApplicationTenancy() {
        return securityApplicationTenancyRepository.findByPathCached(
                ApplicationTenancyConstants.GLOBAL_PATH);
    }

    // //////////////////////////////////////

    @javax.jdo.annotations.Column(name = "sectorId", allowsNull = "false")
    @Getter @Setter
    private Sector sector;

    // //////////////////////////////////////

    @javax.jdo.annotations.Column(allowsNull = "false", length= NameType.Meta.MAX_LEN)
    @Getter @Setter
    private String name;

    public Activity change(
            final String name,
            final Sector sector) {
        setName(name);
        setSector(sector);
        return this;
    }

    public String default0Change() {
        return getName();
    }

    public Sector default1Change() {
        return getSector();
    }

    public String disableChange() {
        return EstatioRole.ADMINISTRATOR.isApplicableFor(getUser()) ? null : "Users cannot change sectors or activities.";
    }
}
