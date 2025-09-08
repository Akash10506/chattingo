pipeline {
  agent any
  options {
    timestamps()
    ansiColor('xterm')
  }
  environment {
    // Image tags kept local only
    FRONTEND_TAG = "chattingo-frontend:jenkins-${BUILD_NUMBER}"
    BACKEND_TAG  = "chattingo-backend:jenkins-${BUILD_NUMBER}"

    // Trivy behavior
    TRIVY_SEVERITY  = 'CRITICAL,HIGH'
    TRIVY_EXIT_CODE = '0'   // set to '1' to fail build on vulns
  }

  stages {
    stage('Git Clone') { // Clone repository from GitHub (2 Marks)
      steps {
        // If this is a Multibranch Pipeline, Jenkins checks out SCM automatically.
        // For a simple Pipeline job, uncomment and set your repo:
        // git url: 'https://github.com/<you>/<repo>.git', branch: 'main'
        sh 'git rev-parse --short HEAD || true'
      }
    }

    stage('Image Build') { // Build Docker images for frontend & backend (2 Marks)
      parallel {
        stage('Build Backend') {
          steps {
            sh '''
              docker build -t ${BACKEND_TAG} -f backend/Dockerfile backend
            '''
          }
        }
        stage('Build Frontend') {
          steps {
            sh '''
              docker build -t ${FRONTEND_TAG} -f frontend/Dockerfile frontend
            '''
          }
        }
      }
    }

    stage('Filesystem Scan') { // Security scan of source code (2 Marks)
      steps {
        // Requires trivy installed on the Jenkins node
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          sh '''
            trivy fs --no-progress --exit-code ${TRIVY_EXIT_CODE} \
              --severity ${TRIVY_SEVERITY} .
          '''
        }
      }
    }

    stage('Image Scan') { // Vulnerability scan of Docker images (2 Marks)
      parallel {
        stage('Scan Backend Image') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '''
                trivy image --no-progress --exit-code ${TRIVY_EXIT_CODE} \
                  --severity ${TRIVY_SEVERITY} ${BACKEND_TAG}
              '''
            }
          }
        }
        stage('Scan Frontend Image') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '''
                trivy image --no-progress --exit-code ${TRIVY_EXIT_CODE} \
                  --severity ${TRIVY_SEVERITY} ${FRONTEND_TAG}
              '''
            }
          }
        }
      }
    }

    stage('Push to Registry') { // Push images to Docker Hub/Registry (2 Marks)
      steps {
        echo 'Local-only build selected — skipping image push.'
      }
    }

    stage('Update Compose') { // Update docker-compose with new image tags (2 Marks)
      steps {
        echo 'Compose uses local build contexts — no tag update needed.'
      }
    }

    stage('Deploy') { // Deploy to Hostinger VPS (5 Marks)
      steps {
        // Rebuild from local folders and restart services on THIS server
        sh '''
          # If your compose uses `build:` entries, this rebuilds from source:
          docker compose down
          docker compose up -d --build
          docker compose ps

          echo 'Smoke checks…'
          curl -sS --max-time 10 http://localhost:8080/actuator/health || true
          curl -sSI --max-time 10 http://localhost:3000 || true
        '''
      }
    }
  }

  post {
    always {
      echo 'Pruning dangling images…'
      sh 'docker image prune -f || true'
    }
  }
}
