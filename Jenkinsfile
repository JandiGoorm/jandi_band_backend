pipeline {
    agent any

    environment {
        GHCR_OWNER = 'kyj0503'
        PROD_IMAGE_NAME = 'rhythmeet-be'
        DEV_IMAGE_NAME = 'rhythmeet-be-dev'
        EC2_HOST = 'rhythmeet.yeonjae.kr'
        EC2_USER = 'ubuntu'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // --- Master 브랜치 전용 스테이지 ---
        stage('Build and Push PROD Image') {
            when { branch 'master' }
            steps {
                script {
                    // 변수를 이 단계에서 명확하게 정의
                    def fullImageName = "ghcr.io/${env.GHCR_OWNER}/${env.PROD_IMAGE_NAME}:${env.BUILD_NUMBER}"
                    echo "Building PROD image for master branch: ${fullImageName}"
                    
                    docker.build(fullImageName, '.')
                    docker.withRegistry("https://ghcr.io", 'github-token') {
                        echo "Pushing PROD image to GHCR..."
                        docker.image(fullImageName).push()
                    }
                }
            }
        }

        stage('Deploy to Production (EC2)') {
            when { branch 'master' }
            steps {
                script {
                    // 배포할 이미지 이름을 다시 명확하게 정의
                    def fullImageName = "ghcr.io/${env.GHCR_OWNER}/${env.PROD_IMAGE_NAME}:${env.BUILD_NUMBER}"
                    withCredentials([sshUserPrivateKey(credentialsId: 'ec2-ssh-key', keyFileVariable: 'EC2_PRIVATE_KEY')]) {
                        echo "Deploying to EC2 host: ${env.EC2_HOST}"
                        sh """
                            ssh -o StrictHostKeyChecking=no -i \${EC2_PRIVATE_KEY} ${env.EC2_USER}@${env.EC2_HOST} \
                            "bash /home/ubuntu/spring-app/deploy.sh ${fullImageName}"
                        """
                    }
                }
            }
        }

        // --- Dev 브랜치 전용 스테이지 ---
        stage('Build and Push DEV Image') {
            when { branch 'dev' }
            steps {
                script {
                    def fullImageName = "ghcr.io/${env.GHCR_OWNER}/${env.DEV_IMAGE_NAME}:${env.BUILD_NUMBER}"
                    echo "Building DEV image for dev branch: ${fullImageName}"

                    docker.build(fullImageName, '.')
                    docker.withRegistry("https://ghcr.io", 'github-token') {
                        echo "Pushing DEV image to GHCR..."
                        docker.image(fullImageName).push()
                    }
                }
            }
        }

        stage('Deploy to Development (Local)') {
            when { branch 'dev' }
            steps {
                script {
                    def fullImageName = "ghcr.io/${env.GHCR_OWNER}/${env.DEV_IMAGE_NAME}:${env.BUILD_NUMBER}"
                    echo "Deploying to local on-premise server"
                    sh "bash /opt/spring-app-dev/deploy.sh ${fullImageName}"
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
