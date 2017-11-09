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
package org.estatio.module.capex.subscriptions;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.scratchpad.Scratchpad;

import org.estatio.module.capex.dom.invoice.IncomingInvoiceRoleTypeEnum;
import org.estatio.module.capex.dom.order.Order;
import org.estatio.module.capex.dom.order.OrderRepository;
import org.estatio.module.base.dom.UdoDomainService;
import org.estatio.module.invoice.dom.Constants;
import org.estatio.module.party.dom.Party;

@DomainService(
        nature = NatureOfService.DOMAIN
)
public class PartySubscriptionsForOrders extends UdoDomainService<PartySubscriptionsForOrders> {

    public PartySubscriptionsForOrders() {
        super(PartySubscriptionsForOrders.class);
    }

    @Programmatic
    @com.google.common.eventbus.Subscribe
    @org.axonframework.eventhandling.annotation.EventHandler
    public void on(final Party.DeleteEvent ev) {
        Party sourceParty = (Party) ev.getSource();
        Party replacementParty = ev.getReplacement();

        switch (ev.getEventPhase()) {
        case VALIDATE:

            if (replacementParty == null && orderRepository.findBySellerParty(sourceParty).size() > 0){
                ev.invalidate("Party is in use as seller in an order. Provide replacement");
            }

            break;
        case EXECUTING:

            if (replacementParty != null) {
                for (Order order : orderRepository.findBySellerParty(sourceParty)) {
                    order.setSeller(replacementParty);
                }
            }

            break;
        default:
            break;
        }
    }

    @Programmatic
    @com.google.common.eventbus.Subscribe
    @org.axonframework.eventhandling.annotation.EventHandler
    public void on(final Party.FixEvent ev) {
        switch (ev.getEventPhase()) {
        case EXECUTING:
            Party sourceParty = ev.getSource();
            if (orderRepository.findBySellerParty(sourceParty).size() > 0) {
                sourceParty.addRole(Constants.InvoiceRoleTypeEnum.SELLER);
                sourceParty.addRole(IncomingInvoiceRoleTypeEnum.SUPPLIER);
            }
            break;
        default:
            break;
        }

    }

    private transient UUID onPartyRemoveScratchpadKey;

    @Inject
    private Scratchpad scratchpad;

    @Inject
    private OrderRepository orderRepository;


}