def call(Map options = [:]) {
    def label = "slave-${UUID.randomUUID().toString()}"

podTemplate(label: label, cloud: 'paas', yaml: """
apiVersion: v1
kind: Pod
metadata:
labels:
    container: slave
spec:
  containers:
  - name: brew
    image: quay.io/homebrew/brew
    command:
    - cat
    tty: true
  - name: docker
    image: docker/compose:1.23.2
    command:
    - cat
    tty: true
    volumeMounts:
    - name: docker
      mountPath: /var/run/docker.sock
  volumes:
  - name: docker
    hostPath:
      path: /var/run/docker.sock
      type: Socket
"""){

        node(label) {

            container('brew') {
                stage("Checkout Repo") {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: 'master']],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [
                            [$class: 'WipeWorkspace'],
                            [$class: 'RelativeTargetDirectory', relativeTargetDir: '${options.gitPath}']],
                        submoduleCfg: [],
                        userRemoteConfigs: [[url: "https://github.com/Maistra/${options.gitRepo}.git"]]
                    ])
                }
                stage("Git Branch") {
                    echo "?????"
                }
                stage("Create RPM") {
                    echo ${options.rpmName}
                    sh """
                        # Must understand how this function works!
                        # buildRPM getRhpkgRPMRepo "${options.rpmName}" "${options.branch}" "${options.privateBranch}" "${options.gitRepo}/${options.gitPath}" "-i $(getHash ${options.gitRepo}/${options.gitPath})"
                        brew --help
                    """
                }                  
            }

            container('docker') {
                stage("Create Docker Image"){
                    sh("docker build -t ${options.imageName}:${options.appVersion} .")
                }
                stage("Push Docker Image"){
                    def parameters = [[
                        $class: 'UsernamePasswordMultiBinding', 
                        credentialsId: 'quayio',
                        usernameVariable: 'DOCKER_USER', 
                        passwordVariable: 'DOCKER_PASSWORD'
                    ]]
                    withCredentials(parameters) {
                        sh """
                            docker login -u ${env.DOCKER_USER} -p ${env.DOCKER_PASSWORD} https://quay.io
                            docker tag ${options.imageName}:${options.appVersion} ${options.imageName}:latest
                            docker push ${options.imageName}:${options.appVersion}
                            docker push ${options.imageName}:latest
                        """
                    }
                }
            }
        }
    }
}