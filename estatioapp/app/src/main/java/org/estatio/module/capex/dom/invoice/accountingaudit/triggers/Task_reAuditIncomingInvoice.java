package org.estatio.module.capex.dom.invoice.accountingaudit.triggers;

import javax.annotation.Nullable;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.estatio.module.task.dom.task.Task;

@Mixin(method = "act")
public class Task_reAuditIncomingInvoice
        extends Task_mixinIncomingInvoiceAbstract<IncomingInvoice_reAudit> {

    protected final Task task;

    public Task_reAuditIncomingInvoice(final Task task) {
        super(task, IncomingInvoice_reAudit.class);
        this.task = task;
    }

    public static class ActionDomainEvent
            extends Task_mixinIncomingInvoiceAbstract.ActionDomainEvent<Task_reAuditIncomingInvoice> { }

    @Action(
            domainEvent = ActionDomainEvent.class,
            semantics = SemanticsOf.IDEMPOTENT
    )
    @ActionLayout(cssClassFa = "fa-check-circle")
    public Object act(
            @Nullable final String comment,
            final boolean goToNext) {
        final Object nextTaskIfAny = nextTaskOrWarnIfRequired(goToNext);
        Object mixinResult = mixin().act(comment, goToNext);
        return coalesce(nextTaskIfAny, mixinResult);
    }

    public boolean hideAct() {
        return super.hideAct() || mixin().hideAct();
    }

    public String disableAct() {
        if(doGetDomainObjectIfAny() == null) {
            return null;
        }
        return mixin().disableAct();
    }

    public boolean default1Act() {
        return true;
    }

}