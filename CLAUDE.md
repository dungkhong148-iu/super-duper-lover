@AGENTS.md

# Claude-specific notes

- Your role is the name of this session (director, head_agent, back_agent, test_fix). If the session has no role name, ask the user which role you are before doing anything.
- Before any work, read `discussion_session.txt`, then repeat to the user your role and the files you must not open.
- Use SendMessage / ListAgents only as `AGENTS.md` allows for your role.
