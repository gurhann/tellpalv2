"""P4a2 launcher'ın bulut öncesi ret kapıları."""
import contextlib
import base64
import os
import hashlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import p4_local_comparison as p4


class P4LocalComparisonTest(unittest.TestCase):
    def test_credential_gate_requires_reserved_login_real_uid_claims_and_external_private_token(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory).resolve()
            common={'runId':p4.RUN_ID,'planSha256':p4.PLAN_SHA,'qaUid':'jLRypTmoBrhhX21HsDJojurcKTC3'}
            attempt={**common,'reservedQaSignInInvocations':1,'reservedFirebaseVerifierInvocations':1}
            claims={'sub':common['qaUid'],'aud':'tellpal-ee0dd',
                    'iss':'https://securetoken.google.com/tellpal-ee0dd','iat':100,'exp':3700}
            token=path/'token'
            def save_claims():
                encoded=base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=')
                token.write_text('header.'+encoded+'.signature')
                token.chmod(0o600)
            save_claims()
            ready={**common,'tokenFile':str(token),'verifiedAtEpochMillis':100000,'expiresAtEpochMillis':3700000}
            for filename,value in [('qa-sign-in.attempt.json',attempt),('qa-sign-in.ready.json',ready)]:
                (path/filename).write_text(json.dumps(value))
                (path/filename).chmod(0o600)
            self.assertRegex(p4.credential_gate(path,now=100000),r'^[0-9a-f]{64}$')
            claims['sub']='wrong'
            save_claims()
            with self.assertRaisesRegex(ValueError,'QA_TOKEN_CLAIMS'):
                p4.credential_gate(path,now=100000)
            claims['sub']=common['qaUid']
            save_claims()
            with self.assertRaisesRegex(ValueError,'QA_CREDENTIAL_EXPIRED'):
                p4.credential_gate(path,now=3700000)
            token.chmod(0o644)
            with self.assertRaisesRegex(ValueError,'PRIVATE_INPUT_REQUIRED'):
                p4.credential_gate(path,now=100000)

    def test_sdk_configuration_rejects_shared_or_unknown_project_before_launch(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory).resolve()/'sdk'
            path.write_text(json.dumps({'projectId':'other','bucket':'wrong','credentialsPath':'/private/no'}))
            path.chmod(0o600)
            with self.assertRaisesRegex(ValueError,'SDK_PROJECT_BUCKET_CHANGED'):
                p4.sdk_configuration(path)
            path.chmod(0o644)
            with self.assertRaisesRegex(ValueError,'PRIVATE_SDK_CONFIG_REQUIRED'):
                p4.sdk_configuration(path)

    def test_default_does_not_load_any_inputs_or_launch_process(self):
        with patch.object(p4, 'checked_plan', side_effect=AssertionError('plan read')), \
                patch.object(p4, 'load_environment', side_effect=AssertionError('credential read')), \
                patch.object(p4.subprocess, 'run', side_effect=AssertionError('process')), \
                contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(0, p4.main([]))
            self.assertIn('ZERO_FILE_CREDENTIAL_DB_NETWORK_IO', output.getvalue())

    def test_exact_approved_plan_and_growth_profile(self):
        plan = p4.checked_plan()
        self.assertEqual([822, 1334], plan['geometry']['expectedOutput'])
        self.assertIn('allow-growth=false', p4.overlay(plan))
        self.assertIn('profiles.READING_PAGE.heap-mi-b=256', p4.overlay(plan))

    def test_plan_byte_drift_refuses(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory).resolve() / 'plan.json'
            path.write_bytes(p4.PLAN.read_bytes() + b'\n')
            with self.assertRaisesRegex(ValueError, 'PLAN_HASH_CHANGED'):
                p4.checked_plan(path)

    def test_source_hash_drift_and_symlink_refuse(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory).resolve() / 'source.png'
            path.write_bytes(b'new')
            source = {'bytes': 3, 'sha256': hashlib.sha256(b'old').hexdigest()}
            with self.assertRaisesRegex(ValueError, 'SOURCE_HASH_CHANGED'):
                p4.source_files({'sources': [source]*3}, [path]*3)
            link = path.with_name('link.png')
            link.symlink_to(path)
            with self.assertRaisesRegex(ValueError, 'SOURCE_PATH_INVALID'):
                p4.source_files({'sources': [source]*3}, [link]*3)

    def test_remote_and_clone_source_database_refuse(self):
        for target in ('jdbc:postgresql://remote/db', 'jdbc:postgresql://localhost/' + p4.CLONE,
                       'jdbc:postgresql://localhost/db?sslmode=disable', 'jdbc:postgresql://user:secret@localhost/db'):
            with self.assertRaises(ValueError):
                p4.database_urls({'TELLPAL_DB_URL': target})

    def test_source_read_only_is_enforced_in_every_pg_client_environment(self):
        environment = p4.pg_environment({'PATH': '/bin', 'PGOPTIONS': 'bad',
                                         'TELLPAL_DB_PASSWORD': 'private'}, True)
        self.assertEqual('-c default_transaction_read_only=on', environment['PGOPTIONS'])
        self.assertEqual('private', environment['PGPASSWORD'])

    def test_source_migration_or_existing_clone_refuses_before_mutation(self):
        for results in (['source:5432:role:off'], ['source:5432:role:on', '37'],
                        ['source:5432:role:on', '36', '1']):
            with patch.object(p4, 'pg_query', side_effect=results), \
                    patch.object(p4, 'clone_database', side_effect=AssertionError('write')):
                with self.assertRaises(ValueError):
                    p4.source_identity({}, 'jdbc:postgresql://localhost/source')

    def test_existing_attempt_survives_restart_and_refuses_launch(self):
        plan=p4.checked_plan()
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory).resolve()
            (path/'attempt.json').write_text('failed')
            args=type('Args', (), {'source':[], 'sdk_config':None, 'approval_sha256':p4.PLAN_SHA, 'java':None, 'preflight':False})()
            with patch.object(p4, 'DIRECTORY', path), patch.object(p4, 'source_files', return_value=[]), \
                    patch.object(p4, 'source_identity', return_value='source:on'), patch.object(p4, 'sdk_configuration', return_value={}), \
                    patch.object(p4, 'clone_database', side_effect=AssertionError('clone')):
                with self.assertRaisesRegex(ValueError, 'EXISTING_ATTEMPT_NO_RETRY'):
                    p4.launch(args,plan,{})
            self.assertEqual('failed', (path/'attempt.json').read_text())


if __name__ == '__main__':
    unittest.main()
