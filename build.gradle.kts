plugins {
	id("com.hayden.graphql")
	id("com.hayden.spring-app")
	id("com.hayden.observable-app")
	id("com.hayden.discovery-app")
	id("com.hayden.messaging")
}

description = "gateway"

tasks.register("prepareKotlinBuildScriptModel")


dependencies {
	implementation("com.squareup:javapoet:1.13.0")
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

