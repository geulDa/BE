pipeline {
    agent any

    environment {
        DOCKER_HOST = 'tcp://dind:2376'
        DOCKER_TLS_VERIFY = '1'
        DOCKER_CERT_PATH = '/certs/client'
        IMAGE_NAME = "kyumin19/geulda-be"
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Building branch: ${env.GIT_BRANCH}"
                }
                git branch: '*/${env.BRANCH_NAME}', credentialsId: 'github-token', url: 'https://github.com/geulDa/BE.git'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build -x test'
            }
        }

        stage('Docker Build & Push') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        docker build -t $IMAGE_NAME:latest .
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $IMAGE_NAME:latest
                    '''
                }
            }
        }

        stage('Staging Build (dev only)') {
            when {
                expression { env.GIT_BRANCH == 'origin/dev' || env.BRANCH_NAME == 'dev' }
            }
            steps {
                echo '🧪 Building dev branch for staging environment...'
                sh 'docker build -t $IMAGE_NAME:staging .'
            }
        }
    }

    post {
        success {
            echo "✅ Build completed for ${env.GIT_BRANCH}"
        }
        failure {
            echo "❌ Build failed for ${env.GIT_BRANCH}"
        }
    }
}
