package org.estatio.capex.dom.documents.categorisation;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;

import org.joda.time.LocalDateTime;

import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;

import org.incode.module.document.dom.impl.docs.Document;

import org.estatio.capex.dom.invoice.approval.IncomingInvoiceApprovalStateTransition;
import org.estatio.capex.dom.state.StateTransitionAbstract;
import org.estatio.capex.dom.state.StateTransitionRepositoryAbstract;
import org.estatio.capex.dom.task.Task;

import lombok.Getter;
import lombok.Setter;

@PersistenceCapable(
        identityType = IdentityType.DATASTORE,
        schema = "incomingDocument",
        table = "IncomingDocumentCategorisationStateTransition"
)
@DatastoreIdentity(
        strategy = IdGeneratorStrategy.IDENTITY,
        column = "id"
)
@javax.jdo.annotations.Inheritance(
        strategy = InheritanceStrategy.NEW_TABLE)
@Queries({
        @Query(
                name = "findByDomainObject", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition "
                        + "WHERE document == :domainObject "
                        + "ORDER BY completedOn DESC "
        ),
        @Query(
                name = "findByDomainObjectAndCompleted", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition "
                        + "WHERE document == :domainObject "
                        + "&& completed == :completed "
                        + "ORDER BY completedOn DESC "
        ),
        @Query(
                name = "findByTask", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition "
                        + "WHERE task == :task "
        ),
})
@DomainObject(objectType = "incomingDocument.IncomingDocumentCategorisationStateTransition" )
public class IncomingDocumentCategorisationStateTransition
        extends StateTransitionAbstract<
                    Document,
        IncomingDocumentCategorisationStateTransition,
        IncomingDocumentCategorisationStateTransitionType,
        IncomingDocumentCategorisationState> {


    /**
     * For the first transition, represents the initial state of the domain object
     * Thereafter, will hold the same value as the "to state" of the preceding transition.
     */
    @Column(allowsNull = "false")
    @Getter @Setter
    private IncomingDocumentCategorisationState fromState;

    @Column(allowsNull = "false")
    @Getter @Setter
    private IncomingDocumentCategorisationStateTransitionType transitionType;

    /**
     * If null, then this transition is not yet complete.
     */
    @Column(allowsNull = "true")
    @Getter @Setter
    private IncomingDocumentCategorisationState toState;

    @Column(allowsNull = "false", name = "documentId")
    @Getter @Setter
    private Document document;


    /**
     * Not every transition necessarily has a task.
     */
    @Column(allowsNull = "true", name = "taskId")
    @Getter @Setter
    private Task task;

    @Column(allowsNull = "false")
    @Getter @Setter
    private LocalDateTime createdOn;

    @Getter @Setter
    @Column(allowsNull = "true")
    private LocalDateTime completedOn;

    @Getter @Setter
    @Column(allowsNull = "false")
    private boolean completed;

    @Programmatic
    @Override
    public Document getDomainObject() {
        return getDocument();
    }

    @Programmatic
    @Override
    public void setDomainObject(final Document domainObject) {
        setDomainObject(domainObject);
    }

    @DomainService(
            nature = NatureOfService.DOMAIN,
            repositoryFor = IncomingInvoiceApprovalStateTransition.class
    )
    public static class Repository
            extends StateTransitionRepositoryAbstract<
                        Document,
                        IncomingDocumentCategorisationStateTransition,
            IncomingDocumentCategorisationStateTransitionType,
                        IncomingDocumentCategorisationState> {

        public Repository() {
            super(IncomingDocumentCategorisationStateTransition.class);
        }

    }

}
