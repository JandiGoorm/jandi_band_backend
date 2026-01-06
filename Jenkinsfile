pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    environment {
        GHCR_OWNER = 'kyj0503'
        IMAGE_NAME = 'jandi-band'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                script {
                    echo "Running tests..."
                    sh './gradlew clean test jacocoTestReport --parallel'
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

        stage('Build and Push Image') {
            when { branch 'master' }
            steps {
                script {
                    def fullImageName = "ghcr.io/${env.GHCR_OWNER}/${env.IMAGE_NAME}:${env.BUILD_NUMBER}"
                    echo "Building image for master branch: ${fullImageName}"
                    
                    // Jenkins 빌드: application.properties.example 복사
                    sh 'cp src/main/resources/application.properties.example src/main/resources/application.properties'
                    
                    docker.build(fullImageName, '.')
                    docker.withRegistry("https://ghcr.io", 'github-token') {
                        echo "Pushing image to GHCR..."
                        docker.image(fullImageName).push()
                        docker.image(fullImageName).push('latest')
                    }
                }
            }
        }

        // 배포는 home-server에서 담당
        stage('Trigger Deploy') {
            when { branch 'master' }
            steps {
                build job: 'home-server-deploy', wait: false, propagate: false
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo '✅ Build and Push completed successfully!'
        }
        failure {
            echo '❌ Build failed!'
        }
    }
}
