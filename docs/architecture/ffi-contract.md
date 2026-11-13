# Rust ↔ mobile FFI

Phase 0 exports pocketforge_abi_version() -> u32 and pocketforge_health_check() -> i32. Error codes: 0 success, 1 invalid argument, 2 unsupported, 3 internal failure, 4 incompatible ABI.

Rust internals remain typed. Only C-compatible values cross the boundary. No panic unwinds across FFI. Validate pointers/lengths/UTF-8 before dereference; document ownership/threading; use explicit release functions for future buffers. Enable #![deny(unsafe_op_in_unsafe_fn)] and comment every unsafe block. Shell checks ABI version at startup. Breaking changes increment ABI version.

Android JNI forwards to the C ABI. Future Swift may use UniFFI or C wrapper; iOS build remains gated.
