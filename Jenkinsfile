pipeline {
    agent any

    tools {
        jdk 'JDK 17'  // Jenkins에 설정된 JDK 이름과 일치해야 함
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Gradle 실행 권한 부여
                sh 'chmod +x ./gradlew'
                // Gradle 빌드
                sh './gradlew clean bootJar'
            }
        }

        stage('Docker Build & Push') {
            steps {
                // Docker 이미지 빌드
                sh "docker build --platform=linux/amd64 -t qkrehdwns032/hairwhere:${BUILD_NUMBER} ."

                // Docker 로그인 및 이미지 푸시
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                    sh 'echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin'
                    sh "docker push qkrehdwns032/hairwhere:${BUILD_NUMBER}"
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'gcp-ssh-key', keyFileVariable: 'SSH_KEY')]) {
                    sh '''
                        chmod 600 ${SSH_KEY}
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} dj@34.64.196.67 '
                        # 기존 app-container 중지 및 제거
                        docker stop app-container || true
                        docker rm app-container || true

                        # 새 이미지 pull 및 실행
                        docker pull qkrehdwns032/hairwhere:'"${BUILD_NUMBER}"'
                        docker run -d --name app-container -p 8080:8080 qkrehdwns032/hairwhere:'"${BUILD_NUMBER}"'
                        '
                    '''
                }
            }
        }
    }
}
