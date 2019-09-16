## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Solution
I am new in Kotlin, so I spend a couple of days during my free time to learn enough to get myself started.

### Designing 
When I think about payments, the first thing that pops up in my mind is what you do if payment failed? Well, you retry, and after some attempts, you might want to notify the user.
When we look at this problem from the DDD perspective, scheduling, retrying, and notifying are three different domains (at least for me). And thus, separating them feels natural, regardless of your infrastructure specifics (monolithic, microservice or lambda). 

Once having said that, the idea is to have two micro-services one for schedule and another for retries. The notification domain will live within the retry microservice. Both micro-services will communicate via events (I'll use RabbitMQ). I have to say that the notification system will be rather simple, probably just logging. The point is to show the possibility to work and maintain, even in this hybrid infrastructures with ease.

The `scheduler` microservice is going to hold the customer and invoice information. It will be capable of committing a payment as well. While the `retry` microservice will know about every payment for a given invoice. And in case of a failed payment will decide whether to retry it or not.

In the following sections, I will describe every step that took me from the initial challenge to its final state. And after that, I will explain in greater detail the final design, and last but not least, some conclusions and potential improvements.

### Iterations
`Iteration 1` - I did the app refactor, no logic changed. I just made sure everything still works as it was. This commit is, perhaps, the largest commit this project will have.

`Iteration 2` - Added the payment-schedule, which marks the invoices as "Ready to process" (every month), is worth noticing that this step does not execute the payment only mark it as `scheduled`.

`Iteration 3` - The payments processor was added. It will fetch all the payments marked as `scheduled`, then mark them as `processing` and finally publish a message to the bus (Mocked for now). This event will in future iterations be consumed by the scheduler micro-service to commit the payment finally.

`Iteration 4` - Created the docker-compose which includes the RabbitMQ Image and topology + new set up scripts. The topology that `setup-events-topology` image creates emulates the topic-service of Kafka where separate consumer groups are entirely independent, thanks to that. They all see all messages from the topics they are consuming. To understand what I mean, go to `localhost:15672` user and pasword: `guest` and then go to queues. (You can only see this after both services are running since the topology gets created at startup time)

`Iteration 5` - Removed the mocked bus publisher and added the good one `pleo-bus`, for now, just publishing the topic `InvoiceScheduledEvent`.

`Iteration 6` - Add the commit payment logic, the exceptions `CustomerNotFoundException` and `CurrencyMismatchException` produce a log that could potentially be a notification to be discarded finally. However, the `NetworkException`  will send the message to the DLX for future retrial. If there is neither data nor infra exceptions an event gets created for both cases `InvoicePayCommitSucceedEvent` and `InvoicePayCommitFailedEvent` with a failed reason, i.e., "Not enough founds." 

`Iteration 7` - I managed to enable the Dead-Letter queue, so now the whole process of `commit payment` is transactional.

`Iteration 8` - Added the `InvoiceScheduledEvent` handler, which ultimately commits the payment.

`Iteration 9` - Added the health check service for the bus.

`Iteration 10` - Added the `MontlyEvent` which emulates "that" external factor that starts the process (every 1st of each month).

`Iteration 11` - Added the `pleo-antaeus-retrier` service, which will run in an independent container. It will communicate with the `pleo-antaeus-scheduler` via RabbitMQ events.

`Iteration 12` - Added the payment logic using `EvenSourcing`, so we get the latest state of the PaymentInvoice upon which we are going to decide if is worth retrying.

`Iteration 13` - Wire-up the `retrier` service, it emits 2 events `InvoicePayRetryApprovedEvent` and `InvoicePayRetryExceededEvent`. Which will be used to either retry the payment or mark it as failed. The `InvoicePayRetryExceededEvent` will be used for the notification domain as well.

`Iteration 14` - Hooked up the `InvoicePayRetryApprovedEvent` and `InvoicePayRetryExceededEvent` into the `pleo-antaeus-scheduler`. So now payments are either committed again or marked as  `FAILED`.

`Iteration 15` - Created the notification domain using the `InvoicePayRetryExceededEvent`. It's just a log since it's to prove that the same event can be used for two micro-services independently. You'll see in the logs a message like `Dear customer your invoice with ID <id> ...`, if you use that invoice ID to ask the `scheduler` service [http://localhost:7000/rest/v1/invoices/\<id\>](http://localhost:7000/rest/v1/invoices/:id), you will see a `FAILED` status for that specific invoice. Notice both processes happen in 2 different containers, but everything is sync thanks to the bus.

`Iteration 16` - Now the Bus topology is created at start up time, you'll have to wait for both services to fully start for it to show in the RabbitMQ Management Tool: [localhost:15672](localhost:15672) - user `guest` password `guest`.

`Iteration 17` - Added few logic to the `utils/paymentProvider`. Now there is a probability of 3% that one of the documented exceptions happen with  `NetworkException` being the one with the highest likelihood. Those exceptions add some realism to the project. Notice how in the case of `NetworkException` the event lands in the `dlx` queue.

`Iteration 18` - And last but not least, added a small rest API in the `pleo-antaeus-retrier` to get all the payment attempts for a given invoice ID. [http://localhost:7001/rest/v1/payments/\<invoiceId\>](http://localhost:7001/rest/v1/payments/:invoiceId). Remember the `Dear customer your invoice with ID <id> ...` log message. Well, you can use the that `id` to hit the endpoint and see all the failed attempts.

### Final design

Uff!! that was something! But we get through it :). Before the wrap-up, I'd like to explain in-depth the final design, that will perhaps help you to walk through it with ease. 

First you'll have to run the `./script/server`. After you run it, a few things will happen.
It will fetch if needed a couple of Docker images
- It will fetch if needed a couple of Docker images
- It will build the project in each container; if is the first time it will take a while, after that gets faster. 
- Once started, It will create the *bus topology*: exchanges and topics. 

Now you'll have to make sure everything is running, `pleo-antaeus-retrier` and `pleo-antaeus-scheduler` expose an HTTP port `7000` and `7001` respectively. You can do that by hitting their health checks endpoints.

- `pleo-antaeus-scheduler` - [http://localhost:7000/rest/health](http://localhost:7000/rest/health)
- `pleo-antaeus-retrier` - [http://localhost:7001/rest/health](http://localhost:7001/rest/health)

If both services are running then, you are good to go. In case something fail just, hit `./script/down` and `./script/server` again and it should be fine. 

The 1st of "this month" has come so is time to schedule some payments, to start the whole process you'll have to publish a message manually (It will be the only one). This event emulates that external factor that will result in real live trigger this process i.e., a CronJob in Kubernetes.

In order to do so, you'll have to go to the RabbitMQ Management Tool: [localhost:15672](localhost:15672) user - `guest` password - `guest`. In the tab bar click in `Queues` and from the list click over `scheduler:MonthlyEvent`.
That will take you to the queue overview. Scroll a bit down till you see the `Publish message` expand it if collapsed and finally click on the `Publish message` button, don't worry about the payload or anything; that will effectively publish an empty message which is enough.

Now, click the `Queues` tab and look at how messages start going back and forth, if you look at your console, you will see some logs related to the app popping up.

Ok, that's nice, but it is time to see what those messages mean and what is happening.

#### Process description

Everything starts with the `MonthlyEvent`, and what that does is to mark every `PENDING` payments as `SCHEDULED`. Then another process that is running every 5 seconds will take those invoices in batches of 10, mark them as `PROCESSING` to finally publish a message to the bus the `InvoiceScheduledEvent`.

This event gets consumed and finally commits the payment with the `payment provider`; if the payment is successful the invoice is marked as `PAID` otherwise is marked as `RETRY` in both cases an event is published `InvoicePayCommitSucceedEvent` and `InvoicePayCommitFailedEvent`.  However, the payment provider has a hardcoded 3% probability of throwing an exception with `NetworkException`  being the most likely. In the case of that, the message goes to the Dead-Letter queue (dlx). If you take a look at the `Queues` tab after the process is finished, you may see some stale messages in the `*-dlx` queues those can be re-queued if you'd like. But it has to be manually since it's not the user's fault; it is more like an infra issue.

Ok, getting back to the point where the payment failed to commit because the user, for instance, didn't have enough funds. Publish an event `InvoicePayCommitFailedEvent` that event gets consumed and aggregated together with the `InvoicePayCommitSucceedEvent`; using event sourcing to determine if the payment is going to retry.

In case the retry has approved the event `InvoicePayRetryApprovedEvent` is published otherwise `InvoicePayRetryExceededEvent` the first event is consumed, and the process of committing a payment starts again. The second one is consumed by both services one "notifies the user", by logging :p, and the second marks the invoice as `FAILED`. 

You can check the payment history for a given invoice with this endpoint: `http://localhost:7001/rest/v1/payments/:invoiceId`
Look at the ``Dear customer ..`` log in order to get some useful invoice ID. You can use the `http://localhost:7000/rest/v1/invoices/:invoiceId` with the same ID and the status should be `FAILED`, since both things happen due to the same event.

#### API
- **pleo-antaeus-scheduler** - [http://localhost:7000](http://localhost:7000)
    * HTTP
        * /rest/health
        * /rest/v1/invoices
        * /rest/v1/invoices/:id
        * /rest/v1/customers
        * /rest/v1/customers/:id

    * Bus
        * Consumes
            * `MonthlyEvent` - The only manual event, you'll need to publish it manually for the process to start in the RabbitMQ Management Tool: [localhost:15672](localhost:15672) user - `guest` password - `guest`. Click the queue and click publish.
            * `InvoiceScheduledEvent`
            * `InvoicePayRetryApprovedEvent`
            * `InvoicePayRetryExceededEvent`
        * Produces
            * `InvoiceScheduledEvent`
            * `InvoicePayCommitSucceedEvent`
            * `InvoicePayCommitFailedEvent`

- **pleo-antaeus-retrier** - [http://localhost:7000](http://localhost:7000)
    * HTTP
        * /rest/health
        * /rest/v1/payments/:invoiceId - every payment attempt for that given invoice ID
    
    * Bus
        * Consumes
            * `InvoicePayCommitFailedEvent`
            * `InvoicePayCommitSucceedEvent`
            * `InvoicePayRetryExceededEvent`
        * Produces
            * `InvoicePayRetryApprovedEvent`
            * `InvoicePayRetryExceededEvent`
    
### Improvements

- Keep working on the on the abstraction in the `pleo-bus` module to get rid of one library I used to publish and consume.
- Handle some exceptions to requeue automatically 
- Use a cron job to trigger the monthly process 
- Add dates to the invoices in order to schedule only those ones of the month
- Enhance the notification service
- Have retry rules per market or currency instead of the fixed one 

### Conclusion

Regardless of the decision, this is a project I enjoyed to do. In spite of my lack of knowledge in Kotlin I enjoyed to get through the small challenges, I found in the way. I got a lot of hands-on experience with this language as well. The things I like the most is the functional approach that comes with it. Also, the mocking library is so straight forward and intuitive, I'd wish there was a mocking library like this In Go :p. I still need some time to go into the details of this language, but so far, so good.

I spend Tuesday and Wednesday in the afternoons learning Kotlin. I start working on the challenge on Thursday in the afternoon, and I continued on Friday afternoon for a couple of hours. However, I didn't progress much I spent most of the time fighting with Gradle: s, you know I was Friday 13th. Saturday was the day I spent most of the time, and thus when I get most of the things done; on Sunday, I work intermittently during the day to wrap up everything up but mostly in the afternoon.

## Developing

Requirements:
- Docker compose - I am using the 1.24 version but anything recent should do.

### Running
You'll need docker-compose

For tests:
        
    ./script/test

To run the application:

    ./script/server

And once you are done:

    ./script/down

Useful links:

RabbitMQ Management Tool: [localhost:15672](localhost:15672) user - `guest` password - `guest`. for this one the queues are created at start up time, so both services gotta be up and running for the queues to show up.

`pleo-antaeus-scheduler` - [http://localhost:7000/rest/health](http://localhost:7000/rest/health)

`pleo-antaeus-retrier` - [http://localhost:7001/rest/health](http://localhost:7001/rest/health)

### App Structure
The code given is structured as follows.
```
├── pleo-antaeus-retrier
|       notification - Domain Notifies the user in case of too many unsuccessful attempts 
|       retry - Domain Keeps track of every payment attemp and decides wether to retry or not
|
├── pleo-antaeus-scheduler
|       scheduler - Domain Schedules and commit payments
|
├── pleo-bus - RabbitMQ abstraction pub/sub + topology creator.
```

Each domain follow this convention
```
├── scheduler
    ├─── app - Is the business logic, use cases, what you call as core
    ├─── domain - Are the domain entities, what you call models. Here you can also see which events each service either pusblishes or consumes
    ├─── infra - Is the layer from which you fetch data, you name it data.
    ├─── delivery - I am used to calling it to interface, but that's not possible in Kotlin. This package would be the entry point or API to your domain. The http and event handlers would go here
├── notification
    ├─── app
└── ...
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
