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

/** 
* ProcessServiceAmountFinder class. 
* 
* Executing a task which gets all Processing Fee amounts
*  
* @author gsUniversity
*/

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.GigaSpace;
import org.springframework.stereotype.Component;

import com.gigaspaces.async.AsyncFuture;
@Component
public class ProcessServiceAmountFinder {

	private final static Log log = LogFactory.getLog(ProcessServiceAmountFinder.class);
    @Resource
   
    private GigaSpace gigaSpace;
    
    @PostConstruct
    public void construct() throws Exception {
       
    	log.info("Calculate Process service amount from all processing fee");
    	
    	AsyncFuture<Double> future = gigaSpace.execute(new ProcessServiceAmountTask());
    	Double processingFeeAmount = future.get();
        if(processingFeeAmount > 0.0){
    	log.info("Processing Fee Amount is: " + processingFeeAmount);
        }else if(processingFeeAmount == 0.0){
        	log.info("BillBuddy profit is 0 (ZERO), Please run the payment-feeder project.");
        }
        
    }
}
