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


import com.mycompany.app.model.AccountStatus;
import com.mycompany.app.model.CategoryType;
import com.mycompany.app.model.Contract;
import com.mycompany.app.model.Merchant;
import org.openspaces.core.GigaSpace;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Merchant feeder class reads the merchant name list which is stored in merchants data member.
 * It then loops through the list, creates a Merchant object and writes it to the space.
 * It also creates Contract objects which is used to determine the transaction fee for a Merchant.
 * <p>
 */

//@Component
public class MerchantFeeder {

    private static final Logger logger = Logger.getLogger(MerchantFeeder.class.getName());

    @Resource
    private GigaSpace gigaSpace;

    private static final List<String> merchants = new ArrayList<String>();

    static {
        merchants.add("Like Pace");
        merchants.add("Konegsad");
        merchants.add("SomeDisk");
        merchants.add("Swakowsky");
        merchants.add("Green Head band");
        merchants.add("Shiruckou");
        merchants.add("Eagle");
        merchants.add("Lohitech");
        merchants.add("The musicals");
        merchants.add("SoccerMaster");
        merchants.add("Fort");
        merchants.add("2-Times");
        merchants.add("Mazalaty");
        merchants.add("jewelry 4 U");
        merchants.add("Gems");
        merchants.add("Hautika");
    }

    public MerchantFeeder() {}

    // This method creates merchants using list of merchant names from the
    // merchants data member and randomized information.
    // It then writes the merchant object into the space using a GigaSpace space proxy.
    @PostConstruct
    public void postConstruct() throws Exception {
        logger.log(Level.INFO, "Starting Merchant Feeder");
        logger.log(Level.INFO, "Method: loadData - loads all merchants into the space");

        // merchantAccountId will serve as the Unique Identifier value

        Integer merchantAccountId = 1;

        // for each merchant in the merchants list do:

        for (String merchantName : MerchantFeeder.merchants) {

            createMerchant(merchantAccountId, merchantName);
            merchantAccountId++;
        }

        logger.log(Level.INFO, "Stopping MerchantFeeder.");

    }

    @Transactional
    private void createMerchant(Integer merchantAccountId, String merchantName) {
        Merchant templateMerchant = new Merchant();
        templateMerchant.setMerchantAccountId(merchantAccountId);

        Merchant foundMerchant = gigaSpace.read(templateMerchant);

        if (foundMerchant == null) {

            Merchant merchant = new Merchant();

            merchant.setName(merchantName);
            merchant.setReceipts(0d);
            merchant.setFeeAmount(0d);

            // Select random category
            CategoryType[] categoryTypes = CategoryType.values();
            merchant.setCategory(categoryTypes[(int) ((categoryTypes.length - 1) * Math.random())]);
            merchant.setStatus(AccountStatus.ACTIVE);
            merchant.setMerchantAccountId(merchantAccountId);

            // Merchant is not found, let's add it.
            gigaSpace.write(merchant);

            logger.log(Level.INFO, String.format("Added Merchant object with name '%s'", merchant.getName()));

            createMerchantContract(merchantAccountId, gigaSpace);
        }

    }


    /**
     * Creates Contract with the terms between Merchant and BillBuddy
     */
    private static void createMerchantContract(Integer merchantId, GigaSpace gigaSpace) {

        Calendar calendar = Calendar.getInstance();

        Contract contract = new Contract();
        contract.setMerchantAccountId(merchantId);
        double randomTransactionFee = Double.parseDouble(new DecimalFormat("#.##").format(Math.random() / 10 + 0.01));
        contract.setTransactionPercentFee(randomTransactionFee);
        contract.setContractDate(calendar.getTime());


        // Write the document to the space:
        gigaSpace.write(contract);

        logger.log(Level.INFO, String.format("Added MerchantContract object with id '%s' with transaction fee of %.2f%n", contract.getId(), contract.getTransactionPercentFee()));

    }

}
