# gs-core-dev-training - lab11-space_based_remoting-solution

## 	Space Based Remoting

###### Lab Goals
1.  Experience developing a remote service.
2.  Understand the location of each class of the remote service (Interface, Service and client).
###### Lab Description
This lab includes 3 solutions:
1. 	Investigate a space based remoting service.
2.	Develop a distributed executor based remoting service.
3.	Develop an executor based remoting with @routing.
## 1 Lab setup
Make sure you restart the service grid and gs-ui (or at least undeploy all Processing Units using gs-ui)

```
$GS_HOME/bin/gs.sh host run-agent --auto --gsc=6
```
In another console window,
```
$GS_HOME/bin/gs-ui.sh
```

**1.1** Open gs-core-dev-training/lab11-space_based_remoting-solution project with Intellij (open pom.xml)<br>
**1.2** Run mvn package

    ~/gs-core-dev-training/lab11-space_based_remoting-solution$ mvn package
    
       [INFO] ------------------------------------------------------------------------
       [INFO] Reactor Summary:
       [INFO] 
       [INFO] lab11-solution 1.0-SNAPSHOT ........................ SUCCESS [  0.535 s]
       [INFO] BillBuddyModel ..................................... SUCCESS [  4.100 s]
       [INFO] BillBuddy_Space .................................... SUCCESS [  1.269 s]
       [INFO] BillBuddyAccountFeeder ............................. SUCCESS [  1.413 s]
       [INFO] BillBuddyPaymentFeeder ............................. SUCCESS [  1.270 s]
       [INFO] BillBuddyCountPaymentsByCategoryDistributedExecutor  SUCCESS [  1.236 s]
       [INFO] BillBuddyCountPaymentByCategoryRemoteService ....... SUCCESS [  1.148 s]
       [INFO] BillBuddyCategoryTop5PaymentRemoteService 1.0-SNAPSHOT SUCCESS [  1.123 s]
       [INFO] ------------------------------------------------------------------------
       [INFO] BUILD SUCCESS

**1.3** Copy the runConfigurations directory to the Intellij .idea directory to enable the Java Application configurations. Restart Intellij.
###### This will add the predefined Run Configuration Application to your Intellij IDE.

## 2	Investigate a space based remoting service
In this exercise we will examine an existing remote service code. 
You will not have to do much coding in this exercise. <br /> 
**2.1**	Expand the **BillBuddyCategoryTop5PaymentRemoteService** project. <br /> 
**2.2**	Add Remoting annotation support to your space (Hint pu.xml at BillBuddy-space). <br />
**2.3**	BillBuddy requires a Top 5 Payments per category query. 
The service was implemented. Please answer the following questions about the remote service: <br />

>a.	Where is the service interface located? In which project? Why? Locate it please. <br /> 
b.	Where is the actual Remote Service code located? Why? Locate it please. <br />
c.	Where is client code located? Locate it please. <br />
d.	Why is the CategoryTop5PaymentReducer not located with the service? <br />
e.	Can you assign a different type of Reducer to the same Remote Service?
f.	Is the service implemented as Executor Based Remoting? Or is it implemented as Event Based Remoting?

## 3	Develop a Distributed Executor Based Remoting
**3.1**	BillBuddy is required to generate a “Payment Count Per Category” report. 
A Payment Count report includes a count of all the payments made in a specific category.
The BillBuddyCountPaymentByCategoryDistributedExecutor project is provided as part of the Lab. 
You can use it in order to complete any coding gaps you might have. <br />
**3.2**	 Expand the BillBuddyModelproject. <br /> 
**3.3**	 Add the required method in the Interface com.gs.billbuddy.remoting.ICountPaymentsByCategoryService <br />

>a.	Declare a method named findPaymentCountByCategory that will retrieve a count of all payments made in a specific category. <br /> 

**3.4**	 Implement the Service as follows: <br />
>a.	Locate the com.gs.billbuddy.remoting.CountPaymentByCategoryService (Hint: BillBuddy_space). <br />
b.	Follow the //TO DO comments in the file. <br />

**3.5**	 Implement the Reducer class as follows: <br />
>a.	Expand the BillBuddyCountPaymentByCategoryRemoteService project. <br />
b.	Locate the com.gs.billbuddy.report.CountPaymentByCategoryReducer. <br />
c.	Follow the //TO DO comments in the file. 
You only have to add all results to a single list. 
The reducer class is almost fully implemented. <br />

**3.6**	 Implement the Client as follows: <br />
>a.	Expand the BillBuddyCountPaymentByCategoryRemoteService project. <br />
b.	Locate the com.gs.billbuddy.report.CountPaymentByCategoryReport. <br />
c.	Follow the //TO DO comments in the file. <br />
d.	Deploy the BillBuddy_Space PU to the Service Grid. <br /> 
e.	Run AccountFeeder and PaymentFeeder (using the RunConfigurations in Intellij) <br />
f.	Run the **BillBuddyCountPaymentByCategoryRemoteService** (using the RunConfigurations in Intellij or by deploying the Processing Unit jar file) and examine your results. 

## 4	Develop Executor Based Remoting using @routing
**4.1**	BillBuddy requires a profit report for a random merchant. 
A merchant's profit is calculated via the following formula: (receipts – feeAmount). 
You can use the Top10Payment distributed Executor Based Remoting as a reference. <br />
**4.2**	We will use the IMerchantProfitService interface. 
Complete the missing code in the Interface (Hint: located in the …model…). <br />
**4.3**	Implement the Service in MerchantProfitService. (Hint: located in the space…). <br />
>a.	The service will return a specific merchant's calculated profit. <br />
b.	Remember that the merchantAccountId is also the merchant's routing attribute. 
Add the relevant annotation to enable the service to use routing. <br />

**4.4**	Implement the Client MerchantProfitReport as follows: <br />
>a.	Expand the BillBuddyMerchantProfitRemoteService. <br />
b.	Select a random merchant from the space (Hint: count the merchants and randomize a number within the count result). <br />
Use the IMerchantProfitService interface to return the profit for the random MerchantAccountId and assign it to the merchant object. <br />

**4.5**	Deploy the BillBuddy_Space PU to the Grid Service followed by 
AccountFeeder and PaymentFeeder. <br />
**4.6**	Run and test the **BillBuddyMerchantProfitRemoteService** service (using the RunConfigurations in Intellij or by deploying the Processing Unit jar file).

