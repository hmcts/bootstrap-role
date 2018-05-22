#!groovy

@Library('Reform') _

if (env.HOSTNAME_PARAM == "") {
    currentBuild.description = "$AZURE_TAGS - $AZURE_PROFILE"
} else {
    currentBuild.description = "$HOSTNAME_PARAM - $AZURE_PROFILE"
}

node {
  ws('bootstrap') { // This must be the name of the role otherwise ansible won't find the role
    try {
      wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
        stage('Checkout') {
          deleteDir()
          git url: "https://github.com/hmcts/bootstrap-role.git", branch: "master"

          dir('bootstrap-role') {
            git url: "https://github.com/hmcts/bootstrap-role.git", branch: "master"
          }

          dir('ansible-management') {
            git url: "https://github.com/hmcts/ansible-management.git", branch: "master", credentialsId: "jenkins-public-github-api-token"
          }
          
          dir('cis') {
            git url: "https://github.com/hmcts/cis-role.git", branch: "master", credentialsId: "jenkins-public-github-api-token"
          }
        }

        stage('Do the needful') {
          env.ANSIBLE_HOST_KEY_CHECKING = 'False'
          // env.AZURE_PROFILE = "mgmt"
          // env.AZURE_TAGS = "product:mgmt"
          // ,role:ldap"
          sh '''
          find
          env
          
          if [ "$test_mode" == "true" ]; then
            echo "In test mode!"
            python ansible-management/inventory/azure_rm.py | python -m json.tool
            exit
          fi

          ansible-galaxy install -r requirements.yml --force --roles-path=roles/
          
          chmod +x ansible-management/inventory/azure_rm.py
          
          cat << EOF > ansible.cfg
[defaults]
remote_port = \$SSH_PORT
remote_user = \$SSH_USER
private_key_file = \$SSH_KEY_FILE
EOF

          if [ "$HOSTNAME_PARAM" == "" ]; then
            ansible-playbook -i ansible-management/inventory/azure_rm.py --tags "$ANSIBLE_TAGS" run_bootstrap.yml
          else
            ansible-playbook -i "$HOSTNAME_PARAM," --tags "$ANSIBLE_TAGS" run_bootstrap.yml
          fi
          
        '''
        }

      }

    } catch (err) {
      notifyBuildFailure channel: '#coredevops'
      throw err
    } finally {
      stage('Cleanup') {
        sh '''
         echo cleanup
        '''
      }
      deleteDir()
    }
  }
}

