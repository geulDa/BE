pipeline {
    agent any

    environment {

        /* ===== Docker in Docker ===== */
        DOCKER_HOST = 'tcp://dind:2376'
        DOCKER_TLS_VERIFY = '1'
        DOCKER_CERT_PATH = '/certs/client'

        /* ===== 이미지 이름 ===== */
        IMAGE_NAME = "kyumin19/geulda-be"

        /* ===== AWX 설정 ===== */
        AWX_URL = "http://34.64.206.170:30080"
        AWX_BLUE_TEMPLATE = "10"      // Deploy-Blue 템플릿 ID
        AWX_GREEN_TEMPLATE = "11"     // Deploy-Green 템플릿 ID
        AWX_TOKEN = credentials('awx-token')

        /* ===== AWS 정보 ===== */
        LISTENER_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:listener/app/geulda-alb/c37d33ae4e691f29/80942d2924901550"
        BLUE_TG_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:targetgroup/geulda-app/b83b1b3a348286f9"
        GREEN_TG_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:targetgroup/geulda-green/5c5a4cf4abad3480"


        /* ===== Discord ===== */
        DISCORD_WEBHOOK = credentials('discord-webhook')
    }

    triggers {
        githubPush()
    }

    stages {

        /* ---------------------------------------- */
        stage('Git Checkout') {
            steps {
                echo "🔄 Git Checkout (${env.BRANCH_NAME})"
                git branch: "${env.BRANCH_NAME}",
                    credentialsId: 'github_Token',
                    url: 'https://github.com/geulDa/BE.git'
            }
        }

        /* ---------------------------------------- */
        stage('Gradle Build') {
            steps {
                echo "⚙️ Gradle Build 시작"
                sh './gradlew clean build -x test'
            }
        }

        /* ---------------------------------------- */
        stage('Docker Build & Push') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred',
                usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {

                    sh '''
                        docker build -t $IMAGE_NAME:latest .
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $IMAGE_NAME:latest
                    '''
                }
            }
        }

        /* ---------------------------------------- */
        stage('Determine Blue/Green Target') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                script {
                    echo "🎯 현재 ALB Listener 상태 확인"

                    def tgArn = sh(
                        script: """
                            aws elbv2 describe-listeners \
                                --listener-arn ${LISTENER_ARN} \
                                --query 'Listeners[0].DefaultActions[0].TargetGroupArn' \
                                --output text
                        """,
                        returnStdout: true
                    ).trim()

                    if (tgArn == GREEN_TG_ARN) {
                        echo "현재 GREEN 활성 → BLUE에 배포"
                        env.DEPLOY_TARGET = "blue"
                        env.NEXT_TG_ARN = BLUE_TG_ARN
                        env.AWX_TEMPLATE = AWX_BLUE_TEMPLATE
                    } else {
                        echo "현재 BLUE 활성 → GREEN에 배포"
                        env.DEPLOY_TARGET = "green"
                        env.NEXT_TG_ARN = GREEN_TG_ARN
                        env.AWX_TEMPLATE = AWX_GREEN_TEMPLATE
                    }

                    echo "📌 다음 배포 대상: ${env.DEPLOY_TARGET}"
                    echo "📌 실행할 AWX 템플릿: ${env.AWX_TEMPLATE}"
                }
            }
        }

        /* ---------------------------------------- */
        stage('Trigger AWX Deployment') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                echo "🚀 AWX 템플릿(${env.AWX_TEMPLATE}) 실행 요청"
                sh """
                    curl -X POST "$AWX_URL/api/v2/job_templates/${env.AWX_TEMPLATE}/launch/" \
                    -H "Authorization: Bearer $AWX_TOKEN" \
                    -H "Content-Type: application/json"
                """
            }
        }

        /* ---------------------------------------- */
        stage('Switch ALB TargetGroup') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                echo "🔀 ALB를 ${env.DEPLOY_TARGET} 방향으로 전환"

                withAWS(region: 'ap-northeast-2', credentials: 'aws-access-key') {
                    sh """
                        aws elbv2 modify-listener \
                            --listener-arn ${LISTENER_ARN} \
                            --default-actions Type=forward,TargetGroupArn=${env.NEXT_TG_ARN}
                    """
                }
            }
        }

        /* ---------------------------------------- */
        stage('Discord Notification') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                sh """
                    curl -H "Content-Type: application/json" \
                        -d "{\\"content\\": \\":white_check_mark: GEULDA 배포 성공! → ${env.DEPLOY_TARGET.toUpperCase()} 활성화\\"}" \
                        "$DISCORD_WEBHOOK"
                """
            }
        }
    }

    /* ---------------------------------------- */
    post {
        failure {
            echo "❌ 파이프라인 실패 — 롤백 수행"

            sh """
                curl -H "Content-Type: application/json" \
                -d '{ "content": ":x: **배포 실패 — 롤백 진행됨**" }' \
                "$DISCORD_WEBHOOK"
            """
        }
    }
}
