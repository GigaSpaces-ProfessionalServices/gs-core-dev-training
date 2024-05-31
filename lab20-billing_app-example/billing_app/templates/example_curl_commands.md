## List of curl command examples to access the REST application

Reminder: replace `localhost:8080` with the host and port numbers to match your environment.


1. Get users
`curl -X POST localhost:8080/users`
2. Get merchants
`curl -X POST localhost:8080/merchants`

3. Get totals summary for the application  

|totals | command |
|---|---|
|totals | `curl -X POST localhost:8080/totals` |
|top merchants | `curl -X POST localhost:8080/topmerchants`|
|top payments | `curl -X POST localhost:8080/toppayments`|
|top processing fees | `curl -X POST localhost:8080/topprocessingfees`|

4. Get payments of type merchant or user and provide the id of the merchant or the user.
`curl -X POST -H 'Content-type:application/json' -d '{"type": "merchant", "id": 1 }' localhost:8080/payments`

5. Get merchant details

|merchant details| input | command |
|---|---|---|
|processing fee | merchant id | `curl -X POST localhost:8080/processingfees?id=1` |
|contract info | merchant id | `curl -X POST localhost:8080/contract?id=1`|

6. Update the transaction percent fee of a merchant.
`curl -X POST -H 'Content-type:application/json' -d '{"id": 1, "transactionPercentFee": 0.25 }' localhost:8085/settransactionpercentfee`









