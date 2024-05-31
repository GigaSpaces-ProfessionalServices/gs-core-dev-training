/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycompany.app.remoting;

import javax.annotation.Resource;

import com.mycompany.app.model.Merchant;
import com.mycompany.app.model.Payment;
import com.mycompany.app.model.ProcessingFee;
import com.mycompany.app.remoting.IServiceFinder;
import com.j_spaces.core.client.SQLQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.springframework.beans.factory.annotation.Qualifier;

/** 
* ServiceFinder class. 
*  
* Implements IServiceFinder interface remoting capabilities on top of the space
* 
* @author gsUniversity
*/

@RemotingService
public class ServiceFinder implements IServiceFinder {
	private final Log log = LogFactory.getLog(ServiceFinder.class);
	@Resource
	@Qualifier(value = "gigaSpace")
	private GigaSpace gigaSpace;

	public Payment[] findTop10Payments() {

		log.info("Find Top 10 payment merchant over the space");

		SQLQuery<Payment> query = new SQLQuery<Payment>(Payment.class,
				" order by paymentAmount desc");
		Payment[] payments = gigaSpace.readMultiple(query, 10);

		log.info("Found: " + payments.length + " payments.");

		return payments;
	}

	public Merchant[] findTop5MerchantFeeAmount() {
		log.info("Find Top 5 merchant fee amount over the space");

		SQLQuery<Merchant> query = new SQLQuery<Merchant>(Merchant.class,
				" order by feeAmount desc");
		Merchant[] merchants = gigaSpace.readMultiple(query, 5);

		log.info("Found: " + merchants.length + " merchants.");

		return merchants;

	}
	public ProcessingFee[] findTop10ProcessingFees() {
		log.info("Find Top 10 Processing fees over the space");

		SQLQuery<ProcessingFee> query = new SQLQuery<ProcessingFee>(ProcessingFee.class,
				" order by amount desc");
		ProcessingFee[] processingFees = gigaSpace.readMultiple(query, 10);

		log.info("Found: " + processingFees.length + " merchants.");
		return processingFees;
	}
}
