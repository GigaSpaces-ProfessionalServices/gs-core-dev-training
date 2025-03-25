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

import com.mycompany.app.model.Merchant;

/**
 * ServiceFinder class.
 * 
 * Implements IServiceFinder interface remoting capabilities on top of the space
 * 
 * @author gsUniversity
 */

@RemotingService
public class MerchantProfitService implements IMerchantProfitService {
	private final Log log = LogFactory.getLog(MerchantProfitService.class);
	@Resource
	private GigaSpace gigaSpace;

	@Override
	public Double getMerchantProfit(Integer merchantAccountId) {
		log.info("Start Merchant Profit service ");

		Double merchantProfit = 0d;
		Merchant merchant = gigaSpace.readById(Merchant.class, merchantAccountId);
		if (merchant != null) {
			merchantProfit = merchant.getReceipts() - merchant.getFeeAmount();
			log.info("Profit for " + merchant.getName() + " is: "
					+ merchantProfit + "");
		}
		log.info("Finish Merchant Profit service for " + merchant.getName()
				+ "...");

		return merchantProfit;
	}

}
