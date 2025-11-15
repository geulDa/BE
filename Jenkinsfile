pipeline {
    agent any

    environment {

        /* ================================
         * Docker in Docker 환경 변수
         * ================================ */
        DOCKER_HOST = 'tcp://dind:2376'
        DOCKER_TLS_VERIFY = '1'
        DOCKER_CERT_PATH = '/certs/client'

        /* ================================
         * 이미지 / 자격증명
         * ================================ */
        IMAGE_NAME = "kyumin19/geulda-be"

        /* ================================
         * AWX 설정
         * ================================ */
        AWX_URL = "http://34.64.206.170:30080"
        AWX_JOB_TEMPLATE_ID = "1"
        AWX_TOKEN = credentials('awx-token')

        /* ================================
         * AWS ALB 설정
         * ================================ */
        LISTENER_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:listener/app/geulda-alb/c37d33ae4e691f29/80942d2924901550"
        GREEN_TG_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:targetgroup/geulda-green/5c5a4cf4abad3480"

        /* ================================
         * Discord Webhook
         * ================================ */
        DISCORD_WEBHOOK = credentials('discord-webhook')
    }

    triggers {
        githubPush()
    }

    stages {

        /* -----------------------------
         * 1) Git Checkout
         * ----------------------------- */
        stage('📥 소스 코드 가져오기') {
            steps {
                echo "🔄 GitHub에서 브랜치(${env.BRANCH_NAME}) 소스 코드를 가져옵니다..."
                git branch: "${env.BRANCH_NAME}",
                    credentialsId: 'github_Token',
                    url: 'https://github.com/geulDa/BE.git'
            }
        }

        /* -----------------------------
         * 2) Gradle Build
         * ----------------------------- */
        stage('⚙️ Gradle 빌드') {
            steps {
                echo "🛠 프로젝트 빌드를 시작합니다..."
                sh './gradlew clean build -x test'
                echo "✅ Gradle 빌드 완료"
            }
        }

        /* -----------------------------
         * 3) Docker Build & Push
         * ----------------------------- */
        stage('🐳 Docker 이미지 빌드 및 푸시') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred',
                usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {

                    echo "🐳 Docker 이미지 빌드를 시작합니다..."
                    sh '''
                        docker build -t $IMAGE_NAME:latest .
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $IMAGE_NAME:latest
                    '''
                    echo "✅ Docker 빌드 및 푸시 완료"
                }
            }
        }

        /* -----------------------------
         * 4) AWX CD Trigger
         * ----------------------------- */
        stage('🚀 AWX CD 실행 요청') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                echo "📡 AWX에 배포 작업을 요청합니다..."
                sh """
                    curl -X POST "$AWX_URL/api/v2/job_templates/$AWX_JOB_TEMPLATE_ID/launch/" \
                    -H "Authorization: Bearer $AWX_TOKEN" \
                    -H "Content-Type: application/json"
                """
                echo "✅ AWX 배포 요청 완료"
            }
        }

        /* -----------------------------
         * 5) ALB TargetGroup GREEN 전환 (Blue → Green)
         * ----------------------------- */
        stage('🔀 ALB 트래픽 GREEN으로 전환') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                echo "🔀 ALB Listener를 GREEN TargetGroup으로 변경합니다..."

                withAWS(region: 'ap-northeast-2', credentials: 'aws-access-key') {
                    sh """
                        aws elbv2 modify-listener \
                        --listener-arn $LISTENER_ARN \
                        --default-actions Type=forward,TargetGroupArn=$GREEN_TG_ARN
                    """
                }

                echo "✅ ALB가 GREEN 서버로 트래픽을 전환했습니다"
            }
        }

        /* -----------------------------
         * 6) Discord 알림
         * ----------------------------- */
        stage('📢 Discord 알림 발송') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                echo "📢 Discord로 배포 성공 메시지를 전송합니다..."
                sh """
                    curl -H "Content-Type: application/json" \
                    -d '{"content": ":white_check_mark: **GEULDA 배포 성공! (GREEN 활성화 완료)**"}' \
                    "$DISCORD_WEBHOOK"
                """
                echo "✅ Discord 알림 전송 완료"
            }
        }
    }

    post {
        success {
            echo "🎉 전체 파이프라인 성공!"
        }
        failure {
            echo "❌ 파이프라인 실패 — 로그를 확인해주세요."
        }
    }
}
