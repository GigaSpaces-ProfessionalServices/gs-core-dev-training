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

import com.fasterxml.jackson.databind.ObjectMapper;

import com.mycompany.app.model.*;
import io.swagger.v3.oas.annotations.Operation;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/* functionality available:
    get objects: users, merchants
    totals and reducers: totals, top merchants, top payments, top processing fees
    drill-down: payments(type merchant | user, id), payment detail (payment id), processing fee (merchant id),
    merchantContract(merchant id)
    update: setTransactionPercentFee(merchant id, new fee)
 */

@Component
@RequestMapping()
@RestController
public class DataController {

    private static final Logger logger = Logger.getLogger(DataController.class.getName());

    @Resource
    private BillBuddyDal billBuddyDal;

    @Resource
    private GigaSpace space;

    @Value("${space.name}")
    private String spaceName;

    // for json output
    private ObjectMapper objectMapper;

    public DataController() {
    }

    @PostConstruct
    public void initialize() {
        logger.info("==========In postConstruct...");
        objectMapper = new ObjectMapper();
    }


    @Operation(summary = "Welcome")
    @GetMapping("/")
    public ResponseEntity<String> welcome(){
        return new ResponseEntity<>("Welcome to space Rest API example! Working with space:"  + spaceName, HttpStatus.OK);
    }

    // get objects:
    @Operation(summary="Get users", description="Returns the users")
    @PostMapping("/users")
    public ResponseEntity<String> users() throws Exception {
        logger.log(Level.INFO, "========== users was called:" + spaceName);
        //connectToSpace();

        List<User> list = UserDal.getInstance().getAllUsers();

        // show only the more important fields
        List<Map<String,String>> outputList = new ArrayList<Map<String,String>> ();

        for(User user : list) {
            Map<String,String> attributes = new HashMap();

            attributes.put("name", user.getName());
            attributes.put("balance", String.valueOf(user.getBalance()));
            attributes.put("credit limit", String.valueOf(user.getCreditLimit()));
            attributes.put("status", String.valueOf(user.getStatus()));
            outputList.add(attributes);
        }

        String json = objectMapper.writeValueAsString(outputList);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }


    @Operation(summary="Get merchants", description="Returns the merchants")
    @PostMapping("/merchants")
    public ResponseEntity<String> merchants() throws Exception {
        logger.log(Level.INFO, "========== merchants was called:" + spaceName);
        //connectToSpace();

        List<Merchant> list = MerchantDal.getInstance().getAllMerchants();
        String json = objectMapper.writeValueAsString(dumpMerchants(list));
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    // totals and reducers:
    @Operation(summary="Provide summary information for the application", description="Provide summary information for the application")
    @PostMapping("/totals")
    public ResponseEntity<String> totals() throws Exception {
        logger.log(Level.INFO, "========== totals was called:" + spaceName);
        //connectToSpace();

        Map<String,String> outputMap = new HashMap();

        Double revenue = billBuddyDal.getBillBuddyRevenue();
        outputMap.put("revenue", String.valueOf(revenue));

        Map<String,Integer> totals = billBuddyDal.getTotalObjectCount();
        outputMap.put("users", String.valueOf(totals.get("users")));
        outputMap.put("totals", String.valueOf(totals.get("merchants")));
        outputMap.put("payments", String.valueOf(totals.get("payments")));

        String json = objectMapper.writeValueAsString(outputMap);
        return  new ResponseEntity<>(json, HttpStatus.OK);
    }

    @Operation(summary="Get top 5 merchants", description="Get top 5 merchants based on merchant fee amounts")
    @PostMapping("/topmerchants")
    public ResponseEntity<String> topMerchants() throws Exception {
        logger.log(Level.INFO, "========== top merchants was called:" + spaceName);
        //connectToSpace();
        List<Merchant> list = billBuddyDal.getTop5Merchants();
        String json = objectMapper.writeValueAsString(dumpMerchants(list));
        return  new ResponseEntity<>(json, HttpStatus.OK);
    }

    @Operation(summary="Get top 10 payments", description="Get top 10 payments based on payment amounts")
    @PostMapping("/toppayments")
    public ResponseEntity<String> topPayments() throws Exception {
        logger.log(Level.INFO, "========== top payments was called:" + spaceName);
        //connectToSpace();
        List<Payment> list = billBuddyDal.getTop10Payments();
        String json = objectMapper.writeValueAsString(dumpPayments(list));
        return  new ResponseEntity<>(json, HttpStatus.OK);
    }

    @Operation(summary="Get top 10 processing fees", description="Get top 10 processing fees based on process fee amounts")
    @PostMapping("/topprocessingfees")
    public ResponseEntity<String> topProcessingFees() throws Exception {
        logger.log(Level.INFO, "========== top processing fees was called:" + spaceName);
        //connectToSpace();
        List<ProcessingFee> list = billBuddyDal.getTop10ProcessingFees();
        String json = objectMapper.writeValueAsString(dumpProcessingFees(list));
        return  new ResponseEntity<>(json, HttpStatus.OK);
    }

    // drill-down
    @Operation(summary="Get payments", description="Inputs are type merchant or user and the id")
    @PostMapping("/payments")
    public ResponseEntity<String> showPayments(@RequestBody Map<String,Object> properties) throws Exception {
        logger.log(Level.INFO, "========== show payments was called:" + spaceName);
        //connectToSpace();

        String type = (String) properties.get("type");
        Integer id = (Integer) properties.get("id");

        if( !"user".equalsIgnoreCase(type) && !"merchant".equalsIgnoreCase(type))
            return new ResponseEntity<>("Failed to run showPayments. Please input a type of user or merchant", HttpStatus.BAD_REQUEST);

        if( "user".equalsIgnoreCase(type)) {
            List<Payment> list = UserDal.getInstance().getUserPayments(new User(id));

            String json = objectMapper.writeValueAsString(dumpPayments(list));
            return new ResponseEntity<>(json, HttpStatus.OK);
        }
        else {
            List<Payment> list = MerchantDal.getInstance().getMerchantPayments(new Merchant(id));
            String json = objectMapper.writeValueAsString(dumpPayments(list));
            return new ResponseEntity<>(json, HttpStatus.OK);
        }
    }

    @Operation(summary="Get payment detail", description="Input is the payment id")
    @PostMapping("/paymentdetails")
    public ResponseEntity<String> paymentDetails(@RequestParam String id) throws Exception {
        logger.log(Level.INFO, "========== payment details was called:" + spaceName);
        //connectToSpace();
        List<Payment> list = new ArrayList();
        list.add(billBuddyDal.getPaymentById(id));
        String json = objectMapper.writeValueAsString(dumpPayments(list));
        return  new ResponseEntity<>(json.toString(), HttpStatus.OK);
    }

    @Operation(summary="Get processing fees", description="Input is the merchant id")
    @PostMapping("/processingfees")
    public ResponseEntity<String> processingFees(@RequestParam int id) throws Exception {
        logger.log(Level.INFO, "========== processing fees was called:" + spaceName);
        //connectToSpace();

        List<ProcessingFee> list = MerchantDal.getInstance().
                getMerchantProcessingFees(new Merchant(id));

        String json = objectMapper.writeValueAsString(dumpProcessingFees(list));
        return  new ResponseEntity<>(json, HttpStatus.OK);
    }

    @Operation(summary="Get contract", description="Input is the merchant id")
    @PostMapping("/contract")
    public ResponseEntity<String> contract(@RequestParam int id) throws Exception {
        logger.log(Level.INFO, "========== contract was called:" + spaceName);
        //connectToSpace();

        Contract contract = billBuddyDal.getContract(id);

        String output = String.format("%.2f%%", contract.getTransactionPercentFee() * 100);
        return  new ResponseEntity<>(output, HttpStatus.OK);
    }

    // update
    @Operation(summary="Update contract transaction percent fee", description="Input is the merchant id of the contract")
    @PostMapping("/settransactionpercentfee")
    public ResponseEntity<String> setTransactionPercentFee(@RequestBody Map<String,Object> properties) throws Exception {
        logger.log(Level.INFO, "========== set transaction percent fee was called:" + spaceName);
        //connectToSpace();

        Integer id = (Integer) properties.get("id");
        Double transactionPercentFee = (Double) properties.get("transactionPercentFee");

        logger.log(Level.INFO, String.format("id is: %d, type: %s", id, id.getClass().getCanonicalName()));
        logger.log(Level.INFO, String.format("transactionPercentFee is: %.2f, type: %s",
                transactionPercentFee, transactionPercentFee.getClass().getCanonicalName()));

        billBuddyDal.updateContractFee(id, transactionPercentFee);
        return  new ResponseEntity<>("transaction percent fee has been updated.", HttpStatus.OK);
    }
    /*
       helper methods for formatting output.
       Don't show all the fields, just show select ones.
     */
    private List<Map<String,String>> dumpMerchants(List<Merchant> list) {

        List<Map<String,String>> outputList = new ArrayList<Map<String,String>>();

        // return
        // name, category, receipts, fee amount, status
        for(Merchant merchant : list) {
            Map<String,String> attributes = new HashMap<String,String>();
            attributes.put("name",merchant.getName());
            attributes.put("category", String.valueOf(merchant.getCategory()));
            attributes.put("receipts", String.valueOf(merchant.getReceipts()));
            attributes.put("fee amount", String.valueOf(merchant.getFeeAmount()));
            attributes.put("status", String.valueOf(merchant.getStatus()));
            outputList.add(attributes);
        }
        return outputList;
    }

    private List<Map<String,String>> dumpPayments(List<Payment> list) {
        List<Map<String,String>> outputList = new ArrayList<Map<String,String>>();

        // return
        // created date, paying account id, receiving merchant id, description, payment amount, status
        for (Payment payment : list) {
            HashMap<String,String> attributes = new HashMap<String,String>();
            attributes.put("created date", String.valueOf(payment.getCreatedDate()));
            attributes.put("paying account id", String.valueOf(payment.getPayingAccountId()));
            attributes.put("receiving merchant id", String.valueOf(payment.getReceivingMerchantId()));
            attributes.put("description", String.valueOf(payment.getDescription()));
            attributes.put("payment amount", String.valueOf(payment.getPaymentAmount()));
            attributes.put("status", String.valueOf(payment.getStatus()));
            outputList.add(attributes);
        }

        return outputList;
    }

    private List<Map<String, String>> dumpProcessingFees(List<ProcessingFee> list) {
        List<Map<String,String>> outputList = new ArrayList<Map<String,String>>();

        StringBuilder sb = new StringBuilder();

        // return
        // payment id, description, amount, status, created date
        for (ProcessingFee processingFee : list) {
            Map<String,String> attributes = new HashMap<>();
            attributes.put("dependent payment id", String.valueOf(processingFee.getDependentPaymentId()));
            attributes.put("description", processingFee.getDescription());
            attributes.put("fee amount", String.valueOf(processingFee.getAmount()));
            attributes.put("status", String.valueOf(processingFee.getStatus()));
            attributes.put("created date", String.valueOf(processingFee.getCreatedDate()));
            outputList.add(attributes);
        }

        return outputList;
    }


}
