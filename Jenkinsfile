#!groovy

node('maven') {
	def mvnCmd = "mvn"

	def appName = "tourreserve"
	def devPrj = "dev"

	stage('Cleanup env Dev') {
		// Delete all objects except for is.
		openshift.withCluster() {
			openshift.withProject(devPrj) {
			    openshift.selector("bc", [ app: appName ]).delete()
			    openshift.selector("dc", [ app: appName ]).delete()
			    openshift.selector("svc", [ app: appName ]).delete()
			    openshift.selector("pod", [ app: appName ]).delete()
			    openshift.selector("route", [ app: appName ]).delete()
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

	stage('Unit Tests') {
		echo "Unit Tests"
		//sh "${mvnCmd} test"
	}

	def newTag = "dev-${version}"

	stage('Build Image') {
		echo "New Tag: ${newTag}"

		// Copy the war file and the configurations to deployments directory.
		sh "cp ./terasoluna-tourreservation-web/target/terasoluna-tourreservation-web.war ./ROOT.war"

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
				openshift.selector("bc", appName).startBuild("--from-file=./ROOT.war").logs("-f")
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
				// Deploy created image.
			    def created = openshift.newApp("--name=$appName", "$devPrj/$appName:$newTag")
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
