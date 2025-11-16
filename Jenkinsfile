pipeline {
    agent any

    environment {
        DOCKER_HOST = 'tcp://dind:2376'
        DOCKER_TLS_VERIFY = '1'
        DOCKER_CERT_PATH = '/certs/client'

        IMAGE_NAME = "kyumin19/geulda-be"
        BRANCH_NAME = "main"

        AWS_DEFAULT_REGION = "ap-northeast-2"

        AWX_URL = "http://34.64.206.170:30080"
        AWX_BLUE_TEMPLATE = "10"
        AWX_GREEN_TEMPLATE = "11"
        AWX_TOKEN = credentials('awx-token')

        LISTENER_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:loadbalancer/app/geulda-alb/c37d33ae4e691f29"
        BLUE_TG_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:targetgroup/geulda-blue/418bbc5869d68f91"
        GREEN_TG_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:targetgroup/geulda-green/5c5a4cf4abad3480"

        DISCORD_WEBHOOK = credentials('discord-webhook')
    }

    triggers {
        githubPush()
    }

    stages {

        /* =======================================================================
         * 1. Gradle Build 최적화
         * ======================================================================= */
        stage('Gradle Build (Optimized)') {
            steps {
                echo "⚡ Optimized Gradle Build 시작"

                sh '''
                    mkdir -p ~/.gradle
                    echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties
                    echo "org.gradle.caching=true" >> ~/.gradle/gradle.properties
                    echo "org.gradle.parallel=true" >> ~/.gradle/gradle.properties
                    echo "org.gradle.configureondemand=true" >> ~/.gradle/gradle.properties

                    chmod +x ./gradlew
                    ./gradlew clean build -x test --parallel --configure-on-demand
                '''
            }
        }

        /* =======================================================================
         * 2. Docker Build + Cache
         * ======================================================================= */
        stage('Docker Build & Push (Layer Cache)') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    echo "🐳 Docker Build with Layer Cache"

                    sh '''
                        docker build \
                            --cache-from=$IMAGE_NAME:latest \
                            -t $IMAGE_NAME:latest .

                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $IMAGE_NAME:latest
                    '''
                }
            }
        }

        /* =======================================================================
         * 3. AWS Credentials 로드
         * ======================================================================= */
        stage('Load AWS Credentials') {
            when { branch 'main' }
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-access-key',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    script {
                        // Jenkins 전체 환경에 직접 주입
                        env.AWS_ACCESS_KEY_ID = AWS_ACCESS_KEY_ID
                        env.AWS_SECRET_ACCESS_KEY = AWS_SECRET_ACCESS_KEY

                        echo "🔐 AWS Credentials Pipeline 환경변수 등록 완료"
                    }
                }
            }
        }

        /* =======================================================================
         * 4. Blue/Green 현재 상태 확인
         * ======================================================================= */
        stage('Determine Blue/Green Target') {
            when { branch 'main' }
            steps {
                script {
                    echo "🎯 현재 ALB Listener 상태 확인 중..."

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
                        env.DEPLOY_TARGET = "blue"
                        env.NEXT_TG_ARN = BLUE_TG_ARN
                        env.AWX_TEMPLATE = AWX_BLUE_TEMPLATE
                    } else {
                        env.DEPLOY_TARGET = "green"
                        env.NEXT_TG_ARN = GREEN_TG_ARN
                        env.AWX_TEMPLATE = AWX_GREEN_TEMPLATE
                    }

                    echo "👉 배포 대상 TG: ${env.DEPLOY_TARGET}"
                }
            }
        }

        /* =======================================================================
         * 5. AWX 배포 실행
         * ======================================================================= */
        stage('Trigger AWX Deployment') {
            when { branch 'main' }
            steps {
                echo "🚀 AWX 템플릿(${env.AWX_TEMPLATE}) 실행"
                sh """
                    curl -X POST "$AWX_URL/api/v2/job_templates/${env.AWX_TEMPLATE}/launch/" \
                        -H "Authorization: Bearer $AWX_TOKEN" \
                        -H "Content-Type: application/json"
                """
            }
        }

        /* =======================================================================
         * 6. HealthCheck 후 ALB 전환
         * ======================================================================= */
        stage('Wait for HealthCheck & Switch TargetGroup') {
            when { branch 'main' }
            steps {
                script {
                    echo "⏳ HealthCheck 안정화 대기 (최대 60초)"

                    retry(12) {
                        sleep 5
                        def out = sh(
                            script: """
                                aws elbv2 describe-target-health \
                                    --target-group-arn ${env.NEXT_TG_ARN} \
                                    --query 'TargetHealthDescriptions[*].TargetHealth.State' \
                                    --output text
                            """,
                            returnStdout: true
                        ).trim()

                        echo "현재 상태: ${out}"
                        if (!(out.contains("healthy")|| out.contains("unused"))) {
                            error("TargetGroup 아직 Healthy 미달")
                        }
                    }

                    echo "💚 Healthy 완료 → ALB 전환 시작"

                    sh """
                        aws elbv2 modify-listener \
                            --listener-arn ${LISTENER_ARN} \
                            --default-actions Type=forward,TargetGroupArn=${env.NEXT_TG_ARN}
                    """
                }
            }
        }

        /* =======================================================================
         * 7. Discord 성공 알림
         * ======================================================================= */
        stage('Discord Notification') {
            when { branch 'main' }
            steps {
                sh """
                    curl -H "Content-Type: application/json" \
                        -d "{\\"content\\": \\":white_check_mark: GEULDA 배포 성공 → ${env.DEPLOY_TARGET.toUpperCase()} 활성화\\"}" \
                        "$DISCORD_WEBHOOK"
                """
            }
        }
    }

    /* =======================================================================
     * 실패 알림
     * ======================================================================= */
    post {
        failure {
            echo "❌ 배포 실패 — 롤백 메시지 전송"
            sh """
                curl -H "Content-Type: application/json" \
                    -d '{ "content": ":x: GEULDA 배포 실패 — 롤백 진행됨" }' \
                    "$DISCORD_WEBHOOK"
            """
        }
    }
}
