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

        LISTENER_ARN = "arn:aws:elasticloadbalancing:ap-northeast-2:430118833260:listener/app/geulda-alb/c37d33ae4e691f29/80942d2924901550"
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
                    echo "org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=256m" >> ~/.gradle/gradle.properties

                    chmod +x ./gradlew
                    ./gradlew clean build -x test --parallel --configure-on-demand
                '''
            }
        }

        /* =======================================================================
         * 2. Docker Build + Push (항상 최신 코드 보장)
         * ======================================================================= */
        stage('Docker Build & Push (No Cache)') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-cred',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    echo "🐳 Docker Build - 항상 최신 코드로 빌드"

                    sh '''
                        # 이전 빌드 이미지 정리 (선택적)
                        docker rmi $IMAGE_NAME:latest 2>/dev/null || true

                        # 캐시 없이 항상 최신 코드로 빌드
                        docker build \
                            --no-cache \
                            --pull \
                            -t $IMAGE_NAME:latest .

                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push $IMAGE_NAME:latest

                        echo "✅ Docker 이미지 빌드 및 푸시 완료: $IMAGE_NAME:latest"
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
        
       /*
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
        */

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
     * 실패 시 자동 롤백 및 알림
     * ======================================================================= */
    post {
        failure {
            script {
                echo "❌ 배포 실패 감지 — 자동 롤백 시작"

                // 배포 실패 시 이전 Target Group으로 롤백
                if (env.DEPLOY_TARGET) {
                    def previousTG = (env.DEPLOY_TARGET == "blue") ? env.GREEN_TG_ARN : env.BLUE_TG_ARN
                    def previousColor = (env.DEPLOY_TARGET == "blue") ? "GREEN" : "BLUE"

                    try {
                        echo "🔄 ${previousColor} 환경으로 롤백 중..."

                        sh """
                            aws elbv2 modify-listener \
                                --listener-arn ${LISTENER_ARN} \
                                --default-actions Type=forward,TargetGroupArn=${previousTG}
                        """

                        echo "✅ 롤백 완료: ${previousColor} 환경으로 복구됨"

                        // 롤백 성공 알림
                        sh """
                            curl -H "Content-Type: application/json" \
                                -d '{"content": ":warning: **GEULDA 배포 실패 - 자동 롤백 완료**\\n- 실패한 환경: ${env.DEPLOY_TARGET.toUpperCase()}\\n- 복구된 환경: ${previousColor}\\n- 상태: 정상 운영 중"}' \
                                "$DISCORD_WEBHOOK"
                        """
                    } catch (Exception e) {
                        echo "❌ 롤백 실패: ${e.message}"

                        // 롤백 실패 긴급 알림
                        sh """
                            curl -H "Content-Type: application/json" \
                                -d '{"content": ":rotating_light: **긴급: GEULDA 배포 실패 및 롤백 실패**\\n- 즉시 수동 확인 필요\\n- 에러: ${e.message}"}' \
                                "$DISCORD_WEBHOOK"
                        """
                    }
                } else {
                    // DEPLOY_TARGET이 없는 경우 (빌드 단계 실패 등)
                    sh """
                        curl -H "Content-Type: application/json" \
                            -d '{"content": ":x: **GEULDA 빌드 실패**\\n- 배포 전 단계에서 실패\\n- 현재 운영 환경은 영향 없음"}' \
                            "$DISCORD_WEBHOOK"
                    """
                }
            }
        }

        success {
            echo "✅ 배포 파이프라인 성공적으로 완료"
        }
    }
}
