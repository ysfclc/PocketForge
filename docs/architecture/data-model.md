# Data model

Project(id,name,root_uri,source_kind,timestamps); ProjectPassport(project_id,vision,constraints,architecture_ref,decisions_ref); Mission(id,project_id,objective,state,acceptance,timestamps); AgentRun(id,mission_id,provider_id,capabilities,status); CapabilityGrant(subject,capability,resource_scope,expiry,approval_ref); ChangeSet(base_revision,patch_ref,review_state); BuildRun(source_revision,toolchain,variant,status,log_ref); Artifact(build_run_id,kind,sha256,local_uri,provenance_ref); Decision(adr_ref,status); ConnectorAccount(provider,credential_ref,scopes).

The first Android workspace slice currently persists a minimal Project record (id, name, description, optional SAF tree URI, createdAt) and Mission record (id, title, brief, optional projectId, state, createdAt) in app-private SharedPreferences. This is an intentionally small local schema, not yet the full versioned Passport / database model. Folder content is not copied into PocketForge; Android grants scoped access to the selected document tree.

Invariants: source tree remains code truth; Passport is versioned/exportable; mission transitions persist before side effects; changes identify base revision; artifacts are immutable by hash; secrets never stored in plaintext database.
