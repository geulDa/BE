pipeline {
    agent any

    environment {
        DOCKER_HOST = 'tcp://dind:2376'
        DOCKER_TLS_VERIFY = '1'
        DOCKER_CERT_PATH = '/certs/client'

        IMAGE_NAME = "kyumin19/geulda-be"
        BRANCH_NAME = "main"

        AWX_URL = "http://34.64.206.170:30080"
        AWX_BLUE_TEMPLATE = "10"
        AWX_GREEN_TEMPLATE = "11"
        AWX_TOKEN = credentials('awx-token')

        LISTENER_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:listener/app/geulda-alb/c37d33ae4e691f29/80942d2924901550"
        BLUE_TG_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:targetgroup/geulda-app/b83b1b3a348286f9"
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
                    echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties
                    echo "org.gradle.caching=true" >> ~/.gradle/gradle.properties
                    echo "org.gradle.parallel=true" >> ~/.gradle/gradle.properties
                    echo "org.gradle.configureondemand=true" >> ~/.gradle/gradle.properties

                    chmod +x ./gradlew

                    ./gradlew clean build -x test \
                        --no-daemon \
                        --parallel \
                        --configure-on-demand
                '''
            }
        }

        /* =======================================================================
         * 2. Docker Build + Cache 최적화
         * ======================================================================= */
        stage('Docker Build & Push (Layer Cache)') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred',
                    usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {

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
         * 3. 다음 배포 타겟 결정
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
         * 4. AWX 배포 실행
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
         * 5. ALB 무중단 전환 (HealthCheck 기반)
         * ======================================================================= */
        stage('Wait for HealthCheck & Switch TargetGroup') {
            when { branch 'main' }
            steps {
                script {

                    echo "⏳ HealthCheck 안정화 대기 (최대 60초)"

                    retry(12) {
                        sleep 5
                        def count = sh(
                            script: """
                                aws elbv2 describe-target-health \
                                  --target-group-arn ${env.NEXT_TG_ARN} \
                                  --query 'TargetHealthDescriptions[*].TargetHealth.State' \
                                  --output text
                            """,
                            returnStdout: true
                        ).trim()

                        echo "현재 상태: ${count}"

                        if (!count.contains("healthy")) {
                            error("TargetGroup 아직 Healthy 미달")
                        }
                    }

                    echo "💚 새 TargetGroup Healthy 완료 → ALB 전환 시작"

                    withAWS(region: 'ap-northeast-2', credentials: 'aws-access-key') {
                        sh """
                            aws elbv2 modify-listener \
                                --listener-arn ${LISTENER_ARN} \
                                --default-actions Type=forward,TargetGroupArn=${env.NEXT_TG_ARN}
                        """
                    }
                }
            }
        }

        /* =======================================================================
         * 6. Discord 알림
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
