pipeline {
  agent any
  options { timestamps() }   // <-- removed ansiColor

  environment {
    // Make sure Jenkins can find snap-installed trivy
    PATH = "/snap/bin:/usr/local/bin:/usr/bin:/bin"
    TRIVY_SEVERITY  = 'CRITICAL,HIGH'
    TRIVY_EXIT_CODE = '0'  // set to '1' to fail build on vulns

    FRONTEND_TAG = "chattingo-frontend:jenkins-${BUILD_NUMBER}"
    BACKEND_TAG  = "chattingo-backend:jenkins-${BUILD_NUMBER}"
  }

  stages {
    stage('Git Clone') {
      steps {
        // SCM checkout happens automatically for "Pipeline script from SCM"
        sh 'git rev-parse --short HEAD || true'
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
        // use trivy from PATH (snap puts it in /snap/bin)
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          sh 'trivy fs --no-progress --exit-code ${TRIVY_EXIT_CODE} --severity ${TRIVY_SEVERITY} .'
        }
      }
    }

    stage('Image Scan') {
      parallel {
        stage('Scan Backend Image') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh 'trivy image --no-progress --exit-code ${TRIVY_EXIT_CODE} --severity ${TRIVY_SEVERITY} ${BACKEND_TAG}'
            }
          }
        }
        stage('Scan Frontend Image') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh 'trivy image --no-progress --exit-code ${TRIVY_EXIT_CODE} --severity ${TRIVY_SEVERITY} ${FRONTEND_TAG}'
            }
          }
        }
      }
    }

    stage('Push to Registry') { steps { echo 'Local-only build — skipping push.' } }
    stage('Update Compose')   { steps { echo 'Compose builds locally — skipping tag update.' } }

    stage('Deploy') {
      steps {
        sh '''
          docker compose down
          docker compose up -d --build
          docker compose ps
          echo "Backend health:" && curl -sS --max-time 10 http://localhost:8081/actuator/health || true
          echo "Frontend head:"  && curl -sSI --max-time 10 http://localhost:3000             || true
        '''
      }
    }
  }

  post {
    always {
      sh 'docker image prune -f || true'
    }
  }
}
