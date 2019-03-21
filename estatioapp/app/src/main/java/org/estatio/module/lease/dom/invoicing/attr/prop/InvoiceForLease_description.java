package org.estatio.module.lease.dom.invoicing.attr.prop;

import org.apache.isis.applib.annotation.Mixin;

import org.estatio.module.invoice.dom.attr.InvoiceAttributeName;
import org.estatio.module.lease.dom.invoicing.InvoiceForLease;

@Mixin(method="prop")
public class InvoiceForLease_description
        extends InvoiceForLease_attributeValueAbstract {

    public InvoiceForLease_description(final InvoiceForLease invoiceForLease) {
        super(invoiceForLease, InvoiceAttributeName.INVOICE_DESCRIPTION);
    }

}
