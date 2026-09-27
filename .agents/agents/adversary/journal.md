# Adversary Journal: Attack Vectors & Vulnerability Records

## 2026-09-27 - Fail-Open Ciphertext Emission on Decryption Exception
**Attack Scenario:** In `AutofillAuthActivity` and `ShellGuardAutofillService`, decryption errors previously defaulted to returning `pearl.secret`. An attacker or corrupted state caused the app to populate target password input fields with raw JSON ciphertext, transmitting the user's encrypted envelope over the network.
**Root Cause:** Careless catch block (`catch (e: Exception) { pearl.secret }`) falling open instead of failing closed.
**Remediation:** Enforced strict fail-closed policy (`setResult(Activity.RESULT_CANCELED); finish()` or `continue` in dataset loop with `null` password).

## 2026-09-27 - PendingIntent HashCode RequestCode Collision
**Attack Scenario:** `PendingIntent.getActivity()` used `pearl.id.hashCode()` with `FLAG_CANCEL_CURRENT`. On 32-bit hash collision across UUIDs, Account A's intent was canceled and overwritten by Account B's, confusing credential delivery.
**Root Cause:** Relying on `String.hashCode()` for `PendingIntent` uniqueness.
**Remediation:** Added distinct data URI (`authIntent.data = Uri.parse("shellguard://autofill/pearl/${pearl.id}")`) with `FLAG_UPDATE_CURRENT`. Android matches `PendingIntent` identity via `Intent.filterEquals()`.

## 2026-09-27 - Asymmetric Package Prefix Domain Matching Bypass
**Attack Scenario:** `DomainMatcher.isMatch()` used `vUrl.startsWith("androidapp://") || rTarget.startsWith("androidapp://")`. If a vault item was `https://example.com` and a malicious app passed `androidapp://https://example.com`, `removePrefix` stripped the scheme from both and produced a match.
**Root Cause:** Asymmetric OR condition in scheme handling.
**Remediation:** Required `vIsApp && rIsApp` and strictly rejected cross-matching if only one side is an app.
