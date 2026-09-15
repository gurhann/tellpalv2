Conduct a review of CONTENT.
Look for what's missing, not only what's wrong.
Find at least ten issues to fix or improve.
Output a Markdown list of findings only — no severity, priority, or ranking.
If the content is empty, stop and say so.
If you have zero findings, re-check and keep thinking; do not stop with an empty list.

CONTENT:

Review the current working-tree changes from baseline commit `3d678ca3ab244f1c66a28e58f76050fb5e6906d4` for the Firebase audio-story importer. The implementation adds:

- `.codex/skills/import-tellpal-story/scripts/audio_story_manifest.py`: four-column CSV parsing, row-level validation, duplicate classification, Firebase cover/ZIP staging, single-MP3 validation, duration/hash calculation, and sidecar CSV writing.
- `.codex/skills/import-tellpal-story/scripts/audio_story_import_workflow.py`: exact STORY-localization matching, existing asset conflict checks, shared listening-cover handling, narration PUT updates, per-row continuation, unknown mutation stop, and verification.
- `.codex/skills/import-tellpal-story/scripts/audio_story_import_report.py`: non-secret JSON run diagnostics.
- `.codex/skills/import-tellpal-story/scripts/inspect_audio_stories.py` and `import_audio_stories.py`: dry-run/live CLI with Firebase options, masked password, and exact `import` confirmation.
- `.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py`: PUT localization client method.
- `.codex/skills/import-tellpal-story/scripts/tests/test_audio_story_import.py`: nine tests covering success, matching errors, duplicates, invalid storage, conflicts, existing imports, source changes, and ambiguous mutation transport.
- `.codex/skills/import-tellpal-story/references/audio-story-import.md` and `SKILL.md`: operator documentation.

The source CSV is `cms/yuklenecek_hikayeler/audio_stories/audio_stories.csv`; it must remain unchanged. The persisted model is existing STORY localization narration plus content-level listeningCoverMediaId, never canonical AUDIO_STORY. Review the actual changed files and tests in the working tree, including interactions with the existing lullaby/meditation import helpers.
