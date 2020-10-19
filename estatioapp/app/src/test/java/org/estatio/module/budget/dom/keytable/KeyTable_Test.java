/*
 * Copyright 2015 Yodo Int. Projects and Consultancy
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.estatio.module.budget.dom.keytable;

import java.math.BigDecimal;
import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.joda.time.LocalDate;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.incode.module.unittestsupport.dom.bean.AbstractBeanPropertiesTest;

import org.estatio.module.budget.dom.budget.Budget;
import org.estatio.module.budget.dom.budgetcalculation.BudgetCalculationRepository;
import org.estatio.module.budget.dom.budgetcalculation.BudgetCalculationType;
import org.estatio.module.budget.dom.budgetitem.BudgetItem;
import org.estatio.module.budget.dom.keyitem.KeyItem;
import org.estatio.module.budget.dom.partioning.PartitionItem;
import org.estatio.module.budget.dom.partioning.PartitionItemRepository;
import org.estatio.module.budget.dom.partioning.Partitioning;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyTable_Test {

    public static class BeanProperties extends AbstractBeanPropertiesTest {

        @Test
        public void test() {
            final KeyTable pojo = new KeyTable();
            newPojoTester()
                    .withFixture(pojos(Budget.class, Budget.class))
                    .exercise(pojo);
        }

    }

    public static class OtherTests extends KeyTable_Test {

        @Test
        public void used_in_partition_items_works() throws Exception {

            // given
            Budget budget = new Budget();

            KeyTable keyTable = new KeyTable();
            keyTable.setBudget(budget);

            PartitionItem partitionItemUsingKeyTable = new PartitionItem();
            partitionItemUsingKeyTable.setPartitioningTable(keyTable);

            PartitionItem otherPartitionItem = new PartitionItem();

            Partitioning partitioning = new Partitioning();
            partitioning.getItems().add(partitionItemUsingKeyTable);
            partitioning.getItems().add(otherPartitionItem);
            assertThat(partitioning.getItems().size()).isEqualTo(2);

            budget.getPartitionings().add(partitioning);

            // when, then
            assertThat(keyTable.usedInPartitionItems().size()).isEqualTo(1);
            assertThat(keyTable.usedInPartitionItems()).contains(partitionItemUsingKeyTable);

        }

        @Rule
        public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

        @Mock PartitionItemRepository mockPartitionItemRepository;

        @Mock BudgetItem mockBudgetItem;

        @Test
        public void is_assigned_for_type_works_when_no_partition_items() throws Exception {

            // given
            KeyTable keyTable = new KeyTable();
            keyTable.partitionItemRepository = mockPartitionItemRepository;

            // expect
            context.checking(new Expectations() {{
                allowing(mockPartitionItemRepository).findByPartitioningTable(keyTable);
            }});

            // when
            assertThat(keyTable.isAssignedForTypeReason(BudgetCalculationType.BUDGETED)).isNull();
            assertThat(keyTable.isAssignedForTypeReason(BudgetCalculationType.AUDITED)).isNull();
        }

        @Test
        public void is_assigned_for_type_works_for_budgeted() throws Exception {

            // given
            KeyTable keyTable = new KeyTable();
            keyTable.partitionItemRepository = mockPartitionItemRepository;

            PartitionItem partitionItemBudgeted = new PartitionItem();
            partitionItemBudgeted.setBudgetItem(mockBudgetItem);

            // expect
            context.checking(new Expectations() {{
                oneOf(mockPartitionItemRepository).findByPartitioningTable(keyTable);
                will(returnValue(Arrays.asList(partitionItemBudgeted)));
                oneOf(mockBudgetItem).isAssignedForType(BudgetCalculationType.BUDGETED);
                will(returnValue(true));
                oneOf(mockBudgetItem).isAssignedForTypeReason(BudgetCalculationType.BUDGETED);
                will(returnValue("some reason"));
            }});

            // when
            String reason = keyTable.isAssignedForTypeReason(BudgetCalculationType.BUDGETED);

            // then
            assertThat(reason).isEqualTo("some reason");
        }

        @Test
        public void is_assigned_for_type_works_for_actual() throws Exception {

            // given
            KeyTable keyTable = new KeyTable();
            keyTable.partitionItemRepository = mockPartitionItemRepository;

            PartitionItem partitionItemActual = new PartitionItem();
            partitionItemActual.setBudgetItem(mockBudgetItem);

            // expect
            context.checking(new Expectations() {{
                oneOf(mockPartitionItemRepository).findByPartitioningTable(keyTable);
                will(returnValue(Arrays.asList(partitionItemActual)));
                oneOf(mockBudgetItem).isAssignedForType(BudgetCalculationType.AUDITED);
                will(returnValue(true));
                oneOf(mockBudgetItem).isAssignedForTypeReason(BudgetCalculationType.AUDITED);
                will(returnValue("some reason"));
            }});

            // when
            String reason =  keyTable.isAssignedForTypeReason(BudgetCalculationType.AUDITED);

            // then
            assertThat(reason).isEqualTo("some reason");

        }

        @Mock BudgetCalculationRepository mockBudgetCalculationRepository;

        @Test
        public void calculateFor_works() throws Exception {

            // given
            final BigDecimal partitionItemValue = new BigDecimal("1234.56");
            final BudgetCalculationType budgetCalculationType = BudgetCalculationType.BUDGETED;
            final KeyTable keyTable = new KeyTable();
            keyTable.budgetCalculationRepository = mockBudgetCalculationRepository;
            keyTable.setKeyValueMethod(KeyValueMethod.PROMILLE);
            keyTable.setPrecision(6);

            KeyItem keyItem1 = new KeyItem();
            keyItem1.setValue(new BigDecimal("1.00"));
            keyTable.getItems().add(keyItem1);

            KeyItem keyItem2 = new KeyItem();
            keyItem2.setValue(new BigDecimal("2.00"));
            keyTable.getItems().add(keyItem2);

            PartitionItem partitionItem = new PartitionItem();

            LocalDate calcStartDate = new LocalDate(2019,1,1);
            LocalDate calcEndDate = new LocalDate(2019,12,31);

            // expect
            context.checking(new Expectations(){{
                oneOf(mockBudgetCalculationRepository).findOrCreateBudgetCalculation(
                        partitionItem
                        , keyItem1
                        , new BigDecimal("1.234560")
                        , BudgetCalculationType.BUDGETED
                        , calcStartDate
                        , calcEndDate);
                oneOf(mockBudgetCalculationRepository).findOrCreateBudgetCalculation(
                        partitionItem
                        , keyItem2
                        , new BigDecimal("2.469120")
                        , BudgetCalculationType.BUDGETED
                        , calcStartDate
                        , calcEndDate);
            }});

            // when
            keyTable.calculateFor(partitionItem, partitionItemValue, budgetCalculationType, calcStartDate, calcEndDate);

        }

    }

}
