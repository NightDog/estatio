package org.estatio.module.lease.dom.invoicing.attr.act;

import org.apache.isis.applib.annotation.Mixin;

import org.estatio.module.invoice.dom.attr.InvoiceAttributeName;
import org.estatio.module.invoice.dom.attr.Invoice_overrideAttributeAbstract;
import org.estatio.module.lease.dom.invoicing.InvoiceForLease;

@Mixin(method = "act")
public class InvoiceForLease_changePreliminaryLetterComment
                extends Invoice_overrideAttributeAbstract {

    public InvoiceForLease_changePreliminaryLetterComment(final InvoiceForLease invoice) {
        super(invoice, InvoiceAttributeName.PRELIMINARY_LETTER_COMMENT);
    }
}
