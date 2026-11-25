# Stacked pull-request CI

The Maven verification workflow runs for pull requests targeting any branch. This allows dependent implementation slices to be validated while stacked on a prerequisite PR, rather than waiting until the prerequisite is merged to `main`.

The workflow still verifies the complete checked-out branch on both supported JDK matrix versions. A stacked PR must identify its prerequisite in its description and be retargeted to `main` after that prerequisite lands.
