from __future__ import annotations

import argparse
import getpass
import os
import sys

from tellpal_admin_client import TellPalAdminClient, validate_base_url


def main() -> int:
    parser = argparse.ArgumentParser(description="Manually recover one verified partial lullaby import.")
    parser.add_argument("content_id", type=int)
    parser.add_argument("--external-key", required=True)
    parser.add_argument("--musician", required=True)
    parser.add_argument("--languages", nargs="+", required=True)
    arguments = parser.parse_args()
    base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
    username = _required_environment("TELLPAL_ADMIN_USERNAME")
    if not sys.stdin.isatty():
        raise RuntimeError("Recovery requires an interactive terminal")
    client = TellPalAdminClient(base_url)
    try:
        password = getpass.getpass(f"TellPal admin password for {username}: ")
        client.login(username, password)
        content = client.get_content(arguments.content_id)
        _verify_partial_content(content, arguments.external_key, set(arguments.languages))
        matches = _exact_matches(client.search_contributors(arguments.musician), arguments.musician)
        if len(matches) > 1:
            raise RuntimeError("Contributor is ambiguous; recovery stopped")
        action = "reuse" if matches else "create"
        print(f"Verified partial content {arguments.content_id}; contributor action: {action}; publish: {', '.join(arguments.languages)}")
        if input("Type 'repair' to apply recovery writes: ") != "repair":
            print("Recovery cancelled.")
            return 1
        contributor_id = matches[0]["contributorId"] if matches else client.create_contributor(arguments.musician, ["MUSICIAN"])["contributorId"]
        client.assign_contributor(arguments.content_id, {"contributorId": contributor_id, "role": "MUSICIAN", "languageCode": None, "creditName": None, "sortOrder": 0})
        for language in arguments.languages:
            client.publish_localization(arguments.content_id, language)
        print(f"Recovery completed for content {arguments.content_id}.")
        return 0
    finally:
        client.logout()


def _verify_partial_content(content: dict[str, object], external_key: str, languages: set[str]) -> None:
    if content.get("type") != "LULLABY" or content.get("externalKey") != external_key:
        raise RuntimeError("Content identity does not match the approved partial import")
    actual_languages = {item.get("languageCode") for item in content.get("localizations", [])}
    if actual_languages != languages:
        raise RuntimeError(f"Localization set does not match: expected={sorted(languages)}, actual={sorted(actual_languages)}")
    if not content.get("listingCoverMediaId") or not content.get("listeningCoverMediaId") or not (content.get("playback") or {}).get("audioMediaId"):
        raise RuntimeError("Partial content is missing required cover or playback media")


def _exact_matches(items: list[dict[str, object]], name: str) -> list[dict[str, object]]:
    expected = name.strip().casefold()
    return [item for item in items if str(item.get("displayName", "")).strip().casefold() == expected]


def _required_environment(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Required environment variable is missing: {name}")
    return value


if __name__ == "__main__":
    raise SystemExit(main())
