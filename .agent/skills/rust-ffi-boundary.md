# Skill: rust-ffi-boundary

## When to invoke
Rust crosses JNI or future Swift/C ABI.

## Preconditions
Versioned FFI contract exists.

## Exact procedure
Keep typed internals; expose C-compatible values; validate pointers/lengths/UTF-8; catch panics; map stable error codes; document ownership/threading.

## Verification
Tests cover invalid inputs, ownership and errors; unsafe lint enabled.

## Failure modes
Never unwind across FFI; escalate contract changes.
