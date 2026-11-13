# Skill: zip-extraction-safety

## When to invoke
Any ZIP is read/extracted.

## Preconditions
Isolated staging directory and maintained ZIP library.

## Exact procedure
Cap compressed/expanded size and entries; reject absolute/drive/parent paths, NUL, symlink/special files, duplicates and forbidden extensions; extract without following links; verify resolved paths remain inside root.

## Verification
Valid fixture passes; traversal, symlink and bomb fixtures fail.

## Failure modes
Reject and clean staging on uncertainty.
