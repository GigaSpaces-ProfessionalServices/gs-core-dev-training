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

package com.mycompany.app.feeder;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/** 
 * Payment Feeder class read user and merchant randomly  
 * by Spring framework. 
 *  
 * This class been injected by Spring framework and it 
 * performs loop on userNameList creates User class and writes it into space.
 * 
 * @author gsUniversity
 */

//@Component
public class PaymentFeederGenerator {
    private final Log log = LogFactory.getLog(PaymentFeederGenerator.class);
    
    @Resource
    private PaymentFeeder paymentFeeder;

 
    @PostConstruct
    public void construct() throws Exception {
        log.info("Starting PaymentFeeder");
        
        // Create a new Thread to handle writing a new payments
        
        Thread t = new Thread(new PaymentCreatorExecuter());
        t.start();
    }


	private PaymentFeeder getPaymentFeeder() {
		return paymentFeeder;
	}

	
	// Thread class in charge of creating a payment every second
	private class PaymentCreatorExecuter implements Runnable {
		private long defaultDelay = 1000;
	    public void run() {
	        try {
	        	log.info("PaymentFeeder.PaymentCreatorExecuter thread has started...");
	        	while (true){
	        		
	        		// Create a payment 
	        		
	        		getPaymentFeeder().createPayment();
	        		
	        		// Create a delay between payments 
	        		
	        		Thread.sleep(defaultDelay);
	        	}
	        } catch (InterruptedException e) {
	        	log.error("PaymentFeeder.PaymentCreatorExecuter has failed.");
	        }
	    }
    }
    
}

