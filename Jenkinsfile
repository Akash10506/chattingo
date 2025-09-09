pipeline {
    agent any

    environment {
        TRIVY_IMAGE     = 'aquasec/trivy:0.54.1'
        TRIVY_SEVERITY  = 'CRITICAL,HIGH'
        TRIVY_EXIT_CODE = '0' // set to '1' to fail build on HIGH/CRITICAL

        FRONTEND_TAG = "chattingo-frontend:jenkins-${BUILD_NUMBER}"
        BACKEND_TAG  = "chattingo-backend:jenkins-${BUILD_NUMBER}"

        HEALTH_URL  = 'http://localhost:8081/actuator/health'
    }

    stages {
        stage('Git Clone') {
            steps {
                // If using "Pipeline from SCM", Jenkins auto-checks out. Else:
                git branch: 'main', url: 'https://github.com/Akash10506/chattingo.git'
            }
        }

        stage('Image Build') {
            parallel {
                stage('Build Backend') {
                    steps {
                        sh 'docker build -t ${BACKEND_TAG} -f backend/Dockerfile backend'
                    }
                }
                stage('Build Frontend') {
                    steps {
                        sh 'docker build -t ${FRONTEND_TAG} -f frontend/Dockerfile frontend'
                    }
                }
            }
        }

        stage('Filesystem Scan') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    sh '''
                      docker run --rm \
                        -e TRIVY_CACHE_DIR=/root/.cache \
                        -v "$PWD":/src \
                        -v /var/run/docker.sock:/var/run/docker.sock \
                        -v /var/lib/jenkins/.cache/trivy:/root/.cache \
                        ${TRIVY_IMAGE} fs --no-progress --exit-code ${TRIVY_EXIT_CODE} \
                        --severity ${TRIVY_SEVERITY} /src
                    '''
                }
            }
        }

        stage('Image Scan') {
            parallel {
                stage('Scan Backend Image') {
                    steps {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            sh '''
                              docker run --rm \
                                -e TRIVY_CACHE_DIR=/root/.cache \
                                -v /var/run/docker.sock:/var/run/docker.sock \
                                -v /var/lib/jenkins/.cache/trivy:/root/.cache \
                                ${TRIVY_IMAGE} image --no-progress --exit-code ${TRIVY_EXIT_CODE} \
                                --severity ${TRIVY_SEVERITY} ${BACKEND_TAG}
                            '''
                        }
                    }
                }
                stage('Scan Frontend Image') {
                    steps {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            sh '''
                              docker run --rm \
                                -e TRIVY_CACHE_DIR=/root/.cache \
                                -v /var/run/docker.sock:/var/run/docker.sock \
                                -v /var/lib/jenkins/.cache/trivy:/root/.cache \
                                ${TRIVY_IMAGE} image --no-progress --exit-code ${TRIVY_EXIT_CODE} \
                                --severity ${TRIVY_SEVERITY} ${FRONTEND_TAG}
                            '''
                        }
                    }
                }
            }
        }

        stage('Push to Registry') {
            steps {
                echo 'Skipping (local build/deploy only — no Docker Hub)'
            }
        }

        stage('Update Compose') {
            steps {
                echo 'Skipping (no tag update required — docker-compose uses local build)'
            }
        }

        stage('Deploy') {
    steps {
        script {
            sh 'docker rm -f chattingo-mysql || true'
            sh 'docker compose down --remove-orphans'
            sh 'docker compose up -d --build'

            echo "Checking backend health..."
            for (i in 1..30) {
              out = sh(script: "curl -sfS ${HEALTH_URL} || true", returnStdout: true).trim()
              echo "Attempt $i: $out"
              if (out.contains('"status":"UP"')) {
                echo "Backend is UP"
                return
              }
              sleep 2
            }
            echo "Backend failed to become healthy"
            sh "docker logs chattingo-backend | tail -n 100"
            error "Backend health check failed"
        }
    }
}

    post {
        always {
            sh 'docker image prune -f || true'
        }
    }
}

