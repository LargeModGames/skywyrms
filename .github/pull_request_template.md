## What

<!-- What does this change, in one or two sentences? -->

Closes #

## Type

- [ ] Model or texture
- [ ] Gameplay or balance
- [ ] Bug fix
- [ ] Cleanup with no behaviour change
- [ ] Build or dependencies

## Testing

<!-- What did you test, and how? What could you not test? -->

- [ ] `.\gradlew build` passes
- [ ] CI is green; for visual changes I looked at the `client-screenshots` artifact
- [ ] Tested in game (`runClient`)
- [ ] Tested on a dedicated server (`runServer`)
- [ ] Model changes: idle, walk, fly, sit, rider and breath checked for every dragon touched
- [ ] Cleanups: bytecode compared with the previous build (see CONTRIBUTING.md)

## Screenshots

<!-- Required for model, texture and HUD changes. Before and after. -->

## Checklist

- [ ] `LICENSE` untouched and still packed into the jar
- [ ] No unrelated files changed
- [ ] `mod_version` left alone (it is raised in its own release commit)
- [ ] Every player needs the new jar: yes / no
