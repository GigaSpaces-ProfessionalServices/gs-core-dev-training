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

import jakarta.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;

import com.mycompany.app.model.CategoryType;
import com.mycompany.app.model.Merchant;
import com.mycompany.app.model.Payment;
import com.j_spaces.core.client.SQLQuery;

/** 
* ServiceFinder class. 
*  
* Implements IServiceFinder interface remoting capabilities on top of the space
* 
* @author gsUniversity
*/

@RemotingService
public class CategoryTransactionVolumeService implements ICategoryTransactionVolumeService {
	private final Log log = LogFactory.getLog(CategoryTransactionVolumeService.class);
	@Resource
	private GigaSpace gigaSpace;
	

	@Override
	public int findCategoryTransactionVolume(CategoryType categoryType) {
		log.info("Start TransactionsVolumeByCategoryTask service");
	    int transactionCount=0;
	    
	    // Search the space for all Merchant per selected category - prepare the query
	    
	    SQLQuery<Merchant> merchantQuery = new SQLQuery<Merchant>(Merchant.class, "category = ?");
	    merchantQuery.setParameter(1, categoryType);
	
	    // Execute Merchant query to the space
	    
	    Merchant[] merchantList = gigaSpace.readMultiple(merchantQuery, Integer.MAX_VALUE);
	    
	    // In case Merchant query result set has more then one Merchant
	    
	    if (merchantList.length > 0){
	    	
	    	SQLQuery<Payment> paymentQuery;
	    	log.info("Number of merchantList found: " + merchantList.length + " for category: " + categoryType);
	    	
	    	// Loop thru the list of Merchants
	    	
	    	for (Merchant merch : merchantList) {
			
	    		// Prepare Payment query to count number of Payments per each merchant
		    	
	    		paymentQuery = new SQLQuery<Payment>(Payment.class, "receivingMerchantId=?");
			    paymentQuery.setParameter(1, merch.getMerchantAccountId());
			    
			    // Execute query to count number of Payments per Merchant
			    
			    transactionCount += gigaSpace.count(paymentQuery);
	    	}

	    	log.info("Number of Transactions found: " + transactionCount + " for category: " + categoryType);
	    } else {
	    	log.info("No Merchants found for category: " + categoryType + " !!");
	    }
	    
	    log.info("TransactionsVolumeByCategoryTask- End Execute.");
	    return transactionCount;
	}
}
