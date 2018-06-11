#!groovy

@Library('Reform') _

if (env.HOSTNAME_PARAM == "") {
    currentBuild.description = "$AZURE_TAGS - $AZURE_PROFILE"
} else {
    currentBuild.description = "$HOSTNAME_PARAM - $AZURE_PROFILE"
}

secrets = [
      [$class: 'VaultSecret', path: 'secret/devops/vms_windows_admin_username', secretValues:
          [[$class: 'VaultSecretValue', envVar: 'TF_VAR_vms_windows_admin_username', vaultKey: 'value']]
      ],
      [$class: 'VaultSecret', path: 'secret/devops/vms_windows_admin_password', secretValues:
          [[$class: 'VaultSecretValue', envVar: 'TF_VAR_vms_windows_admin_password', vaultKey: 'value']]
      ]
  ]

node {
  ws("$JOB_NAME") { // This must be the name of the role otherwise ansible won't find the role
    try {
      wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
        stage('Checkout') {
          deleteDir()
          git url: "https://github.com/hmcts/ansible-management.git", branch: "update-inventory", credentialsId: "jenkins-public-github-api-token"

          dir('roles/bootstrap-role') {
            git url: "https://github.com/hmcts/bootstrap-role.git", branch: "winchg"
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
          pwd
          find
          env

          if [ "$test_mode" == "true" ]; then
            echo "In test mode!"
            python inventory/azure_rm.py | python -m json.tool
            exit
          fi

          ansible-galaxy install -r roles/bootstrap-role/requirements.yml --force --roles-path=roles/

          chmod +x inventory/azure_rm.py

          echo '==========================='
          echo '==========================='
          echo '==========================='
          echo '==========================='
          echo '==========================='

          if [ "$windows" != "true" ]; then
            echo "DEBUG CASE 1"
            cat << EOF > ansible.cfg
[defaults]
remote_port = \$SSH_PORT
remote_user = \$SSH_USER
private_key_file = \$SSH_KEY_FILE
roles_path = roles
EOF

          else
            echo "DEBUG CASE 2"
            cat << EOF > ansible.cfg
[defaults]
ansible_user = xxx
ansible_password = xxx
ansible_port = 5986
ansible_connection = winrm
ansible_winrm_server_cert_validation = ignore
EOF
          fi

          ls -lh ansible.cfg
          cat ansible.cfg
          ansible-config view

          # File is searched in wrong location because task is directly included from pre_tasks
          mkdir -p roles/bootstrap-role/tasks/templates/
          cp -a ./roles/bootstrap-role/templates/resolv.conf.j2 ./roles/bootstrap-role/tasks/templates/resolv.conf.j2

            # Execute ansible-playbook
            ln -s roles/bootstrap-role/run_bootstrap.yml run_bootstrap.yml
            if [ "$HOSTNAME_PARAM" == "" ]; then
              ansible-config view   
              ansible-playbook -vvv -i  inventory/azure_rm.py --tags "$ANSIBLE_TAGS" run_bootstrap.yml 
            else
              ansible-playbook -vvv -i "$HOSTNAME_PARAM," --tags "$ANSIBLE_TAGS" run_bootstrap.yml
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
