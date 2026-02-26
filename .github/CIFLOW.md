# Development Version and Branch Handling

This document describes the Git branching strategy, versioning policy, and CI/CD workflows for JUDO NG modules. The strategy is based on [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).

## Branches

The repository uses five types of branches, each with a specific purpose in the development lifecycle:

| Branch Pattern | Base Branch | Purpose |
|----------------|-------------|---------|
| `develop` | — | Main development branch; contains the latest development sources of the current active version |
| `feature/JNG-NUMBER_short_summary` | `develop` | Feature branches for new work to be included in the next release |
| `(release/)X.Y.Z` | `develop` | Release branches for stabilization and testing before a final release (`release/` prefix is reserved for CI) |
| `bugfix/JNG-NUMBER_short_summary` | release branch | Bug fixes applied during release testing; must be propagated to newer release and development branches |
| `support/JNG-NUMBER_short_summary` | release branch | Minor changes for a previous release; merged back to the release branch when the update ships |
| `master` | — | Contains the latest released (stable) sources of the current active version |
| `hotfix/JNG-NUMBER_short_summary` | `master` | Urgent fixes applied to both release and master branches |

### Branch Flow

```mermaid
gitGraph
    commit id: "init"
    branch develop
    checkout develop
    commit id: "dev-1"
    branch feature/JNG-1
    commit id: "feat-1a"
    commit id: "feat-1b"
    checkout develop
    merge feature/JNG-1 id: "merge-feat-1"
    branch feature/JNG-3
    commit id: "feat-3"
    checkout develop
    merge feature/JNG-3 id: "merge-feat-3"
    branch release/1.0-beta1
    commit id: "rc-1"
    branch bugfix/JNG-4
    commit id: "fix-4"
    checkout release/1.0-beta1
    merge bugfix/JNG-4 id: "merge-fix"
    checkout develop
    merge release/1.0-beta1 id: "merge-release"
    checkout main
    merge release/1.0-beta1 id: "release-1.0"
```

## Version Numbers

Version numbers follow **semantic versioning** with these rules:

| Event | Version Change |
|-------|---------------|
| Starting a `feature/` branch | No change — inherits from `develop` |
| Starting a `release/` branch | 2nd number on `develop` is incremented |
| `bugfix/` branch on a release | No change — fixes are applied before the release ships |
| Starting a `support/` branch | 3rd number is incremented (minor changes for a previous release) |
| Starting a `hotfix/` branch | 4th number is incremented (applied to both release and master) |

## GitHub Actions CI/CD Workflows

The project uses four interconnected GitHub Actions workflows that automate building, tagging, merging, and releasing.

### build.yml — Build & Deploy

This is the primary workflow, triggered on pushes to `develop` or pull requests targeting `develop`, `master`, `increment/*`, or `release/*`.

```mermaid
flowchart TD
    A["Trigger:<br/>push on develop OR<br/>PR on develop/master/increment/release"] --> B{Branch type?}
    B -->|master, release/*| C["Set version from pom.xml<br/>(without -SNAPSHOT)"]
    B -->|develop, increment/*| D["Set version as<br/>major.minor.qualifier.date_commitId_branch"]
    C --> E["Build & deploy to Nexus"]
    D --> E
    E --> F["Create git tag<br/>v&lt;version&gt;"]
    F --> G{Branch type?}
    G -->|increment/*, release/*| H["Create tag merge-pr/&lt;version&gt;"]
    H --> I["Triggers merge-pr-tagged.yml"]
    G -->|develop| J["Build changelog"]
    J --> K["Create GitHub pre-release"]
    G -->|other| L["Done"]
```

### merge-pr-tagged.yml — Auto-Merge Pull Requests

Triggered when a `merge-pr/*` tag is pushed. Decides whether to merge to `master` or squash to `develop` based on the version format.

```mermaid
flowchart TD
    A["Trigger: push on merge-pr/* tag"] --> B["Extract version from tag"]
    B --> C{Version format?}
    C -->|major.minor.qualifier| D["Merge PR to master"]
    D --> E["Triggers create-release-on-master.yml"]
    C -->|other| F["Squash PR to develop"]
    F --> G["Triggers build.yml"]
    D --> H["Delete merge-pr tag"]
    F --> H
```

### create-release-on-master.yml — Create Stable Release

Triggered on pushes to `master`. Builds a changelog and creates a GitHub release (marked as latest).

```mermaid
flowchart TD
    A["Trigger: push on master"] --> B["Get version from tag"]
    B --> C["Build changelog"]
    C --> D["Create GitHub release (latest)"]
```

### release.yml — Manual Release Trigger

Manually triggered with a version parameter. Creates release pull requests for both `master` and `develop`.

```mermaid
flowchart TD
    A["Trigger: manual with version param"] --> B{Version = 'auto'?}
    B -->|yes| C["Use version from pom.xml<br/>(without -SNAPSHOT)"]
    B -->|no| D["Use provided version"]
    C --> E["Set next version = qualifier + 1"]
    D --> E
    E --> F["Create PR on master<br/>with release version"]
    E --> G["Create PR on develop<br/>with next version"]
    F --> H["Triggers build.yml"]
    G --> I["Triggers build.yml"]
```

### Workflow Interaction Overview

```mermaid
flowchart LR
    Release["release.yml<br/>(manual)"] -->|creates PRs| Build["build.yml<br/>(auto)"]
    Build -->|tags merge-pr/*| MergePR["merge-pr-tagged.yml<br/>(auto)"]
    MergePR -->|merges to master| CreateRelease["create-release-on-master.yml<br/>(auto)"]
    MergePR -->|squashes to develop| Build
```

## Development Rules

> **Important:** There is no commit without a ticket number. Every pull request and commit must reference a JIRA ticket (`JNG-xxx`).

Issue tracking: [JIRA Dashboard](https://blackbelt.atlassian.net/jira/dashboards)
