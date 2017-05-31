package org.estatio.capex.dom.invoice.approval;

import javax.inject.Inject;

import org.apache.isis.applib.AbstractSubscriber;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.xactn.TransactionService;

import org.incode.module.document.dom.impl.paperclips.PaperclipRepository;

import org.estatio.capex.dom.documents.categorisation.invoice.IncomingDocAsInvoiceViewModel;
import org.estatio.capex.dom.documents.categorisation.invoice.IncomingDocAsInvoiceViewmodel_saveInvoice;
import org.estatio.capex.dom.invoice.IncomingInvoice;
import org.estatio.capex.dom.state.StateTransitionService;

@DomainService(nature = NatureOfService.DOMAIN)
public class IncomingInvoiceApprovalInitiatorSubscribingToViewModel extends AbstractSubscriber {

    //
    // TODO: should get rid of this when the upstream document state is implemented.
    //
    // (think it's not a harm to have both, behaviour of STS is kinda idempotent).
    //

    @Programmatic
    @com.google.common.eventbus.Subscribe
    @org.axonframework.eventhandling.annotation.EventHandler
    public void on(IncomingDocAsInvoiceViewmodel_saveInvoice.DomainEvent ev) {
        switch (ev.getEventPhase()) {
        case EXECUTED:
            final IncomingDocAsInvoiceViewModel viewModel = (IncomingDocAsInvoiceViewModel) ev.getMixedIn();
            final IncomingInvoice incomingInvoice = viewModel.getDomainObject();

            transactionService.flushTransaction();

            // an alternative design would be to just do this in IncomingDocAsInvoiceViewmodel_saveInvoice#act method
            stateTransitionService.trigger(incomingInvoice, IncomingInvoiceApprovalStateTransitionType.INSTANTIATE, null);
            break;
        }
    }

    @Inject
    PaperclipRepository paperclipRepository;

    @Inject
    StateTransitionService stateTransitionService;

    @Inject
    TransactionService transactionService;

}
