plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    compile("com.rabbitmq:amqp-client:5.7.3")
    compile("com.viartemev:the-white-rabbit:0.0.6")
}