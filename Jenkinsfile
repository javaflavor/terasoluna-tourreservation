#!groovy

node('maven') {
	def mvnCmd = "mvn"

	def appName = "${env.APPNAME ?: 'tourreserve'}"
	def devPrj = "${env.DEV_PROJECT ?: 'dev'}"
	def devopsPrj = "${env.DEVOPS_PROJECT ?: 'devops'}"

	stage('Cleanup env Dev') {
		// Delete all objects except for is.
		openshift.withCluster() {
			openshift.withProject(devPrj) {
				// Delete webapp.
			    openshift.selector("bc", [ app: appName ]).delete()
			    openshift.selector("dc", [ app: appName ]).delete()
			    openshift.selector("svc", [ app: appName ]).delete()
			    openshift.selector("pod", [ app: appName ]).delete()
			    openshift.selector("route", [ app: appName ]).delete()
				// Delete postgresql.
				if (openshift.selector("dc/$appName-postgresql").exists()) {
			    	openshift.selector("dc/$appName-postgresql").delete()				       
				}
				if (openshift.selector("svc/$appName-postgresql").exists()) {
			    	openshift.selector("svc/$appName-postgresql").delete()
			    }
				if (openshift.selector("secret/$appName-postgresql").exists()) {
			    	openshift.selector("secret/$appName-postgresql").delete()
			    }
			}
		}
	}

	stage('Checkout Source') {
		checkout scm
	}

	def groupId    = getGroupIdFromPom("pom.xml")
	def artifactId = getArtifactIdFromPom("pom.xml")
	def version    = getVersionFromPom("pom.xml")

	stage('Build WAR') {
		if(fileExists("./settings.xml")) {
			sh "cp ./settings.xml ~/.m2/"
		}
		if(fileExists("./settings-security.xml")) {
			sh "cp ./settings-security.xml ~/.m2/"
		}

		echo "Building version ${version}"
		sh "${mvnCmd} clean package -DskipTests"
	}
	
	stage('Prepare Postgresql in Dev') {
		// Do deploy the target.
		openshift.withCluster() {
			openshift.withProject(devPrj) {
				// Deploy postgresql server.
			    def created = openshift.newApp("postgresql-ephemeral",
			    	"-p", "DATABASE_SERVICE_NAME=$appName-postgresql",
			    	"-p", "POSTGRESQL_DATABASE=$appName",
			    	"-p", "POSTGRESQL_USER=test",
			    	"-p", "POSTGRESQL_PASSWORD=test")
				echo "${created.actions[0].cmd}"
				echo "${created.actions[0].out}"

				// Wait and print status deployment.
				def dc = created.narrow("dc")
				dc.rollout().status("-w")
			}
		}
	}
	
	stage('Unit Tests') {
		echo "Unit Tests"
		sh "${mvnCmd} test -Dspring.profiles.active=test -Ddatabase.host=${appName}-postgresql.${devPrj}"
	}

 	stage('Code Analysis') {
		openshift.withCluster() {
			openshift.withProject(devopsPrj) {
				if (openshift.selector("svc/sonarqube").exists()) {
					echo "Code Analysis"
					sh "${mvnCmd} sonar:sonar -Dsonar.host.url=http://sonarqube:9000/ -Dsonar.projectName=${appName}"
				} else {
					echo "Warning: No SonarQube deployed. Skip Code Analysis."      
				}
			}
		}
	}
	
	def newTag = "dev-${version}"

	stage('Build Image') {
		echo "New Tag: ${newTag}"

		// Copy the war file and other artifaces to deployments directory.
		sh "mkdir -p deployments"
		sh "cp ./terasoluna-tourreservation-web/target/terasoluna-tourreservation-web.war ./deployments/ROOT.war"
		sh "cp -a ./.s2i ./deployments/"

		// Start Binary Build in OpenShift using the file we just published
		openshift.withCluster() {
			openshift.withProject(devPrj) {
				if (!openshift.selector("is", appName).exists()) {
					// Create imageStream from file "openshift/sampleweb-is.yaml".
					openshift.create(readFile("openshift/$appName-is.yaml"))
				}
				// Create buildConfig from file "openshift/sampleweb-bc.yaml".
			    openshift.create(readFile("openshift/$appName-bc.yaml"))
			    // Start image build.
				openshift.selector("bc", appName).startBuild("--from-dir=./deployments").logs("-f")
				// Tag created image.
				def result = openshift.tag("$appName:latest", "$appName:$newTag")
				echo "${result.actions[0].cmd}"
				echo "${result.actions[0].out}"
			}
		}
	}

	stage('Deploy to Dev') {
		// Do deploy the target.
		openshift.withCluster() {
			openshift.withProject(devPrj) {
				// Deploy created webapp image.
			    def created = openshift.newApp("--name=$appName", "--as-deployment-config", "$devPrj/$appName:$newTag")
				echo "${created.actions[0].cmd}"
				echo "${created.actions[0].out}"

				// Expose service.
				created.narrow("svc").expose()

				// Wait and print status deployment.
				def dc = created.narrow("dc")
				dc.rollout().status("-w")
			}
		}
	}

	stage('Integration Tests') {
		echo "Integration Tests"
		sh "${mvnCmd} verify -f integration-test/pom.xml -Dtarget.host=${appName}.${devPrj}"
		
		// Add Staging ready tag to the IT-passed image.
		newTag = "stg-${version}"
		echo "New Tag: ${newTag}"
		
		openshift.withCluster() {
			openshift.withProject(devPrj) {
				// Tag IT-passed image.
				def result = openshift.tag("$appName:latest", "$appName:$newTag")
				echo "${result.actions[0].cmd}"
				echo "${result.actions[0].out}"
			}
		}
	}
}

// Convenience Functions to read variables from the pom.xml
def getVersionFromPom(pom) {
	def matcher = readFile(pom) =~ '<version>(.+)</version>'
	matcher ? matcher[0][1] : null
}
def getGroupIdFromPom(pom) {
	def matcher = readFile(pom) =~ '<groupId>(.+)</groupId>'
	matcher ? matcher[0][1] : null
}
def getArtifactIdFromPom(pom) {
	def matcher = readFile(pom) =~ '<artifactId>(.+)</artifactId>'
	matcher ? matcher[0][1] : null
}
