# Security Policy

## Supported Versions

To receive fixes for security vulnerabilities it is required to always upgrade to the latest version of OpenRemote.

Fixes will only be released for previous releases under special circumstances.

## Reporting a Vulnerability

You can report a security vulnerability either through email, or as a GitHub security advisory. If you are uncertain what you have
discovered is a vulnerability or you believe it is a critical issue please report using email (or both).

### Report One Vulnerability At A Time

Please report each vulnerability separately, even when you found them together and even when they share a theme. If several of them chain into a larger attack, describe the chain in each report and link them to one another.

GitHub will not issue a CVE for an advisory that covers more than one independently fixable vulnerability. This follows the CVE CNA rules, specifically [4.1 vulnerability determination](https://www.cve.org/ResourcesSupport/AllResources/CNARules#section_4-1_Vulnerability_Determination) and [4.2 CVE ID assignment](https://www.cve.org/ResourcesSupport/AllResources/CNARules#section_4-2_CVE_ID_Assignment) (4.2.6 and 4.2.11). A report that bundles several has to be split before any of it can be published, which delays the fix and your credit for all of them.

The rule of thumb is whether one change would fix everything you are describing. If two findings need two separate fixes, they are two reports.

### What To Include

- The version, release tag or commit hash you tested against. A floating tag such as `develop` does not identify a build, so we cannot tell what you were running.
- Steps to reproduce, and what you observed that shows the issue is real rather than theoretical.
- The affected endpoint, class or file.
- The full CVSS vector rather than only a number, so the score can be checked. We prefer 4.0 over 3.1.

### Where To Send It

To report through email send an email to security@openremote.io

To report through GitHub go to [https://github.com/openremote/openremote/security/advisories/new](https://github.com/openremote/openremote/security/advisories/new)

If you have a patch for the issue please use `git format-patch` and attach to the email or issue. Please do not open a
pull request on GitHub as that may disclose sensitive details around the vulnerability.

### What Happens Next

Your report is reviewed and you are told the outcome, whatever it is.

We reproduce the issue before writing a fix. If we cannot, we will come back to you rather than close it silently.

We agree the severity within the team. If we arrive at a different score from the one you assigned, we will discuss it with you before changing it.

If your report turns out to describe the same vulnerability as another one, the reports are merged and everyone who found it is credited on the advisory that is published.

If a fix is planned, the advisory is published once a release containing it is publicly available. If we do not intend to fix the vulnerability, the advisory is published anyway, with the mitigations documented.

The process behind all of this is documented in section 6 of the OpenRemote handbook, [Vulnerability Management and Coordinated Disclosure](https://handbook.openremote.io/docs/vulnerability-management-and-coordinated-disclosure).
