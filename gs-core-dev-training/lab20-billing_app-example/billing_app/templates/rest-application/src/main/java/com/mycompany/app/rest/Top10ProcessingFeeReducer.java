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

import com.mycompany.app.model.ProcessingFee;

/** 
 *	{@link RemoteResultReducer} object used to reduce the results
 *	returned from the space.
 * 
 * @author gsUniversity
 */


public class Top10ProcessingFeeReducer implements RemoteResultReducer<ProcessingFee[], ProcessingFee[]> {

	private final Log log = LogFactory.getLog(Top10ProcessingFeeReducer.class);

    public ProcessingFee[] reduce(SpaceRemotingResult<ProcessingFee[]>[] results,
        SpaceRemotingInvocation remotingInvocation) throws Exception {
    	
    	log.info("Starting Top10MerchantFeeAmountReducer");
    	
        List<ProcessingFee> processingFees = new ArrayList<ProcessingFee>();

        // Each result is an array of events. Each result is from a single partition.        
        for (SpaceRemotingResult<ProcessingFee[]> result : results) {
            if (result.getException() != null) {
                // just log the fact that there was an exception
                log.error("Executor Remoting Exception [" + result.getException() + "]");

                continue;
            }
           	Collections.addAll(processingFees, result.getResult());
        }
        
        Collections.sort(processingFees,
                new Comparator<ProcessingFee>() {
                    public int compare(ProcessingFee p1, ProcessingFee p2) {
                        return p2.getAmount().compareTo(p1.getAmount());
                    }
        });

        // If the number of results needed is less than the number of events that were reduced, then
        // return a sublist. Otherwise, return the entire list of events.
        ProcessingFee[] top10ProcessingFee;
        if (processingFees.size() < 10){
        	top10ProcessingFee = new ProcessingFee[processingFees.size()];
        	processingFees.toArray(top10ProcessingFee);
        	
        }
        else {
        	top10ProcessingFee = new ProcessingFee[10];
        	processingFees.subList(0, 10).toArray(top10ProcessingFee);
        }
        return top10ProcessingFee;
    }
    
}
