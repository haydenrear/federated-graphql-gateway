plugins {
	id("com.netflix.dgs.codegen") version "6.0.3"
	id("com.hayden.spring-app")
	id("com.hayden.observable-app")
}

description = "gateway"

//configurations {
//	compileOnly {
//		extendsFrom(configurations.annotationProcessor.get())
//	}
//}

tasks.register("prepareKotlinBuildScriptModel")


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.liquibase:liquibase-core")
	implementation("org.springframework.cloud:spring-cloud-starter-zookeeper-discovery")
	implementation("org.springframework.kafka:spring-kafka")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.graphql:spring-graphql-test")
	testImplementation("com.h2database:h2")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter")
	implementation("com.netflix.graphql.dgs.codegen:graphql-dgs-codegen-core:6.1.4")
	implementation("com.netflix.graphql.dgs:graphql-dgs-mocking:8.2.5")
	implementation("com.apollographql.federation:federation-graphql-java-support:2.1.0")
	implementation("com.squareup:javapoet:1.13.0")
	implementation("org.springframework.boot:spring-boot-starter-integration")
	implementation("org.springframework.integration:spring-integration-graphql")
	implementation("org.springframework.integration:spring-integration-core")
	implementation("org.springframework.integration:spring-integration-http")
	api(project(":utilitymodule"))
	api(project(":graphql"))
}

tasks.compileJava {
	dependsOn("copyAgent")
	// java -javaagent:build/agent/opentelemetry-javaagent.jar -jar build/libs/gateway-1.0.0.jar
}

tasks.generateJava {
	schemaPaths.add("${projectDir}/src/main/resources/graphql-client")
	packageName = "com.hayden.gateway.codegen"
	generateClient = true
}

