def call(Map options = [:]) {
    def label = "slave-${UUID.randomUUID().toString()}"

    properties([
        parameters([
            choice(
                name: 'image',
                choices: "other\nnode\njava",
                description: 'POD Template that will be used'
            )
        ])
    ])

    if (params.image == 'node') {
        echo "Using POD with Node Template"

podTemplate(label: label, yaml: """
apiVersion: v1
kind: Pod
metadata:
labels:
    container: slave
spec:
  containers:
  - name: npm
    image: node:carbon
    command:
    - cat
    tty: true
""")    {
            node(label) {
                // PARAMETERS SESSION
                stage('podTemplate'){
                    echo "${image} choosed"
                }
            } 
        }
    }
    else if (params.image == 'java') {
        echo "Using POD with Java Template"
        
podTemplate(label: label, yaml: """
apiVersion: v1
kind: Pod
metadata:
labels:
    container: slave
spec:
  containers:
  - name: java
    image: openjdk:8u181-jdk-stretch
    command:
    - cat
    tty: true

  - name: maven
    image: maven:3.6.0-jdk-8-alpine
    command:
    - cat
    tty: true
""")    {
            node(label) {
                // PARAMETERS SESSION
                stage('podTemplate'){
                    echo "${image} choosed"
                }
            } 
        }
    }
    else  {
        echo "Nothing to do!!!"

podTemplate(label: label, yaml: """
apiVersion: v1
kind: Pod
metadata:
labels:
    container: slave
spec:
  containers:
  - name: dummy
    image: alpine
    command:
    - cat
    tty: true
""")    {
            node(label) {
                // PARAMETERS SESSION
                stage('podTemplate'){
                    echo "${image} choosed"
                }
            } 
        }
    } 
}
