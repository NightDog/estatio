package org.estatio.module.budgetassignment.dom;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.message.MessageService;

import org.incode.module.base.dom.valuetypes.LocalDateInterval;

import org.estatio.module.asset.dom.Unit;
import org.estatio.module.asset.dom.UnitRepository;
import org.estatio.module.budget.dom.budget.Budget;
import org.estatio.module.budget.dom.budgetcalculation.BudgetCalculation;
import org.estatio.module.budget.dom.budgetcalculation.BudgetCalculationRepository;
import org.estatio.module.budget.dom.budgetcalculation.BudgetCalculationType;
import org.estatio.module.budget.dom.budgetcalculation.InMemBudgetCalculation;
import org.estatio.module.budget.dom.budgetcalculation.Status;
import org.estatio.module.budgetassignment.dom.calculationresult.BudgetCalculationResult;
import org.estatio.module.budgetassignment.dom.calculationresult.BudgetCalculationResultLeaseTermLinkRepository;
import org.estatio.module.budgetassignment.dom.calculationresult.BudgetCalculationResultRepository;
import org.estatio.module.charge.dom.Charge;
import org.estatio.module.invoice.dom.PaymentMethod;
import org.estatio.module.lease.dom.InvoicingFrequency;
import org.estatio.module.lease.dom.Lease;
import org.estatio.module.lease.dom.LeaseAgreementRoleTypeEnum;
import org.estatio.module.lease.dom.LeaseItem;
import org.estatio.module.lease.dom.LeaseItemType;
import org.estatio.module.lease.dom.LeaseTermForServiceCharge;
import org.estatio.module.lease.dom.occupancy.Occupancy;
import org.estatio.module.lease.dom.occupancy.OccupancyRepository;

@DomainService(nature = NatureOfService.DOMAIN)
public class BudgetAssignmentService {

    public static Logger LOG = LoggerFactory.getLogger(BudgetAssignmentService.class);

    @Programmatic
    public List<BudgetCalculationResult> calculateResults(final Budget budget, final BudgetCalculationType type){

        List<BudgetCalculationResult> results = new ArrayList<>();
        for (Unit unit : unitRepository.findByProperty(budget.getProperty())) {
            results.addAll(calculatResultsForUnit(budget, type, unit));
        }
        return results;
    }

    @Programmatic
    public List<BudgetCalculationResult> calculatResultsForUnit(final Budget budget, final BudgetCalculationType type, final Unit unit) {

        List<BudgetCalculationResult> results = new ArrayList<>();
        final List<Occupancy> occupanciesForUnitDuringBudgetInterval = occupancyRepository.occupanciesByUnitAndInterval(unit, budget.getInterval());

        if (!occupanciesForUnitDuringBudgetInterval.isEmpty()) {

            if (overlappingOccupanciesFoundIn(occupanciesForUnitDuringBudgetInterval)) {
                String message = String.format("Overlapping occupancies found for unit %s", unit.getReference());
                message.concat(". No calculation results made for this unit.");
                messageService.warnUser(message);
                LOG.warn(message);
            } else {

                List<BudgetCalculation> calculationsForUnitAndType = budgetCalculationRepository.findByBudgetAndUnitAndType(budget, unit, type);
                List<Charge> invoiceChargesUsed = calculationsForUnitAndType.stream().map(c -> c.getInvoiceCharge()).distinct().collect(Collectors.toList());

                for (Occupancy occupancy : occupanciesForUnitDuringBudgetInterval) {

                    for (Charge charge : invoiceChargesUsed) {
                        BigDecimal value = BigDecimal.ZERO;
                        List<BudgetCalculation> calculationsForCharge = calculationsForUnitAndType.stream().filter(c -> c.getInvoiceCharge().equals(charge)).collect(Collectors.toList());
                        for (BudgetCalculation calc : calculationsForCharge) {
                            value = value.add(calc.getValue());
                        }
                        BudgetCalculationResult calcResult = budgetCalculationResultRepository.upsertBudgetCalculationResult(budget, occupancy, charge, type, value);
                        results.add(calcResult);
                    }

                }

                // finalize calculations
                calculationsForUnitAndType.stream().forEach(c -> c.setStatus(Status.ASSIGNED));

            }
        }

        return results;
    }

    boolean overlappingOccupanciesFoundIn(final List<Occupancy> occupancies){
        boolean overlappingOccupanciesFound = false;
        if (occupancies.size()>1) {
            List<LocalDateInterval> intervals = new ArrayList<>();
            for (Occupancy occupancy : occupancies){
                for (LocalDateInterval interval : intervals){
                    if (occupancy.getInterval().overlaps(interval)){
                        overlappingOccupanciesFound = true;
                        break;
                    }
                }
                intervals.add(occupancy.getInterval());
            }
        }
        return overlappingOccupanciesFound;
    }


    @Programmatic
    public void assignNonAssignedCalculationResultsToLeases(final List<BudgetCalculationResult> results) {

        List<BudgetCalculationResult> nonAssignedResults = results.stream().filter(r->budgetCalculationResultLeaseTermLinkRepository.findByBudgetCalculationResult(r).isEmpty()).collect(Collectors.toList());
        List<Occupancy> distinctOccupanciesInResults = nonAssignedResults.stream().map(r->r.getOccupancy()).distinct().collect(Collectors.toList());
        for (Occupancy occupancy : distinctOccupanciesInResults){

            List<BudgetCalculationResult> resultsForOccupancy = nonAssignedResults.stream().filter(r->r.getOccupancy().equals(occupancy)).collect(Collectors.toList());
            List<Charge> distinctInvoiceChargesForOccupancy = nonAssignedResults.stream().map(r->r.getInvoiceCharge()).distinct().collect(Collectors.toList());
            Lease lease = occupancy.getLease();

            if (resultsForOccupancy.size() != distinctInvoiceChargesForOccupancy.size()){
                // this should not be possible
                String message = String.format("Multiple budget calculation results with same invoice charge found for occupancy %s.", occupancy.title());
                message.concat(String.format("The calculation results were not assigned to lease %s.", lease.getReference()));
                messageService.warnUser(message);
                LOG.warn(message);
                break;
            }

            for (BudgetCalculationResult result : resultsForOccupancy){
                if (result.getType()==BudgetCalculationType.AUDITED && !result.getOccupancy().getEffectiveInterval().contains(result.getBudget().getInterval())){
                    String message = String.format("Occupancy %s for lease %s needs custom assignment for AUDITED because the occupancy does not cover the entire budget period", occupancy.title(), occupancy.getLease().getReference());
                    messageService.warnUser(message);
                    LOG.warn(message);
                } else {
                    LeaseItem serviceChargeItem = findOrCreateLeaseItemForServiceCharge(lease, result);
                    upsertLeaseTermForServiceCharge(serviceChargeItem, result);
                }
            }

        }

    }

    void upsertLeaseTermForServiceCharge(final LeaseItem serviceChargeItem, final BudgetCalculationResult result) {

        LeaseTermForServiceCharge termIfAny = (LeaseTermForServiceCharge) serviceChargeItem.findTerm(result.getBudget().getStartDate());
        if (termIfAny==null){
            termIfAny = (LeaseTermForServiceCharge) serviceChargeItem.newTerm(result.getBudget().getStartDate(), result.getBudget().getEndDate());
        }
        budgetCalculationResultLeaseTermLinkRepository.findOrCreate(result, termIfAny);
        recalculateTerm(termIfAny);
    }

    void recalculateTerm(final LeaseTermForServiceCharge term) {

        term.setBudgetedValue(null);
        term.setAuditedValue(null);

        final List<BudgetCalculationResult> resultsForTerm = budgetCalculationResultLeaseTermLinkRepository.findByLeaseTerm(term)
                .stream().map(l->l.getBudgetCalculationResult()).collect(Collectors.toList());
        for (BudgetCalculationResult result : resultsForTerm){

            BigDecimal newValue;

            switch (result.getType()){
            case BUDGETED:
                BigDecimal oldBudgeted = term.getBudgetedValue();
                newValue = oldBudgeted!=null ? oldBudgeted.add(result.getValue()) : result.getValue();
                term.setBudgetedValue(newValue);
                break;

            case AUDITED:
                BigDecimal oldActual = term.getAuditedValue();
                newValue = oldActual!=null ? oldActual.add(result.getValue()) : result.getValue();
                term.setAuditedValue(newValue);
                break;
            }

        }
    }

    LeaseItem findOrCreateLeaseItemForServiceCharge(final Lease lease, final BudgetCalculationResult calculationResult){

        LeaseItem leaseItem = lease.findFirstActiveItemOfTypeAndChargeInInterval(LeaseItemType.SERVICE_CHARGE, calculationResult.getInvoiceCharge(), calculationResult.getBudget().getInterval());

        if (leaseItem==null){
            LeaseItem itemToCopyFrom = findItemToCopyFrom(lease); // try to copy invoice frequency and payment method from another lease item
            leaseItem = lease.newItem(
                    LeaseItemType.SERVICE_CHARGE,
                    LeaseAgreementRoleTypeEnum.LANDLORD,
                    calculationResult.getInvoiceCharge(),
                    itemToCopyFrom!=null ? itemToCopyFrom.getInvoicingFrequency() : InvoicingFrequency.QUARTERLY_IN_ADVANCE,
                    itemToCopyFrom!=null ? itemToCopyFrom.getPaymentMethod() : PaymentMethod.DIRECT_DEBIT,
                    calculationResult.getBudget().getStartDate());
        }
        return leaseItem;
    }

    LeaseItem findItemToCopyFrom(final Lease lease){
        LeaseItem itemToCopyFrom = lease.findFirstItemOfType(LeaseItemType.SERVICE_CHARGE);
        if (itemToCopyFrom==null){
            // then try rent item
            itemToCopyFrom = lease.findFirstItemOfType(LeaseItemType.RENT);
        }
        if (itemToCopyFrom==null && lease.getItems().size()>0) {
            // then try any item
            itemToCopyFrom = lease.getItems().first();
        }
        return itemToCopyFrom;
    }

    /**
     * This method assumes to be called AFTER a reconciliation of the budget has been made
     * @param budget
     * @param lease
     * @return
     */
    @Programmatic
    public List<BudgetCalculationResult> calculateAuditedResultsForLease(final Budget budget, final Lease lease){
        List<BudgetCalculationResult> result = new ArrayList<>();
        if (budget.getStatus()!= org.estatio.module.budget.dom.budget.Status.RECONCILED) return result;
        for (Occupancy occupancy : lease.getOccupancies()){
            // check if the occupancy effective interval contains budget interval
            // if so, there should be calculation results already ...
            if (occupancy.getEffectiveInterval().contains(budget.getInterval())) {

                result.addAll(budgetCalculationResultRepository
                        .findByBudgetAndOccupancyAndType(budget, occupancy, BudgetCalculationType.AUDITED));

            } else {
                if (occupancy.getEffectiveInterval().overlaps(budget.getInterval())){
                    final List<InMemBudgetCalculation> inMemCalcs = budgetService
                            .auditedCalculationsForBudgetAndUnitAndCalculationInterval(budget, occupancy.getUnit(),
                                    occupancy.getEffectiveInterval());
                    List<BudgetCalculation> calculations = new ArrayList<>();
                    inMemCalcs.forEach(c->{
                        calculations.add(budgetCalculationRepository.findOrCreateBudgetCalculation(c));
                    });
                    // TODO: create BudgetCalculationResults
                    // TODO: set calculations to ASSIGNED
                }
            }
        }
        return result;
    }

    @Inject UnitRepository unitRepository;

    @Inject OccupancyRepository occupancyRepository;

    @Inject BudgetCalculationRepository budgetCalculationRepository;

    @Inject MessageService messageService;

    @Inject BudgetCalculationResultRepository budgetCalculationResultRepository;

    @Inject BudgetCalculationResultLeaseTermLinkRepository budgetCalculationResultLeaseTermLinkRepository;

    @Inject BudgetService budgetService;

}
