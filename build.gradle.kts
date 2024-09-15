plugins {
	id("com.hayden.dgs-graphql")
	id("com.hayden.spring-app")
	id("com.hayden.observable-app")
	id("com.hayden.discovery-app")
	id("com.hayden.messaging")
	id("com.hayden.docker-compose")
	id("com.hayden.jdbc-persistence")
}

description = "gateway"

tasks.register("prepareKotlinBuildScriptModel")

dependencies {
	implementation("com.squareup:javapoet:1.13.0")
	api(project(":utilitymodule"))
	api(project(":graphql"))
}

sourceSets { main { resources { exclude("schema-in/*.graphql") } }  }


tasks.compileJava {
	dependsOn("copyAgent")
	// java -javaagent:build/agent/opentelemetry-javaagent.jar -jar build/libs/gateway-1.0.0.jar
}

tasks.generateJava {
	schemaPaths.add("${projectDir}/src/main/resources/graphql-client")
	packageName = "com.hayden.gateway.codegen"
	generateClient = true
}

