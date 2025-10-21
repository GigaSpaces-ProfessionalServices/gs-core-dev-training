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

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.GigaSpace;

import org.springframework.transaction.annotation.Transactional;

import com.mycompany.app.model.AccountStatus;
import com.mycompany.app.model.Merchant;
import com.mycompany.app.model.Payment;
import com.mycompany.app.model.TransactionStatus;
import com.mycompany.app.model.User;
import com.gigaspaces.client.ChangeSet;
import com.gigaspaces.query.IdQuery;
//@Component
public class PaymentFeeder  {

    private static final Logger logger = Logger.getLogger(PaymentFeeder.class.getName());
    @Resource
    private GigaSpace gigaSpace;

    public void setGigaSpace(GigaSpace gigaSpace) {
        this.gigaSpace = gigaSpace;
    }

    private final Log log = LogFactory.getLog(PaymentFeederGenerator.class);
    private int userCount = 0;
    private int merchantCount = 0;
    private User user;
    private Merchant merchant;

    @PostConstruct
    public void construct() throws Exception {
        log.info("Starting PaymentWriter");

        userCount = gigaSpace.count(new User());
        if (userCount == 0) {
            log.info("Could not find users, did you write any?");
            //System.exit(-1);
        }
        

        merchantCount = gigaSpace.count(new Merchant());
        if (merchantCount == 0) {
            log.info("Could not find merchants, did you write any?");
            //System.exit(-1);
        }
       
    }
    
    @Transactional
    public void createPayment() {
    
        Random random = new Random();
        
        int userId = (int)(userCount * random.nextDouble());
        user = gigaSpace.readById(User.class, userId);
        
        int merchantId = (int)(merchantCount * random.nextDouble());
        merchant = gigaSpace.readById(Merchant.class, merchantId);
        
        if (user != null && merchant != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            Date date = calendar.getTime();

            Double paymentAmount = Double.valueOf(Math.random()*100);
            paymentAmount = Math.round(paymentAmount*100.0)/100.0;
            
            // Check If user valid and have credit limit
            if (user.getStatus() != AccountStatus.ACTIVE){
                log.info("User: " + user.getName() + " status is " + user.getStatus());
            }
            else if (user.getBalance() - paymentAmount < user.getCreditLimit()){
                log.info("User: " + user.getName() + " doesn't have credit.");
                
                Double addUserBalance = Double.valueOf(Math.random()*1000);
                addUserBalance = Math.round(addUserBalance*100.0)/100.0;
                
                log.info("Add " + addUserBalance + " to user balance");
                
                user.setBalance(user.getBalance()+ addUserBalance);
                gigaSpace.write(user);
                
            }
            else { 
                
                 // Withdraw payment amount from user account
                updateUserBalance(user, paymentAmount);

                // Deposit payment amount to merchant account                    
                updateMerchantReceipts(merchant, paymentAmount);
                
                // Create a Payment POJO and set it up.  
                Payment payment = new Payment();
                
                payment.setPayingAccountId(user.getUserAccountId());
                payment.setReceivingMerchantId(merchant.getMerchantAccountId());
                payment.setDescription(merchant.getCategory().name());
                payment.setCreatedDate(date);
                payment.setPaymentAmount(Math.round(paymentAmount*100.0)/100.0);
                payment.setStatus(TransactionStatus.NEW);
                
                // Write the payment object
                gigaSpace.write(payment);
                
                log.info("TransactionWriterTask wrote new transaction between user: " + user.getName() + 
                        " and merchant: " + merchant.getName());
            }
        }
    }

    private void updateUserBalance(User user, Double paymentAmount) {
        
        log.info("TransactionWriterTask withdraw " + paymentAmount + 
                " from user: " + user.getName());

        // Subtract paymentAmount from user balance using Change API       
        
        IdQuery<User> idQuery = new IdQuery<User>(User.class, user.getUserAccountId(), user.getUserAccountId());
        gigaSpace.change(idQuery, new ChangeSet().decrement("balance", paymentAmount));
        
    }
    
    private void updateMerchantReceipts(Merchant merchant, Double paymentAmount) {
        
        log.info("TransactionWriterTask deposit " + paymentAmount + 
                " to merchant: " + merchant.getName());
               
        // Add paymentAmount to Merchant receipts using Change API
        
        IdQuery<Merchant> idQuery = new IdQuery<Merchant>(Merchant.class, merchant.getMerchantAccountId(), merchant.getMerchantAccountId());
        gigaSpace.change(idQuery, new ChangeSet().increment("receipts", paymentAmount));
        
    }
}

