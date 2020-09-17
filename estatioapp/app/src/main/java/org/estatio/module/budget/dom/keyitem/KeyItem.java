/*
 *
 *  Copyright 2012-2015 Eurocommercial Properties NV
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
package org.estatio.module.budget.dom.keyitem;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.InheritanceStrategy;
import javax.validation.constraints.Digits;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.incode.module.base.dom.utils.TitleBuilder;

import org.estatio.module.budget.dom.Distributable;
import org.estatio.module.budget.dom.budgetcalculation.BudgetCalculationType;
import org.estatio.module.budget.dom.keytable.FoundationValueType;
import org.estatio.module.budget.dom.keytable.KeyTable;

import lombok.Getter;
import lombok.Setter;

@javax.jdo.annotations.PersistenceCapable(
        identityType = IdentityType.DATASTORE
        ,schema = "dbo" // Isis' ObjectSpecId inferred from @DomainObject#objectType
)
@javax.jdo.annotations.Inheritance(strategy = InheritanceStrategy.SUPERCLASS_TABLE)
@javax.jdo.annotations.Discriminator("org.estatio.dom.budgeting.keyitem.KeyItem")
@DomainObject(
        editing = Editing.DISABLED,
        objectType = "org.estatio.dom.budgeting.keyitem.KeyItem"
)
public class KeyItem extends PartitioningTableItem
        implements Distributable {

    public String title() {
        return TitleBuilder
                .start()
                .withParent(getPartitioningTable())
                .withName(getUnit())
                .toString();
    }

    public KeyItem(){
        super("partitioningTable, unit, sourceValue, value, auditedValue");
    }

    @Column(allowsNull = "false", scale = 6)
    @Getter @Setter
    private BigDecimal sourceValue;

    @ActionLayout(hidden = Where.EVERYWHERE)
    public KeyItem changeSourceValue(final BigDecimal sourceValue) {
        setSourceValue(sourceValue.setScale(2, BigDecimal.ROUND_HALF_UP));
        return this;
    }

    public BigDecimal default0ChangeSourceValue(final BigDecimal sourceValue) {
        return getSourceValue();
    }

    public String validateChangeSourceValue(final BigDecimal sourceValue) {
        if (sourceValue.compareTo(BigDecimal.ZERO) < 0) {
            return "Source Value must be positive";
        }
        return null;
    }

    // //////////////////////////////////////

    @Column(allowsNull = "false", scale = 6)
    @Getter @Setter
    private BigDecimal value;

    @ActionLayout(hidden = Where.EVERYWHERE)
    public KeyItem changeValue(final BigDecimal keyValue) {
        KeyTable keyTable = (KeyTable) getPartitioningTable();
        setValue(keyValue.setScale(keyTable.getPrecision(), BigDecimal.ROUND_HALF_UP));
        return this;
    }

    public BigDecimal default0ChangeValue(final BigDecimal targetValue) {
        KeyTable keyTable = (KeyTable) getPartitioningTable();
        return getValue().setScale(keyTable.getPrecision(), BigDecimal.ROUND_HALF_UP);
    }

    public String validateChangeValue(final BigDecimal keyValue) {
        if (keyValue.compareTo(BigDecimal.ZERO) < 0) {
            return "Value cannot be less than zero";
        }
        return null;
    }

    //region > auditedValue (property)
    @Column(allowsNull = "true", scale = 6)
    @PropertyLayout(hidden = Where.EVERYWHERE)
    @Getter @Setter
    private BigDecimal auditedValue;

    @ActionLayout(hidden = Where.EVERYWHERE)
    public KeyItem changeAuditedValue(final BigDecimal auditedKeyValue) {
        KeyTable keyTable = (KeyTable) getPartitioningTable();
        setAuditedValue(auditedKeyValue.setScale(keyTable.getPrecision(), BigDecimal.ROUND_HALF_UP));
        return this;
    }

    public BigDecimal default0ChangeAuditedValue(final BigDecimal auditedKeyValue) {
        if (getAuditedValue()!=null) {
            KeyTable keyTable = (KeyTable) getPartitioningTable();
        return getAuditedValue().setScale(keyTable.getPrecision(), BigDecimal.ROUND_HALF_UP);
        } else {
            return BigDecimal.ZERO;
        }
    }

    public String validateChangeAuditedValue(final BigDecimal auditedKeyValue) {
        if (auditedKeyValue.compareTo(BigDecimal.ZERO) < 0) {
            return "Value cannot be less than zero";
        }
        return null;
    }
    //endregion

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    public KeyTable deleteKeyItem() {
        KeyTable keyTable = (KeyTable) this.getPartitioningTable();
        this.getPartitioningTable().getBudget().removeNewCalculationsOfType(BudgetCalculationType.BUDGETED);
        removeIfNotAlready(this);
        return keyTable;
    }

    public String disableDeleteKeyItem(){
        KeyTable keyTable = (KeyTable) this.getPartitioningTable();
        return keyTable.isAssignedReason();
    }

    @Override
    @PropertyLayout(hidden = Where.EVERYWHERE)
    public ApplicationTenancy getApplicationTenancy() {
        return getPartitioningTable().getApplicationTenancy();
    }

    @Digits(integer = 13, fraction = 6)
    @Action(semantics = SemanticsOf.SAFE)
    public BigDecimal getDivCalculatedSourceValue(){
        KeyTable keyTable = (KeyTable) getPartitioningTable();
        if (keyTable.getFoundationValueType() == FoundationValueType.AREA) {
            return getUnit().getArea() != null ? getUnit().getArea().subtract(getSourceValue()).setScale(6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    public boolean hideDivCalculatedSourceValue(){
        KeyTable keyTable = (KeyTable) getPartitioningTable();
        return keyTable.getFoundationValueType()!= FoundationValueType.AREA;
    }
}
