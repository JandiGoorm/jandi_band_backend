pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-1.21.0-openjdk-arm64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        GHCR_OWNER = 'kyj0503'
        IMAGE_NAME = 'jandi-band'
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/master']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/kyj0503/jandi_band_backend.git',
                        credentialsId: 'github-token'
                    ]]
                ])
            }
        }

        stage('Test') {
            steps {
                script {
                    echo "Running tests..."
                    sh './gradlew clean test jacocoTestReport --parallel --no-daemon'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                }
                failure {
                    echo "Tests failed. Stopping pipeline."
                }
            }
        }

        stage('Login GHCR') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'github-token', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                        sh 'echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USER --password-stdin'
                    }
                }
            }
        }

        stage('Build and Push Image') {
            steps {
                script {
                    def fullImageName = "ghcr.io/${env.GHCR_OWNER}/${env.IMAGE_NAME}"
                    echo "Building image: ${fullImageName}"
                    
                    // Jenkins 빌드: application.properties.example 복사
                    sh 'cp src/main/resources/application.properties.example src/main/resources/application.properties'
                    
                    // 빌드 및 푸시
                    sh """
                        docker build \
                            --tag ${fullImageName}:${env.BUILD_NUMBER} \
                            --tag ${fullImageName}:latest \
                            .
                        docker push ${fullImageName}:${env.BUILD_NUMBER}
                        docker push ${fullImageName}:latest
                    """
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    sh '''
                        cd /home/ubuntu/source/home-server/docker
                        docker compose -f docker-compose.apps.yml pull jandi-band
                        docker compose -f docker-compose.apps.yml up -d --force-recreate jandi-band
                        sleep 10
                        docker ps | grep jandi-band
                        echo "✅ jandi-band deployment completed!"
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    sh '''
                        sleep 20
                        curl -f https://rhythmeet-be.yeonjae.kr/actuator/health || echo "Health check pending..."
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo '✅ jandi-band Build, Push, and Deploy completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}
