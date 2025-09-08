pipeline {
  agent any

  options { timestamps() }

  environment {
    // --- Trivy via Docker (avoids snap/permissions issues) ---
    TRIVY_IMAGE    = 'aquasec/trivy:0.54.1'
    TRIVY_SEVERITY = 'CRITICAL,HIGH'
    // set to '1' to fail build on HIGH/CRITICAL vulns
    TRIVY_EXIT_CODE = '0'

    // --- Local image tags (no external registry) ---
    FRONTEND_TAG = "chattingo-frontend:jenkins-${BUILD_NUMBER}"
    BACKEND_TAG  = "chattingo-backend:jenkins-${BUILD_NUMBER}"

    // --- Deploy using the LIVE compose file Jenkins can read ---
    DEPLOY_COMPOSE = '/opt/chattingo/docker-compose.yml'
  }

  stages {
    stage('Git Clone') { 
      // Clone repository from GitHub (2 Marks)
      steps {
        // (SCM checkout is done automatically by Jenkins for "Pipeline from SCM")
        sh 'git rev-parse --short HEAD || true'
      }
    }

    stage('Image Build') { 
      // Build Docker images for frontend & backend (2 Marks)
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
      // Security scan of source code (2 Marks)
      steps {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          sh '''
            UID=$(id -u); GID=$(id -g)
            docker run --rm -u "$UID:$GID" \
              -v "$PWD":/src \
              -v /var/run/docker.sock:/var/run/docker.sock \
              -v "$HOME/.cache/trivy":/root/.cache \
              ${TRIVY_IMAGE} fs --no-progress --exit-code ${TRIVY_EXIT_CODE} \
              --severity ${TRIVY_SEVERITY} /src
          '''
        }
      }
    }

    stage('Image Scan') { 
      // Vulnerability scan of Docker images (2 Marks)
      parallel {
        stage('Scan Backend Image') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '''
                UID=$(id -u); GID=$(id -g)
                docker run --rm -u "$UID:$GID" \
                  -v /var/run/docker.sock:/var/run/docker.sock \
                  -v "$HOME/.cache/trivy":/root/.cache \
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
                UID=$(id -u); GID=$(id -g)
                docker run --rm -u "$UID:$GID" \
                  -v /var/run/docker.sock:/var/run/docker.sock \
                  -v "$HOME/.cache/trivy":/root/.cache \
                  ${TRIVY_IMAGE} image --no-progress --exit-code ${TRIVY_EXIT_CODE} \
                  --severity ${TRIVY_SEVERITY} ${FRONTEND_TAG}
              '''
            }
          }
        }
      }
    }

    stage('Push to Registry') { 
      // Push images to Docker Hub/Registry (2 Marks)
      steps {
        echo 'Local-only build — skipping image push.'
      }
    }

    stage('Update Compose') { 
      // Update docker-compose with new image tags (2 Marks)
      steps {
        echo 'Compose builds locally — no tag update required.'
      }
    }

    stage('Deploy') { 
      // Deploy to Hostinger VPS (5 Marks)
      steps {
        sh '''
          # Deploy against the live compose under /opt/chattingo
          docker compose -f ${DEPLOY_COMPOSE} down
          docker compose -f ${DEPLOY_COMPOSE} up -d --build --remove-orphans
          docker compose -f ${DEPLOY_COMPOSE} ps

          echo "Backend health:" && curl -sS --max-time 15 http://localhost:8081/actuator/health || true
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
