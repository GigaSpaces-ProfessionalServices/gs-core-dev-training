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

package com.mycompany.app.report;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

import com.mycompany.app.model.Merchant;
import com.gigaspaces.async.AsyncResult;

/**
 * ProcessServiceAmountTask class.
 * 
 * Task executing more than once (concurrently) and returns a result that is a
 * reduced operation of all the different executions and gets all Processing Fee
 * amounts
 * 
 * 
 * @author gsUniversity
 */

@SuppressWarnings("serial")
public class ProcessServiceAmountTask implements
		DistributedTask<Double, Double> {
	private final static Log log = LogFactory.getLog(ProcessServiceAmountTask.class);

	@TaskGigaSpace
	private transient GigaSpace gigaSpace;

	public Double execute() throws Exception {
    	log.info("ProcessServiceAmountTask- Start Execute.");
	
        Double processingFeesTotal= 0.0d;
   	
    	Merchant merchantTemplate = new Merchant();
    	Merchant[] merchants = gigaSpace.readMultiple(merchantTemplate, Integer.MAX_VALUE);
    	if(merchants!=null){
    	for(int i=0;i<merchants.length;i++){
    		processingFeesTotal+=merchants[i].getFeeAmount();
    	}}
    	
	    log.info("Number of Merchants found: " + merchants.length);
	    log.info("Total Fee Amount for current partition: " + processingFeesTotal);
	    log.info("ProcessServiceAmountTask- End Execute.");
	    
	    return processingFeesTotal;
    }

	public Double reduce(List<AsyncResult<Double>> results)
			throws Exception {
		log.info("MerchantByCategoryTask- Start reduce.");
		
		double processingFeeAmount = 0;

		for (AsyncResult<Double> result : results) {
			if (result.getException() != null) {
				throw result.getException();
			}

//			Collections.addAll(processingFees, result.getResult());
			processingFeeAmount+=result.getResult();
		}
		
		log.info("Processing Fee Amount is " + processingFeeAmount);
		log.info("ProcessServiceAmountTask- End reduce.");

		return processingFeeAmount;
	}

}
