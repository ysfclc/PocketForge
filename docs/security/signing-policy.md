# Signing policy

Debug key is disposable and never committed. Release signing runs only from protected main after review in GitHub Environment android-release with required reviewers. Keystore is materialized in runner temp, permission-restricted, used, verified and removed in the same job. No key/password enters agent context, artifact or cache. Missing secrets or failed verification block release; never fall back to debug signing. Maintain an offline backup and recovery plan; key loss can prevent upgrades.
