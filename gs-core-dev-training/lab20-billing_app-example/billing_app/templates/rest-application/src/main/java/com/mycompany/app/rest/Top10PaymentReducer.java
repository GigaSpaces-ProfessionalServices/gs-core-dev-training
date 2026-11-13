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

package com.mycompany.app.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.remoting.RemoteResultReducer;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;

import com.mycompany.app.model.Payment;

/** 
 *	{@link RemoteResultReducer} object used to reduce the results
 *	returned from the space.
 * 
 * @author gsUniversity
 */


public class Top10PaymentReducer implements RemoteResultReducer<Payment[], Payment[]> {

	private final Log log = LogFactory.getLog(Top10PaymentReducer.class);



	@Override
	public Payment[] reduce(SpaceRemotingResult<Payment[]>[] results,
			SpaceRemotingInvocation remotingInvocation) throws Exception {
		log.info("Starting Top10MerchantFeeAmountReducer");
		
		List<Payment> payments = new ArrayList<Payment>();

        // Each result is an array of events. Each result is from a single partition.        
        for (SpaceRemotingResult<Payment[]> result : results) {
            if (result.getException() != null) {
                // just log the fact that there was an exception
                log.error("Executor Remoting Exception [" + result.getException() + "]");

                continue;
            }
           	Collections.addAll(payments, result.getResult());
        }
        
        Collections.sort(payments,
                new Comparator<Payment>() {
                    public int compare(Payment p1, Payment p2) {
                        return p2.getPaymentAmount().compareTo(p1.getPaymentAmount());
                    }
        });

        // If the number of results needed is less than the number of events that were reduced, then
        // return a sublist. Otherwise, return the entire list of events.
        Payment[] top10Payment;
        if (payments.size() < 10){
        	top10Payment = new Payment[payments.size()];
        	payments.toArray(top10Payment);
        	
        }
        else {
        	top10Payment = new Payment[10];
        	payments.subList(0, 10).toArray(top10Payment);
        }
        return top10Payment;
	}
    
}
