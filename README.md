Testing
=======

DO NOT RUN THIS TEST LOCALLY USING DOCKER!

This test does some very specific changes, like forcefully writing to raw disks. It's a good way to render your computer unbootable.

I would suggesting using Vagrant and a CentOS VM.

Furthermore, because this role pulls in the cis-role, which does not pass tests
of idempotency, and has a few similar issues inside this role itself, the
tests for idempotence have been disabled in molecule.yml, and it is accepted
that they won't pass. Anyone making changes to this role in future should run
the tests before doing so, accept that this is the case, and record the test
state, then run the tests after their changes are applied to make sure no new
errors have been added.

Role Name
=========

Collection of roles to configure a server for when is newly created.
It configures:
 - ntp
 - mail
 - selinux
 - openldapclient

also creates a swap partition and formats the third disk `/dev/sdc` when/if it exists
 - diskutil

Requirements
------------

The new server needs to be created and in the correct network.
Please make sure the new server has the correct tags, verify from jenkins server.

Role Variables
--------------

NTP uses different variables for `SERVER` or `INTERNAL` modes:
```yaml
ntp_server_servers:
  - "x"
  - "y"
  - "z"

ntp_internal_server: "f"
```

Dependencies
------------

A list of other roles hosted on Galaxy should go here, plus any details in regards to parameters that may need to be set for other roles, or variables that are used from other roles.

Example Playbook
----------------

```yaml
---
 - hosts: all
   vars:
    realenv: "{{ lookup('env','AZURE_PROFILE') | default('prod',true) }}"
   become: true
   roles:
    - { role: bootstrap, mode: internal }
```

it is important to give the correct `{{ mode }}`
to the NTP role, as it will configure differently a Server than an Internal box.

```yaml
mode = { server | internal }
```

this is set in the `playbook.yml`

License
-------
MIT

Author Information
------------------
HMCTS Reform Programme
