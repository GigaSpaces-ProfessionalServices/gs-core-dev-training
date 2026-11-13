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

package com.mycompany.app.model;

import java.util.Date;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;


@SpaceClass
public class Contract {

	private String id;
	private Integer merchantAccountId;
	private Date contractDate;	
	private Double transactionPercentFee;
	
	@SpaceId(autoGenerate = true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@SpaceRouting
	public Integer getMerchantAccountId() {
		return merchantAccountId;
	}

	public void setMerchantAccountId(Integer merchantAccountId) {
		this.merchantAccountId = merchantAccountId;
	}

	public Date getContractDate() {
		return contractDate;
	}

	public void setContractDate(Date contractDate) {
		this.contractDate = contractDate;
	}

	public Double getTransactionPercentFee() {
		return transactionPercentFee;
	}

	public void setTransactionPercentFee(Double transactionPercentFee) {
		this.transactionPercentFee = transactionPercentFee;
	}


}
