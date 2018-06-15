#!groovy

@Library('Reform') _

if (env.HOSTNAME_PARAM == "") {
    currentBuild.description = "$AZURE_TAGS - $AZURE_PROFILE"
} else {
    currentBuild.description = "$HOSTNAME_PARAM - $AZURE_PROFILE"
}

node {
  ws("$JOB_NAME") { // This must be the name of the role otherwise ansible won't find the role
    try {
      wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
        stage('Checkout') {
          deleteDir()
          git url: "https://github.com/hmcts/ansible-management.git", branch: "master", credentialsId: "jenkins-public-github-api-token"

          dir('roles/bootstrap-role') {
            git url: "https://github.com/hmcts/bootstrap-role.git", branch: "master"
          }

          dir('roles/prebootstrap-role') {
            git url: "https://github.com/hmcts/prebootstrap-role.git", branch: "master", credentialsId: "jenkins-public-github-api-token"
          }

          dir('roles/cis') {
            git url: "https://github.com/hmcts/cis-role.git", branch: "master", credentialsId: "jenkins-public-github-api-token"
          }
        }

        stage('Do the needful') {
          env.ANSIBLE_HOST_KEY_CHECKING = 'False'
          // env.AZURE_PROFILE = "mgmt"
          // env.AZURE_TAGS = "product:mgmt"
          // ,role:ldap"
          sh '''

          if [ "$test_mode" == "true" ]; then
            echo "In test mode!"
            python inventory/azure_rm.py | python -m json.tool
            exit
          fi

          ansible-galaxy install -r roles/bootstrap-role/requirements.yml --force --roles-path=roles/

          chmod +x inventory/azure_rm.py

          if [ "$windows" != "true" ]; then
            cat << EOF > ansible.cfg
[defaults]
remote_port = \$SSH_PORT
remote_user = \$SSH_USER
private_key_file = \$SSH_KEY_FILE
roles_path = roles
EOF

          else
            cat << EOF > ansible.cfg
[privilege_escalation]
become = False
EOF
          fi

            # Execute ansible-playbook
            ln -s roles/bootstrap-role/run_bootstrap.yml run_bootstrap.yml
            if [ "$HOSTNAME_PARAM" == "" ]; then
              ansible-playbook -i inventory/azure_rm.py --tags "$ANSIBLE_TAGS" run_bootstrap.yml
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
