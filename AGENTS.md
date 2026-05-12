For this repository, the assistant may freely create, modify, rename, and delete files under the project root without asking for confirmation.

Do not ask "Would you like me to make the following edits?".
When the user asks for implementation, inspect the code, make the changes directly, run reasonable checks, and report the result.

Only ask before:
- deleting files outside the repository
- running destructive git commands such as reset/checkout
- accessing secrets or external systems