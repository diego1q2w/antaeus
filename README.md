## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Solution
I am new in Kotlin, so I spend a couple of days during my free time to learn it!

Designing: When we think about payments, the first thing that pops up in your mind is what you do if payment failed? Well, you retry, and after some attempts, you might want to notify.
When we look at this problem from the DDD perspective, scheduling, retrying, and notifying are three different tasks (at least for me). And thus, separating them feels natural, regardless of your infrastructure specifics (monolithic, microservice or lambda).

That's the main reason why I change the project structure, having each layer of each domain as a module can be confusing, and by that, you'd end up with something like this.
```
├── pleo-antaeus-schedule-app
├── pleo-antaeus-schedule-core
├── pleo-antaeus-schedule-data
├── pleo-antaeus-retry-app
├── pleo-antaeus-retry-core
├── pleo-antaeus-notification-core
├── pleo-antaeus-notification-app
└── ...
```
Which is not bad at all but at least for me can be confusing, so instead of that why not having something like this:
```
├── pleo-antaeus-schedule
    ├─── app - Is the business logic, use cases, what you call as core
    ├─── domain - Are the domain entities, what you call models
    ├─── infra - Is the layer from which you fetch data, you name it data
    ├─── delivery - I am used to calling it to interface, but that's not possible in Kotlin. This package would be the entry point to your domain the rest API, and event handlers would go here
├── pleo-antaeus-retry
    ├─── app
    ├─── domain
    ├─── infra
    ├─── delivery
└── ...
```
Now, you see where I am getting at. 

The idea is to have two micro-services one for schedule and another for retries. The notification domain will live within the retry microservice. Both micro-services will communicate via events (I'll use RabbitMQ). I have to say that the notification system will be rather simple probably just a logging. The point is to show the possibility to work and maintain, even in this hybrid infrastructures with ease.

TODO: Update the App Structure
### Iterations
`Iteration 1` - I did the app refactor, no logic changed. I just made sure everything still works as it was. This is perhaps, the largest commit this project will have.

`Iteration 2` - Added the payment scheduler, which marks the invoices as "Ready to process" (every month), is worth noticing that this step does not execute the payment only mark it as `scheduled`.

`Iteration 3` - The payments processor was added, It will fetch all the payments marked as `scheduled`, then mark them as `processing` and finally publish a message to the bus (Mocked for now). This event will in future iterations be consumed by the scheduler micro-service to finally commit the payment. Why I am doing this? I will explain it in the next iterations.

`Iteration 4` - Created the docker-compose which includes the RabbitMQ Image and topology + new set up scripts. The topology that `setup-events-topology` image creates, emulates the topic-service of Kafka where separate consumer groups are completely independent, so they all see all messages. To understand what I mean go to `localhost:15672` user and pasword: `guest` and then go to queues.

`Iteration 5` - Removed the mocked bus publisher and added the good one `pleo-bus`, for now, just publishing the topic `InvoiceScheduledEvent`.

`Iteration 6` - Add the commit payment logic, most of the exceptions `CustomerNotFoundException`, `CurrencyMismatchException`, etc. Produce a log, that could be used as a notification, however, the `NetworkException` that also log something, should as well send the message to the DLX for future retrial, unfortunately the library I am using does not support that. So, that would be a future improvement. If there are no data nor infra issues an event is created for both cases `InvoicePayCommitSucceedEvent = yeeey` and `InvoicePayCommitFailedEvent` with a reason 

## Developing

Requirements:
- \>= Java 11 environment

RabbitMQ Management Tool:
 `localhost:15672` user - `guest` password - `guest`.

### Running
You'll need docker-compose

For tests:
        
    ./script/test

To run the application:

    ./script/server

And once you are done:

    ./script/down


### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the "rest api" models used throughout the application.
|
├── pleo-antaeus-rest
|        Entry point for REST API. This is where the routes are defined.
└──
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
