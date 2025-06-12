pipeline {
  agent {
    kubernetes {
      yaml '''
        apiVersion: v1
        kind: Pod
        metadata:
          labels:
            jenkins-slave-es: jenkins-slave-es
        spec:
          tolerations:
          - key: "jenkinsSlave"
            operator: "Equal"
            value: "true"
            effect: "NoExecute"
          containers:
          - name: jnlp
            volumeMounts:
              - mountPath: "/home/jenkins/ansible-vault-password"
                name: ansible-secrets
                readOnly: true
              - mountPath: "/home/jenkins/service-account-keys"
                name: account-secrets
                readOnly: true
              - mountPath: "/home/jenkins/npmrc"
                name: npmrc
                readOnly: true
              - name: dockersock
                mountPath: "/var/run/docker.sock"
            image: europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/jenkins-inbound-agent:3.1.22
            tty: true
            env:
            - name: NPM_CONFIG_USERCONFIG
              value: /home/jenkins/npmrc/password
            - name: ANSIBLE_VAULT_PASSWORD_FILE
              value: "/home/jenkins/ansible-vault-password/password"
            - name: CLOUDPERMIT_GITHUB_USERNAME
              value: "lupapiste-ci"
            - name: CLOUDPERMIT_GITHUB_TOKEN
              valueFrom:
                secretKeyRef:
                  name: lupapiste-ci
                  key: text
            resources:
              limits:
                memory: 8Gi
              requests:
                cpu: "6"
                memory: 8Gi
          - name: mongo
            image: europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/mongo-test:6.0
            ports:
              - containerPort: 27017
            tty: true
            env:

            - name: MONGO_INITDB_DATABASE
              value: "toj"
          volumes:
            - name: ansible-secrets
              secret:
                secretName: ansible-vault-password
            - name: account-secrets
              secret:
                secretName: service-account-keys
            - name: dockersock
              hostPath:
                path: /var/run/docker.sock
            - name: npmrc
              secret:
                secretName: npmrc
          nodeSelector:
            cloud.google.com/gke-nodepool: jenkins-build-slave-pool
        '''
      }
  }
  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: "15"))
  }

  triggers { pollSCM( scmpoll_spec: BRANCH_NAME == "master" ? "30 22 * * *"  : "" , ignorePostCommitHooks: BRANCH_NAME == "master" ? 'true'  : "" )}

  stages{
    stage('Build') {
      environment {
                CI_BUILD = 1
                OPENSSL_CONF="/etc/ssl/"
      }
      steps {
        sh """
          npm ci --verbose
          lein clean
          lein cljsbuild once prod
          lein cljsbuild test
          npm run test-cljs:ci
          lein test2junit
          lein uberjar
        """
      }
    }
    stage('Image') {
      when {
        anyOf {
          expression { BRANCH_NAME == "develop" }
          expression { BRANCH_NAME == "release" }
          expression { BRANCH_NAME == "master" }
        }
      }
      steps {
        sh """
          gcloud auth activate-service-account --key-file=/home/jenkins/service-account-keys/dev.json
          gcloud auth configure-docker europe-north1-docker.pkg.dev
          docker build -t europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:latest -t europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:${BRANCH_NAME}-\${GIT_COMMIT} .
          docker push europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:${BRANCH_NAME}-\${GIT_COMMIT}
        """
      }
    }
    stage('Infra scm') {
      steps {
        script{
          sh 'mkdir -p gcp-infra'
          dir("gcp-infra"){
            git branch: "master",
            changelog: false,
            poll: false,
            credentialsId: 'github-key',
            url: 'git@github.com:cloudpermit/gcp-infra.git'
          }
        }
      }
    }
    stage('deploy from develop branch') {
      when {
        anyOf {
          expression { BRANCH_NAME == "develop" }
        }
      }
      parallel {
        stage ('Deploy dev'){
          steps {
              dir("gcp-infra/ansible"){
                script{
                  sh """
                    gcloud container clusters get-credentials lupis --zone europe-north1-b --project lupapiste-dev
                    docker push europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:latest
                    GOOGLE_APPLICATION_CREDENTIALS=/home/jenkins/service-account-keys/dev.json K8S_AUTH_KUBECONFIG=/home/jenkins/.kube/config ansible-playbook -i lupapiste/dev -e toj_tag=${BRANCH_NAME}-\${GIT_COMMIT} --tags=deploy toj-app.yml
                  """
                }
              }
          }
        }
        stage ('Deploy test'){
          steps {
              dir("gcp-infra/ansible"){
                script{
                  sh """
                    gcloud container clusters get-credentials lupis --zone europe-north1-b --project lupapiste-dev
                    GOOGLE_APPLICATION_CREDENTIALS=/home/jenkins/service-account-keys/dev.json K8S_AUTH_KUBECONFIG=/home/jenkins/.kube/config ansible-playbook -i lupapiste/test -e toj_tag=${BRANCH_NAME}-\${GIT_COMMIT} --tags=deploy toj-app.yml
                  """
                }
              }
          }
        }
      }
    }
    stage ('Deploy QA'){
      when {
        anyOf {
          expression { BRANCH_NAME == "release" }
        }
      }
      steps {
          dir("gcp-infra/ansible"){
            script{
              sh """
                gcloud container clusters get-credentials lupis --zone europe-north1-b --project lupapiste-dev
                docker tag europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:${BRANCH_NAME}-\${GIT_COMMIT} europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:latest-release
                docker push europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:latest-release
                GOOGLE_APPLICATION_CREDENTIALS=/home/jenkins/service-account-keys/dev.json K8S_AUTH_KUBECONFIG=/home/jenkins/.kube/config ansible-playbook -i lupapiste/qa -e toj_tag=${BRANCH_NAME}-\${GIT_COMMIT} --tags=deploy toj-app.yml
              """
            }
          }
      }
    }
    stage ('Deploy prod'){
      when {
        anyOf {
          expression { BRANCH_NAME == "master" }
        }
      }
      steps {
          dir("gcp-infra/ansible"){
            script{
              sh """
                docker tag europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:${BRANCH_NAME}-\${GIT_COMMIT} europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:latest-master
                docker push europe-north1-docker.pkg.dev/lupapiste-dev/cloudpermit-fi-docker-repo/toj:latest-master
                gcloud auth activate-service-account --key-file=/home/jenkins/service-account-keys/prod.json
                gcloud container clusters get-credentials lupis --region europe-north1 --project lupapiste-prod
                GOOGLE_APPLICATION_CREDENTIALS=/home/jenkins/service-account-keys/prod.json K8S_AUTH_KUBECONFIG=/home/jenkins/.kube/config ansible-playbook -i lupapiste/prod -e toj_tag=${BRANCH_NAME}-\${GIT_COMMIT} --tags=deploy toj-app.yml
              """
            }
          }
      }
    }
  }
  post {
    failure {
       script{
         if ( BRANCH_NAME == "develop" | BRANCH_NAME == "master" | BRANCH_NAME == "release")
           slackSend (color: '#FF0000', channel: "#dev-lupapiste", message: "FAILED: Job '${env.JOB_NAME} - ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
       }
    }
    fixed {
       script{
         if ( BRANCH_NAME == "develop" | BRANCH_NAME == "master" | BRANCH_NAME == "release")
           slackSend (color: '#00FF00', channel: "#dev-lupapiste", message: "Back to normal: Job '${env.JOB_NAME} - ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
       }
    }
    success {
      script{
         if ( BRANCH_NAME == "master")
           slackSend (color: '#00FF00', channel: "#dev-lupapiste", message: "Production deployment: Job '${env.JOB_NAME} - ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
      }
    }
  }
}
