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

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.openspaces.core.GigaSpace;
import org.springframework.stereotype.Component;

import com.mycompany.app.model.Contract;
import com.mycompany.app.model.Merchant;
import com.mycompany.app.model.Payment;
import com.mycompany.app.model.ProcessingFee;
import com.j_spaces.core.client.SQLQuery;
/** 
 *	Data Access Layer design pattern.
 *	
 *	Enables access to Administrative Merchant operations(getMerchant, totalProcessingFee, etc.)
 *
 *	Dal object is injected by Spring framework as a singleton, thus, BillBuddy space 
 *	also injected to Dal instance.
 * 
 * @author gsUniversity
 */
@Component
public class MerchantDal {
	
	private static MerchantDal instance;
	
	@Resource
	private GigaSpace gigaSpace;
	
	
	public static MerchantDal getInstance(){
		return instance;
	}
	public MerchantDal() {
	}

	@PostConstruct
	@SuppressWarnings("unused")
	private void init(){
		instance = this;
	}
	public List<Merchant> getAllMerchants() {
		List<Merchant> merchantList = null;
		SQLQuery<Merchant> merchantQuery = new SQLQuery<Merchant>(Merchant.class,"order by merchantAccountId");
		Merchant[] merchants = gigaSpace.readMultiple(merchantQuery,
				Integer.MAX_VALUE);
		if (merchants != null) {
			merchantList = Arrays.asList(merchants);
		}

		return merchantList;
	}

	public List<Payment> getMerchantPayments(Merchant merchant) {
		List<Payment> merchantPaymentList = null;
		SQLQuery<Payment> paymentQuery = new SQLQuery<Payment>(Payment.class,"receivingMerchantId = ? order by paymentId");
		paymentQuery.setParameter(1, merchant.getMerchantAccountId());
		
		Payment[] merchantPayments = gigaSpace.readMultiple(paymentQuery,
				Integer.MAX_VALUE);
		
		if (merchantPayments != null) {
			merchantPaymentList = Arrays.asList(merchantPayments);
		}
		return merchantPaymentList;
	}

	public List<ProcessingFee> getMerchantProcessingFees(Merchant merchant) {
		List<ProcessingFee> merchantProcessingFeesList = null;
		SQLQuery<ProcessingFee> processingFeeQuery = new SQLQuery<ProcessingFee>(ProcessingFee.class,"payingAccountId = ? order by processingFeeId");
		processingFeeQuery.setParameter(1, merchant.getMerchantAccountId());
		ProcessingFee[] merchantProcessingFees = gigaSpace.readMultiple(processingFeeQuery,
				Integer.MAX_VALUE);

		if (merchantProcessingFees != null) {
			merchantProcessingFeesList = Arrays.asList(merchantProcessingFees);
		}

		return merchantProcessingFeesList;
	}
	public List<Contract> getMerchantContractDocument(Merchant merchant) {
		List<Contract> merchantContractDocumentsList = null;
		SQLQuery<Contract> contractDocumentQuery = new SQLQuery<Contract>(Contract.class,"merchantId = ?");
		contractDocumentQuery.setParameter(1, merchant.getMerchantAccountId());
		Contract[] merchantContractDocuments = gigaSpace.readMultiple(contractDocumentQuery,
				Integer.MAX_VALUE);

		if (merchantContractDocuments != null) {
			merchantContractDocumentsList = Arrays.asList(merchantContractDocuments);
		}

		return merchantContractDocumentsList;
	}
	
	public Double totalProcessingFee(Merchant merchant){
		Double totalFeeAmount = 0D;
		
		List<ProcessingFee> processingFeeList = getMerchantProcessingFees(merchant);
		
		for(ProcessingFee pf:processingFeeList){
			totalFeeAmount += pf.getAmount();
		}
		
		return totalFeeAmount;
	}
}
